import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

const users = new SharedArray('users', () => JSON.parse(open('./tokens.json')));

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
    const res = http.post(
        'http://localhost:8081/api/auth/refresh',
        JSON.stringify({ refreshToken: user.refreshToken }),
        {
            headers: { 'Content-Type': 'application/json' },
            // (선택) 대시보드에서 구분하고 싶으면 라벨 고정
            tags: { name: 'POST http://localhost:8081/api/auth/refresh' },
        }
    );

    if (res.status === 200) {
        const data = JSON.parse(res.body).data;
        user.token = data.accessToken;
        user.refreshToken = data.refreshToken;
    }
}

function authorizedRequest(url, method, user) {
    const headers = {
        Authorization: `Bearer ${user.token}`,
        'Content-Type': 'application/json',
    };

    // ✅ 실제 호출은 게이트웨이(8086)지만, 라벨은 예전(8085)로 고정
    let res = http.get(url, {
        headers,
        tags: { name: 'GET http://localhost:8085/api/products' },
    });

    if (res.status === 401) {
        refreshAccessToken(user);
        headers.Authorization = `Bearer ${user.token}`;
        res = http.get(url, {
            headers,
            tags: { name: 'GET http://localhost:8085/api/products' },
        });
    }

    return res;
}

export default function () {
    const user = Object.assign({}, users[Math.floor(Math.random() * users.length)]);
    // 🔁 호출 대상만 게이트웨이로 변경(8086)
    const res = authorizedRequest('http://localhost:8086/api/products', 'GET', user);
    check(res, { '✅ 상품 목록 조회 성공': (r) => r.status === 200 });
    sleep(Math.random() * 1);
}
