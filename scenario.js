import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

const users = new SharedArray("users", function () {
    return JSON.parse(open('./tokens.json'));
});

export const options = {
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
};

// ✅ Refresh 로직
function refreshAccessToken(user) {
    const res = http.post('http://localhost:8081/api/auth/refresh', JSON.stringify({ refreshToken: user.refreshToken }), {
        headers: { 'Content-Type': 'application/json' },
    });

    if (res.status === 200) {
        const body = JSON.parse(res.body).data;
        user.token = body.accessToken;
        user.refreshToken = body.refreshToken;
        console.log(`✅ 토큰 갱신 성공: ${user.loginId}`);
    } else {
        console.error(`❌ 토큰 갱신 실패: ${user.loginId}, status: ${res.status}, body: ${res.body}`);
    }
}

function authorizedRequest(url, method, user, payload = null) {
    let params = {
        headers: {
            Authorization: `Bearer ${user.token}`,
            'Content-Type': 'application/json',
        },
    };

    let res = method === 'GET' ? http.get(url, params) : http.post(url, payload, params);

    // 만료(401)면 refresh 시도
    if (res.status === 401) {
        console.warn(`⚠️ 토큰 만료 감지: ${user.loginId} → 갱신 시도`);
        refreshAccessToken(user);
        params.headers.Authorization = `Bearer ${user.token}`;
        res = method === 'GET' ? http.get(url, params) : http.post(url, payload, params);
    }

    return res;
}

export default function () {
    const user = users[Math.floor(Math.random() * users.length)];

    // 1️⃣ 상품 목록 조회
    let res = authorizedRequest('http://localhost:8085/api/products', 'GET', user);
    check(res, { '✅ 상품 목록 조회 성공': (r) => r.status === 200 });

    sleep(0.3);

    // 2️⃣ 상품 상세 조회
    res = authorizedRequest('http://localhost:8085/api/products/1', 'GET', user);
    check(res, { '✅ 상품 상세 조회 성공': (r) => r.status === 200 });

    sleep(Math.random() * 1);

    // 3️⃣ 주문 생성
    if (Math.random() > 0.2) {
        res = authorizedRequest('http://localhost:8084/orders/items/1?count=1', 'POST', user);
        check(res, { '✅ 주문 최종 성공': (r) => r.status === 200 });
    }

    sleep(Math.random() * 1);
}