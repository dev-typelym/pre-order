import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.4/index.js';

// ★ 실행 식별자: 매 실행마다 고유 run_id
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;

// 논리적 단계 카운터(재시도와 무관하게 1번만 증가)
export const orders_prepare_step = new Counter('orders_prepare_step');
export const orders_attempt_step = new Counter('orders_attempt_step');
export const orders_complete_2xx_step = new Counter('orders_complete_2xx_step');

const users = new SharedArray('users', () => JSON.parse(open('./tokens.json')));

export const options = {
    tags: { run_id: RUN_ID }, // ★ 모든 메트릭에 run_id 태그 부여
    stages: [
        { duration: '1m', target: 500 },
        { duration: '1m', target: 1000 },
        { duration: '1m', target: 2000 },
        { duration: '1m', target: 3000 },
        { duration: '1m', target: 4000 },
        { duration: '3m', target: 5000 },
        { duration: '1m', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<1500'],
    },
    // (선택) 메모리/네트워크 효율 향상용
    // discardResponseBodies: true,
    // noConnectionReuse: true,
};

// 🔄 AccessToken 재발급
function refreshAccessToken(user) {
    const res = http.post(
        'http://localhost:8081/api/auth/refresh',
        JSON.stringify({ refreshToken: user.refreshToken }),
        {
            headers: { 'Content-Type': 'application/json' },
            tags: { name: 'auth_refresh', run_id: RUN_ID },
        }
    );

    if (res.status === 200) {
        const data = JSON.parse(res.body).data;
        user.token = data.accessToken;
        user.refreshToken = data.refreshToken;
    }
}

// ✅ 요청 전송 (name 태그 + run_id 항상 포함)
function authorizedRequest(url, method, user, payload = null, tagName = null) {
    const headers = {
        Authorization: `Bearer ${user.token}`,
        'Content-Type': 'application/json',
    };
    const baseTags = { run_id: RUN_ID };
    const tags = tagName ? { ...baseTags, name: tagName } : baseTags;

    let res =
        method === 'GET'
            ? http.get(url, { headers, tags })
            : http.post(url, payload, { headers, tags });

    if (res.status === 401) {
        refreshAccessToken(user);
        headers.Authorization = `Bearer ${user.token}`;
        res =
            method === 'GET'
                ? http.get(url, { headers, tags })
                : http.post(url, payload, { headers, tags });
    }

    return res;
}

// 🧪 시뮬레이션
export default function () {
    const user = { ...users[Math.floor(Math.random() * users.length)] };
    const productId = Math.floor(Math.random() * 10) + 1; // 1~10
    const count = Math.floor(Math.random() * 10) + 1; // 1~10

    // 1) 준비
    let res = authorizedRequest(
        `http://localhost:8084/api/orders/prepare/${productId}?count=${count}`,
        'POST',
        user,
        null,
        'orders_prepare'
    );
    orders_prepare_step.add(1); // 논리적 단계 카운트(재시도와 무관)
    check(res, { '✅ 주문 준비 성공': (r) => r.status === 200 });
    if (res.status !== 200) return;

    const orderId = res.json().data;

    // 이탈 1
    if (Math.random() < 0.2) return;
    sleep(Math.random() * 1.5);

    // 2) 시도
    res = authorizedRequest(
        `http://localhost:8084/api/orders/${orderId}/attempt`,
        'POST',
        user,
        null,
        'orders_attempt'
    );
    orders_attempt_step.add(1);
    check(res, { '✅ 결제 시도 성공': (r) => r.status === 200 });

    // 이탈 2
    if (Math.random() < 0.2) return;
    sleep(Math.random() * 1.5);

    // 3) 완료
    res = authorizedRequest(
        `http://localhost:8084/api/orders/${orderId}/complete`,
        'POST',
        user,
        null,
        'orders_complete'
    );
    if (res.status === 200) orders_complete_2xx_step.add(1); // 성공한 완료만
    check(res, { '✅ 결제 완료 성공': (r) => r.status === 200 });
}

// 🔎 터미널 요약 & 전환율 + 총 요청 수 출력
export function handleSummary(data) {
    // 📌 재시도 포함 전체 HTTP 요청 수 (Grafana http_reqs와 동일)
    const totalHttpReqs = data.metrics.http_reqs?.values?.count || 0;

    // 📌 논리적 단계 카운트 (재시도 제외)
    const prep = data.metrics.orders_prepare_step?.values?.count || 0;
    const att = data.metrics.orders_attempt_step?.values?.count || 0;
    const comp = data.metrics.orders_complete_2xx_step?.values?.count || 0;

    const pa = prep > 0 ? (att / prep) * 100 : 0;
    const ac = att > 0 ? (comp / att) * 100 : 0;
    const e2e = prep > 0 ? (comp / prep) * 100 : 0;

    console.log(`\n[RUN_ID] ${RUN_ID}`);
    console.log(`[TOTAL REQUESTS] (incl. retries): ${totalHttpReqs}`); // ✅ Grafana와 동일
    console.log(`[CONVERSION] Prepare→Attempt: ${pa.toFixed(2)}%`);
    console.log(`[CONVERSION] Attempt→Complete: ${ac.toFixed(2)}%`);
    console.log(`[CONVERSION] E2E: ${e2e.toFixed(2)}%\n`);

    // ✅ 기본 요약을 터미널에 출력
    return {
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
        'summary.json': JSON.stringify(data, null, 2), // 파일 저장 원하면 주석 해제
    };
}