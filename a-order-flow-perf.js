// a-order-flow-perf.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.4/index.js';

// ===== 실행 파라미터 =====
const GW  = __ENV.GW     || 'http://localhost:8087';
const RUN = __ENV.RUN_ID || `aorder-${Date.now()}`;
const USE_ATTEMPT = (__ENV.ATTEMPT || 'false').toLowerCase() === 'true'; // 기본 false

// 상품: PRODUCT_IDS("1,2") 또는 구간(PRODUCT_MIN/MAX), 기본 1~10
const PRODUCT_IDS = (__ENV.PRODUCT_IDS || '').split(',').map(s => s.trim()).filter(Boolean).map(Number);
const P_MIN = parseInt(__ENV.PRODUCT_MIN || '1', 10);
const P_MAX = parseInt(__ENV.PRODUCT_MAX || '10', 10);
function pickPid() {
    if (PRODUCT_IDS.length > 0) return PRODUCT_IDS[(__VU + __ITER) % PRODUCT_IDS.length];
    return P_MIN + Math.floor(Math.random() * (P_MAX - P_MIN + 1));
}

// tokens.json 라운드로빈(없으면 TOKEN 하나)
const users = new SharedArray('users', () => { try { return JSON.parse(open('./tokens.json')); } catch { return []; }});
function pickToken() {
    if (users.length > 0) return users[(__VU + __ITER) % users.length]?.token || null;
    return __ENV.TOKEN || null;
}

// ===== 커스텀 메트릭 =====
export const system_fail  = new Rate('system_fail');   // 5xx/타임아웃만
export const conflict_409 = new Rate('conflict_409');  // 409(경쟁) 비율

export const options = {
    tags: { run_id: RUN },
    scenarios: {
        ramp: {
            executor: 'ramping-arrival-rate',
            startRate: 100, timeUnit: '1s',
            preAllocatedVUs: 400, maxVUs: 2000,
            stages: [
                { target: 200, duration: '1m' },
                { target: 300, duration: '2m' },
                { target: 400, duration: '2m' },
                { target:   0, duration: '30s' },
            ],
            gracefulStop: '15s',
        },
    },
    thresholds: {
        // 합격/불합격은 "진짜 장애"만으로 판정
        system_fail: ['rate<0.05'],

        // 속도 목표 (엔드포인트별)
        'http_req_duration{ep:prepare}':  ['p(95)<1500'],
        ...(USE_ATTEMPT ? { 'http_req_duration{ep:attempt}': ['p(95)<1500'] } : {}),
        'http_req_duration{ep:complete}': ['p(95)<1500'],
    },
    summaryTrendStats: ['avg','p(95)','p(99)'],
};

function headers(token) {
    const h = { 'Content-Type': 'application/json', 'X-Run-Id': RUN };
    if (token) h.Authorization = `Bearer ${token}`;
    return h;
}

const API = {
    prepare: (pid, cnt) => `${GW}/api/orders/prepare/${pid}?count=${cnt}`,
    attempt: (oid)      => `${GW}/api/orders/${oid}/attempt`,
    complete: (oid)     => `${GW}/api/orders/${oid}/complete`,
    parseId: (res) => { try { return JSON.parse(res.body)?.data; } catch { return null; } },
};

export default function () {
    const token = pickToken();
    const pid = pickPid();
    const cnt = 1;

    // 1) 준비
    let res = http.post(API.prepare(pid, cnt), null, {
        headers: headers(token),
        // name 태그를 고정 경로로 → 시계열 폭증 방지
        tags: { ep: 'prepare', name: '/api/orders/prepare', run_id: RUN },
    });
    // 메트릭 기록 (장애/경쟁 분리)
    system_fail.add(res.status === 0 || res.status >= 500);
    conflict_409.add(res.status === 409);
    check(res, { 'prepare 2xx': r => r.status >= 200 && r.status < 300 });
    if (!(res.status >= 200 && res.status < 300)) return;

    const orderId = API.parseId(res);
    if (!orderId) return;

    // 2) (옵션) 시도
    if (USE_ATTEMPT) {
        res = http.post(API.attempt(orderId), null, {
            headers: headers(token),
            tags: { ep: 'attempt', name: '/api/orders/attempt', run_id: RUN },
        });
        system_fail.add(res.status === 0 || res.status >= 500);
        conflict_409.add(res.status === 409);
        check(res, { 'attempt 2xx': r => r.status >= 200 && r.status < 300 });
        if (!(res.status >= 200 && res.status < 300)) return;
    }

    // 3) 완료
    res = http.post(API.complete(orderId), null, {
        headers: headers(token),
        tags: { ep: 'complete', name: '/api/orders/complete', run_id: RUN },
    });
    system_fail.add(res.status === 0 || res.status >= 500);
    conflict_409.add(res.status === 409);
    check(res, { 'complete 2xx': r => r.status >= 200 && r.status < 300 });

    sleep(0.03);
}

export function handleSummary(data) {
    return { stdout: textSummary(data, { indent: ' ', enableColors: true }) };
}
