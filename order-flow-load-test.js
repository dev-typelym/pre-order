import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

const users = new SharedArray("users", () => JSON.parse(open('./tokens.json')));
const productId = 1;

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
    }
}

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

export default function () {
    const user = Object.assign({}, users[Math.floor(Math.random() * users.length)]);

    // 1️⃣ 주문 준비
    let res = authorizedRequest(`http://localhost:8084/api/orders/prepare/${productId}?count=1`, 'POST', user);
    check(res, { '✅ 주문 준비 성공': (r) => r.status === 200 });
    if (res.status !== 200) return;

    const orderId = res.json().data;

    // 🚪 이탈율 1
    if (Math.random() < 0.2) {
        console.log('🚪 유저 이탈 시뮬레이션 (prepare 이후)');
        return;
    }

    sleep(Math.random() * 1.5);

    // 2️⃣ 결제 시도
    res = authorizedRequest(`http://localhost:8084/api/orders/${orderId}/attempt`, 'POST', user);
    check(res, { '✅ 결제 시도 성공': (r) => r.status === 200 });

    // 🚪 이탈율 2
    if (Math.random() < 0.2) {
        console.log('🚪 유저 이탈 시뮬레이션 (attempt 이후)');
        return;
    }

    sleep(Math.random() * 1.5);

    // 3️⃣ 결제 완료
    res = authorizedRequest(`http://localhost:8084/api/orders/${orderId}/complete`, 'POST', user);
    check(res, { '✅ 결제 완료 성공': (r) => r.status === 200 });
}