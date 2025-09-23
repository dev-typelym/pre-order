// a-order-flow-perf.js — MSR(최대 지속 처리율) 탐색용 "완만 램프" 버전
// 기본 스테이지: 60 -> 90 -> 120 RPS (각 1m) 후 쿨다운 20s
// 필요시 ENV로 조절:
//   RPS1=40 RPS2=60 RPS3=90 DUR1=45s DUR2=45s DUR3=60s COOL=20s
// 실행 예시:
//   k6 run -o xk6-influxdb -e RPS1=40 -e RPS2=60 -e RPS3=90 a-order-flow-perf.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.4/index.js';

// ===== 실행 파라미터 =====
const GW  = __ENV.GW     || 'http://localhost:8087';
const RUN = __ENV.RUN_ID || `aorder-${Date.now()}`;
// 기본 ON: attempt 단계 포함
const USE_ATTEMPT = (__ENV.ATTEMPT || 'true').toLowerCase() === 'true';

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

// ===== 램프 스테이지(로컬 친화 기본) =====
const R1   = parseInt(__ENV.RPS1 || '60', 10);
const R2   = parseInt(__ENV.RPS2 || '90', 10);
const R3   = parseInt(__ENV.RPS3 || '120', 10);
const D1   = __ENV.DUR1 || '1m';
const D2   = __ENV.DUR2 || '1m';
const D3   = __ENV.DUR3 || '1m';
const COOL = __ENV.COOL || '20s';
const PEAK = Math.max(R1, R2, R3);

// VU 프리할당/상한(너무 크게 잡지 않음)
const PRE_VUS = Math.min(800, Math.max(60, Math.ceil(PEAK * 1.5)));
const MAX_VUS = Math.min(2000, Math.max(120, PEAK * 3));

export const options = {
    tags: { run_id: RUN },
    scenarios: {
        ramp: {
            executor: 'ramping-arrival-rate',
            startRate: Math.max(10, Math.min(30, Math.floor(R1 / 2))), // 너무 급하지 않게
            timeUnit: '1s',
            preAllocatedVUs: PRE_VUS,
            maxVUs: MAX_VUS,
            stages: [
                { target: R1, duration: D1 },
                { target: R2, duration: D2 },
                { target: R3, duration: D3 },
                { target:   0, duration: COOL },
            ],
            gracefulStop: '15s',
        },
    },
    thresholds: {
        // 합격/불합격은 "진짜 장애"만으로 판정
        system_fail: ['rate<0.05'],

        // 속도 목표 (엔드포인트별) — 참고치
        'http_req_duration{ep:prepare}':  ['p(95)<1500'],
        ...(USE_ATTEMPT ? { 'http_req_duration{ep:attempt}': ['p(95)<1500'] } : {}),
        'http_req_duration{ep:complete}': ['p(95)<1500'],
    },
    // prepare에서 orderId 파싱해야 하므로 전체 discard는 안 씀
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

    // 1) 준비 (응답 본문 필요)
    let res = http.post(API.prepare(pid, cnt), null, {
        headers: headers(token),
        tags: { ep: 'prepare', name: '/api/orders/prepare', run_id: RUN },
    });
    system_fail.add(res.status === 0 || res.status >= 500);
    conflict_409.add(res.status === 409);
    check(res, { 'prepare 2xx': r => r.status >= 200 && r.status < 300 });
    if (!(res.status >= 200 && res.status < 300)) return;

    const orderId = API.parseId(res);
    if (!orderId) return;

    // 2) (옵션) 시도 — 응답 본문 불필요 → responseType: 'none'으로 수신 부담↓
    if (USE_ATTEMPT) {
        res = http.post(API.attempt(orderId), null, {
            headers: headers(token),
            tags: { ep: 'attempt', name: '/api/orders/attempt', run_id: RUN },
            responseType: 'none',
        });
        system_fail.add(res.status === 0 || res.status >= 500);
        conflict_409.add(res.status === 409);
        check(res, { 'attempt 2xx': r => r.status >= 200 && r.status < 300 });
        if (!(res.status >= 200 && res.status < 300)) return;
    }

    // 3) 완료 — 응답 본문 불필요
    res = http.post(API.complete(orderId), null, {
        headers: headers(token),
        tags: { ep: 'complete', name: '/api/orders/complete', run_id: RUN },
        responseType: 'none',
    });
    system_fail.add(res.status === 0 || res.status >= 500);
    conflict_409.add(res.status === 409);
    check(res, { 'complete 2xx': r => r.status >= 200 && r.status < 300 });

    // 너무 타이트하게 몰지 않도록 미세 페이싱
    if (Math.random() < 0.3) sleep(0.01);
}

export function handleSummary(data) {
    return { stdout: textSummary(data, { indent: ' ', enableColors: true }) };
}
