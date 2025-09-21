// b-order-funnel.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.4/index.js';

const GW  = __ENV.GW     || 'http://localhost:8087';
const RUN = __ENV.RUN_ID || `border-${Date.now()}`;

const DROP_PREPARE = parseFloat(__ENV.DROP_PREPARE || '0.2'); // 20%
const DROP_ATTEMPT = parseFloat(__ENV.DROP_ATTEMPT || '0.2'); // 20%

const PRODUCT_IDS = (__ENV.PRODUCT_IDS || '').split(',').map(s => s.trim()).filter(Boolean).map(Number);
const P_MIN = parseInt(__ENV.PRODUCT_MIN || '1', 10);
const P_MAX = parseInt(__ENV.PRODUCT_MAX || '10', 10);
function pickPid() {
    if (PRODUCT_IDS.length > 0) return PRODUCT_IDS[(__VU + __ITER) % PRODUCT_IDS.length];
    return P_MIN + Math.floor(Math.random() * (P_MAX - P_MIN + 1));
}

const users = new SharedArray('users', () => { try { return JSON.parse(open('./tokens.json')); } catch { return []; }});
function pickToken() {
    if (users.length > 0) return users[(__VU + __ITER) % users.length]?.token || null;
    return __ENV.TOKEN || null;
}

export const step_prepare = new Counter('border_prepare_step');
export const step_attempt = new Counter('border_attempt_step');
export const step_complete = new Counter('border_complete_2xx_step');

export const options = {
    tags: { run_id: RUN },
    scenarios: {
        ramp: {
            executor: 'ramping-arrival-rate',
            startRate: 50, timeUnit: '1s',
            preAllocatedVUs: 300, maxVUs: 1500,
            stages: [
                { target: 100, duration: '1m' },
                { target: 200, duration: '2m' },
                { target:   0, duration: '30s' },
            ],
            gracefulStop: '15s',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        'http_req_duration{ep:prepare}':  ['p(95)<1500'],
        'http_req_duration{ep:attempt}':  ['p(95)<1500'],
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
    let res = http.post(API.prepare(pid, cnt), null, { headers: headers(token), tags: { ep: 'prepare', run_id: RUN } });
    step_prepare.add(1);
    check(res, { 'prepare 2xx': r => r.status >= 200 && r.status < 300 });
    if (!(res.status >= 200 && res.status < 300)) return;

    // 이탈 #1 (prepare 후)
    if (Math.random() < DROP_PREPARE) return;

    const orderId = API.parseId(res);
    if (!orderId) return;

    // 2) 시도
    res = http.post(API.attempt(orderId), null, { headers: headers(token), tags: { ep: 'attempt', run_id: RUN } });
    step_attempt.add(1);
    check(res, { 'attempt 2xx': r => r.status >= 200 && r.status < 300 });
    if (!(res.status >= 200 && res.status < 300)) return;

    // 이탈 #2 (attempt 후)
    if (Math.random() < DROP_ATTEMPT) return;

    // 3) 완료
    res = http.post(API.complete(orderId), null, { headers: headers(token), tags: { ep: 'complete', run_id: RUN } });
    if (res.status >= 200 && res.status < 300) step_complete.add(1);
    check(res, { 'complete 2xx': r => r.status >= 200 && r.status < 300 });

    sleep(0.03);
}

export function handleSummary(data) {
    const prep = data.metrics.border_prepare_step?.values?.count || 0;
    const att  = data.metrics.border_attempt_step?.values?.count || 0;
    const comp = data.metrics.border_complete_2xx_step?.values?.count || 0;
    const pa = prep ? (att / prep) * 100 : 0;
    const ac = att  ? (comp / att) * 100 : 0;
    const e2e = prep ? (comp / prep) * 100 : 0;

    console.log(`\n[RUN_ID] ${RUN}`);
    console.log(`[CONVERSION] Prepare→Attempt: ${pa.toFixed(2)}%`);
    console.log(`[CONVERSION] Attempt→Complete: ${ac.toFixed(2)}%`);
    console.log(`[CONVERSION] E2E: ${e2e.toFixed(2)}%\n`);

    return { stdout: textSummary(data, { indent: ' ', enableColors: true }) };
}
