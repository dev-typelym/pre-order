import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// ✅ 사전 준비: 토큰 리스트 JSON 파일 로드
const users = new SharedArray("users", function () {
    return JSON.parse(open('./tokens.json'));
});

export const options = {
    stages: [
        { duration: '1m', target: 500 },    // 점진적으로 ramp-up
        { duration: '3m', target: 5000 },   // 고정 부하 유지
        { duration: '1m', target: 0 },      // ramp-down
    ],
};

export default function () {
    const user = users[Math.floor(Math.random() * users.length)];
    const authHeader = {
        headers: {
            Authorization: `Bearer ${user.token}`,
            'Content-Type': 'application/json',
        },
    };

    // 1️⃣ 상품 목록 조회
    let res = http.get('http://localhost:8082/api/products', authHeader);
    check(res, { '✅ 상품 목록 조회 성공': (r) => r.status === 200 });

    sleep(0.3);

    // 2️⃣ 상품 상세 조회 (예: 상품 ID 1)
    res = http.get('http://localhost:8082/api/products/1', authHeader);
    check(res, { '✅ 상품 상세 조회 성공': (r) => r.status === 200 });

    sleep(Math.random() * 1);

    // 3️⃣ 주문 생성 (단일 상품 결제 시도)
    if (Math.random() > 0.2) {
        res = http.post('http://localhost:8081/orders/items/1?count=1', null, authHeader);
        check(res, { '✅ 주문 생성 성공': (r) => r.status === 200 });
    } else {
        // 20% 이탈 → 주문 시도 안 함
    }

    sleep(Math.random() * 1);
}