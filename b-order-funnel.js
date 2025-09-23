// b-order-funnel.js — 퍼널 정합성 검증(닫힌 런: TOTAL/RPS)
// TOTAL=총 prepare 시도 수, RPS=초당 prepare 시도 수 → DURATION 자동계산
// 예) $env:TOTAL="10000"; $env:RPS="80"; .\k6.exe run .\b-order-funnel.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.4/index.js';

// ── 실행 파라미터 ───────────────────────────────────────────────
const GW   = __ENV.GW     || 'http://localhost:8087';
const RUN  = __ENV.RUN_ID || `border-${Date.now()}`;

const TOTAL        = parseInt(__ENV.TOTAL || '10000', 10);  // 총 prepare 시도 수
const RPS          = parseInt(__ENV.RPS   || '80',    10);  // 초당 prepare 시도 수
const DROP_PREPARE = parseFloat(__ENV.DROP_PREPARE || '0.2'); // 20% (prepare→attempt 이탈)
const DROP_ATTEMPT = parseFloat(__ENV.DROP_ATTEMPT || '0.2'); // 20% (attempt→complete 이탈)

// duration = TOTAL / RPS (초), 최소 1초
const DURATION_SEC = Math.max(1, Math.ceil(TOTAL / Math.max(1, RPS)));
const DURATION     = `${DURATION_SEC}s`;

// 상품 선택(명시 목록 or 구간)
const PRODUCT_IDS = (__ENV.PRODUCT_IDS || '').split(',').map(s => s.trim()).filter(Boolean).map(Number);
const P_MIN = parseInt(__ENV.PRODUCT_MIN || '1', 10);
const P_MAX = parseInt(__ENV.PRODUCT_MAX || '10', 10);
function pickPid() {
    if (PRODUCT_IDS.length > 0) return PRODUCT_IDS[(__VU + __ITER) % PRODUCT_IDS.length];
    return P_MIN + Math.floor(Math.random() * (P_MAX - P_MIN + 1));
}

// tokens.json 라운드로빈(없으면 단일 TOKEN)
const users = new SharedArray('users', () => { try { return JSON.parse(open('./tokens.json')); } catch { return []; } });
function pickToken() {
    if (users.length > 0) return users[(__VU + __ITER) % users.length]?.token || null;
    return __ENV.TOKEN || null;
}

// ── 커스텀 메트릭(퍼널/상태) ───────────────────────────────────
export const step_prepare  = new Counter('border_prepare_step_2xx');  // prepare 2xx 건수
export const step_attempt  = new Counter('border_attempt_step_2xx');  // attempt 2xx 건수
export const step_complete = new Counter('border_complete_step_2xx'); // complete 2xx 건수

export const system_fail   = new Rate('system_fail');   // 5xx/timeout 비율
export const conflict_409  = new Rate('conflict_409');  // 409 비율(경쟁)

// ── k6 옵션 ────────────────────────────────────────────────────
export const options = {
    tags: { run_id: RUN },
    scenarios: {
        funnel: {
            executor: 'constant-arrival-rate',
            rate: RPS,
            timeUnit: '1s',
            duration: DURATION,                           // TOTAL/RPS만큼만 실행
            preAllocatedVUs: Math.max(20, Math.min(300, RPS * 2)),
            maxVUs: Math.max(40, RPS * 3),
            gracefulStop: '15s',
        },
    },
    thresholds: {
        // 합격/불합격은 "장애(5xx/timeout)"만으로 판정
        system_fail: ['rate<0.05'],

        // 지연 목표(참고용)
        'http_req_duration{ep:prepare}':  ['p(95)<1500'],
        'http_req_duration{ep:attempt}':  ['p(95)<1500'],
        'http_req_duration{ep:complete}': ['p(95)<1500'],
    },
    summaryTrendStats: ['avg','p(95)','p(99)'],
};

// ── 공통 유틸 ───────────────────────────────────────────────────
function headers(token) {
    const h = { 'Content-Type': 'application/json', 'X-Run-Id': RUN };
    if (token) h.Authorization = `Bearer ${token}`;
    return h;
}

