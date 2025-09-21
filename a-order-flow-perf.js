// a-order-flow-perf.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.4/index.js';

// ===== 실행 파라미터 =====
const GW = __ENV.GW || 'http://localhost:8087';
const RUN = __ENV.RUN_ID || `aorder-${Date.now()}`;
const USE_ATTEMPT = (__ENV.ATTEMPT || 'true').toLowerCase() === 'true';

// 상품 선택: PRODUCT_IDS("1001,1002") 또는 구간(PRODUCT_MIN/MAX), 기본 1~10
const PRODUCT_IDS = (__ENV.PRODUCT_IDS || '')
    .split(',')
    .map(s => s.trim())
    .filter(Boolean)
    .map(Number);
const P_MIN = parseInt(__ENV.PRODUCT_MIN || '1', 10);
const P_MAX = parseInt(__ENV.PRODUCT_MAX || '10', 10);
function pickPid() {
    if (PRODUCT_IDS.length > 0) return PRODUCT_IDS[( __VU + __ITER ) % PRODUCT_IDS.length];
    return P_MIN + Math.floor(Math.random() * (P_MAX - P_MIN + 1));
}

// tokens.json이 있으면 라운드로빈, 없으면 TOKEN 하나 사용
const users = new SharedArray('users', () => {
    try { return JSON.parse(open('./tokens.json')); } catch (_) { return []; }
});
function pickToken() {
    if (users.length > 0) {
        const u = users[( __VU + __ITER ) % users.length];
        return u?.token || null;
    }
    return __ENV.TOKEN || null;
}

export const options = {
    tags: { run_id: RUN },
    scenarios: {
        ramp: {
            executor: 'ramping-arrival-rate',
            startRate: 100,
            timeUnit: '1s',
            preAllocatedVUs: 400,
            maxVUs: 2000,
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
        http_req_failed: ['rate<0.05'],
        'http_req_duration{ep:prepare}': ['p(95)<1500'],
        ...(USE_ATTEMPT ? { 'http_req_duration{ep:attempt}': ['p(95)<1500'] } : {}),
        'http_req_duration{ep:complete}': ['p(95)<1500'],
    },
    discardResponseBodies: true,
    summaryTrendStats: ['avg','p(95)','p(99)'],
};

function headers(token) {
    const h = { 'Content-Type': 'application/json', 'X-Run-Id': RUN };
    if (token) h.Authorization = `Bearer ${token}`;
    return h;
}

// === 주문 API 경로(네 프로젝트 시그니처에 맞춤: path param 버전) ===
const API = {
    prepare: (pid, cnt) => `${GW}/api/orders/prepare/${pid}?count=${cnt}`,
    attempt: (orderId)  => `${GW}/api/orders/${orderId}/attempt`,
    complete: (orderId) => `${GW}/api/orders/${orderId}/complete`,
    parsePrepareId: (res) => { try { return JSON.parse(res.body)?.data; } catch { return null; } },
};

export default function () {
    const token = pickToken();
    const pid = pickPid();
    const cnt = 1; // 재고 예측 쉬우라고 1로 고정 (성능 측정 목적)

    // 1) 준비
    let res = http.post(
        API.prepare(pid, cnt),
        null,
        { headers: headers(token), tags: { ep: 'prepare', run_id: RUN } }
    );
    check(res, { 'prepare 2xx': (r) => r.status >= 200 && r.status < 300 });
    if (res.status < 200 || res.status >= 300) return;

    const orderId = API.parsePrepareId(res);
    if (!orderId) return;

    // 2) 시도(옵션)
    if (USE_ATTEMPT) {
        res = http.post(
            API.attempt(orderId),
            null,
            { headers: headers(token), tags: { ep: 'attempt', run_id: RUN } }
        );
        check(res, { 'attempt 2xx': (r) => r.status >= 200 && r.status < 300 });
        if (res.status < 200 || res.status >= 300) return;
    }

    // 3) 완료
    res = http.post(
        API.complete(orderId),
        null,
        { headers: headers(token), tags: { ep: 'complete', run_id: RUN } }
    );
    check(res, { 'complete 2xx': (r) => r.status >= 200 && r.status < 300 });

    sleep(0.03); // 소폭 지터
}

export function handleSummary(data) {
    return { stdout: textSummary(data, { indent: ' ', enableColors: true }) };
}
