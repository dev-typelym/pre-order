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

// ✅ Refresh 토큰 처리
function refreshAccessToken(user) {
    const res = http.post('http://localhost:8081/api/auth/refresh', JSON.stringify({
        refreshToken: user.refreshToken
    }), {
        headers: { 'Content-Type': 'application/json' }
    });

    if (res.status === 200) {
        const data = JSON.parse(res.body).data;
        user.token = data.accessToken;
        user.refreshToken = data.refreshToken;
        console.log(`✅ 토큰 갱신 성공: ${user.loginId}`);
    } else {
        console.error(`❌ 토큰 갱신 실패: ${user.loginId}`);
    }
}

// ✅ 인증 요청 처리
function authorizedRequest(url, method, user, payload = null) {
    const headers = {
        Authorization: `Bearer ${user.token}`,
        'Content-Type': 'application/json',
    };

    let res = method === 'GET'
        ? http.get(url, { headers })
        : http.post(url, payload, { headers });

    if (res.status === 401) {
        refreshAccessToken(user);
        headers.Authorization = `Bearer ${user.token}`;
        res = method === 'GET'
            ? http.get(url, { headers })
            : http.post(url, payload, { headers });
    }

    return res;
}

// ✅ 시나리오 본문
export default function () {
    const originalUser = users[Math.floor(Math.random() * users.length)];
    const user = Object.assign({}, originalUser);
    const productId = Math.floor(Math.random() * 10) + 1;

    // 1️⃣ 상품 목록 조회
    let res = authorizedRequest('http://localhost:8085/api/products', 'GET', user);
    check(res, { '✅ 상품 목록 조회 성공': (r) => r.status === 200 });
    sleep(Math.random() * 1);

    // 2️⃣ 상품 상세 조회
    res = authorizedRequest(`http://localhost:8085/api/products/${productId}`, 'GET', user);
    check(res, { '✅ 상품 상세 조회 성공': (r) => r.status === 200 });
    sleep(Math.random() * 1.5);

    // 3️⃣ 상품 가용 재고 수량 조회 (추가)
    res = authorizedRequest(`http://localhost:8085/api/products/available-quantities`, 'POST', user, JSON.stringify([productId]));
    check(res, { '✅ 가용 재고 수량 조회 성공': (r) => r.status === 200 });
    sleep(Math.random() * 1);

    // 4️⃣ 주문 준비
    res = authorizedRequest(`http://localhost:8084/orders/items/${productId}?count=1`, 'POST', user);
    check(res, { '✅ 주문 준비 성공': (r) => r.status === 200 });

    if (res.status !== 200) {
        console.error('❌ 주문 준비 실패');
        return;
    }

    const orderId = res.json().data;

    // 🚪 이탈율 1 (prepare 이후)
    if (Math.random() < 0.2) {
        console.log('🚪 유저 이탈 시뮬레이션 (prepare 이후)');
        return;
    }

    sleep(Math.random() * 1.5);

    // 5️⃣ 결제 시도
    res = authorizedRequest(`http://localhost:8084/orders/${orderId}/attempt`, 'POST', user);
    check(res, { '✅ 결제 시도 성공': (r) => r.status === 200 });

    // 🚪 이탈율 2 (attempt 이후)
    if (Math.random() < 0.2) {
        console.log('🚪 유저 이탈 시뮬레이션 (attempt 이후)');
        return;
    }

    sleep(Math.random() * 1.5);

    // 6️⃣ 결제 완료
    res = authorizedRequest(`http://localhost:8084/orders/${orderId}/complete`, 'POST', user);
    check(res, { '✅ 결제 완료 성공': (r) => r.status === 200 });

    sleep(Math.random());

    // 7️⃣ 주문 목록 조회 (추가)
    res = authorizedRequest(`http://localhost:8084/orders/me`, 'GET', user);
    check(res, { '✅ 주문 목록 조회 성공': (r) => r.status === 200 });
}