const API = {
    prepare:  (pid, cnt) => `${GW}/api/orders/prepare/${pid}?count=${cnt}`,
    attempt:  (oid)      => `${GW}/api/orders/${oid}/attempt`,
    complete: (oid)      => `${GW}/api/orders/${oid}/complete`,
    parseId:  (res)      => { try { return JSON.parse(res.body)?.data; } catch { return null; } },
};

// ── 본문 ────────────────────────────────────────────────────────
export default function () {
    const token = pickToken();
    const pid   = pickPid();
    const cnt   = 1;

    // 1) 준비 (본문 필요: orderId 파싱)
    let res = http.post(API.prepare(pid, cnt), null, {
        headers: headers(token),
        tags: { ep: 'prepare', name: '/api/orders/prepare', run_id: RUN },
    });
    system_fail.add(res.status === 0 || res.status >= 500);
    conflict_409.add(res.status === 409);
    const okPrepare = res.status >= 200 && res.status < 300;
    check(res, { 'prepare 2xx': () => okPrepare });
    if (!okPrepare) return;

    step_prepare.add(1);

    // 이탈 #1 (prepare 이후 시도 안 함)
    if (Math.random() < DROP_PREPARE) return;

    const orderId = API.parseId(res);
    if (!orderId) return;

    // 2) 시도 (본문 불필요)
    res = http.post(API.attempt(orderId), null, {
        headers: headers(token),
        tags: { ep: 'attempt', name: '/api/orders/attempt', run_id: RUN },
        responseType: 'none',
    });
    system_fail.add(res.status === 0 || res.status >= 500);
    conflict_409.add(res.status === 409);
    const okAttempt = res.status >= 200 && res.status < 300;
    check(res, { 'attempt 2xx': () => okAttempt });
    if (!okAttempt) return;

    step_attempt.add(1);

    // 이탈 #2 (attempt 이후 complete 안 함)
    if (Math.random() < DROP_ATTEMPT) return;

    // 3) 완료 (본문 불필요)
    res = http.post(API.complete(orderId), null, {
        headers: headers(token),
        tags: { ep: 'complete', name: '/api/orders/complete', run_id: RUN },
        responseType: 'none',
    });
    system_fail.add(res.status === 0 || res.status >= 500);
    conflict_409.add(res.status === 409);
    const okComplete = res.status >= 200 && res.status < 300;
    check(res, { 'complete 2xx': () => okComplete });
    if (okComplete) step_complete.add(1);

    // 미세 페이싱(폭주 방지용 소량 지터)
    if (Math.random() < 0.2) sleep(0.01);
}

// ── 요약(퍼널 전환율) ───────────────────────────────────────────
export function handleSummary(data) {
    const prep = data.metrics.border_prepare_step_2xx?.values?.count || 0;
    const att  = data.metrics.border_attempt_step_2xx?.values?.count || 0;
    const comp = data.metrics.border_complete_step_2xx?.values?.count || 0;

    const pa  = prep ? (att / prep) * 100 : 0;
    const ac  = att  ? (comp / att) * 100 : 0;
    const e2e = prep ? (comp / prep) * 100 : 0;

    console.log(`\n[RUN_ID] ${RUN}`);
    console.log(`[TARGET] TOTAL=${TOTAL}, RPS=${RPS}, DURATION=${DURATION}`);
    console.log(`[FUNNEL] Prepare(2xx)=${prep}, Attempt(2xx)=${att}, Complete(2xx)=${comp}`);
    console.log(`[CONVERSION] Prepare→Attempt: ${pa.toFixed(2)}%`);
    console.log(`[CONVERSION] Attempt→Complete: ${ac.toFixed(2)}%`);
    console.log(`[CONVERSION] End-to-End: ${e2e.toFixed(2)}%\n`);

    return { stdout: textSummary(data, { indent: ' ', enableColors: true }) };
}
