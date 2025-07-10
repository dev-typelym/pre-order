import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// ✅ 사전 준비: 토큰 리스트 JSON 파일 로드
// 예: tokens.json => [{ "token": "abc123" }, ...]
const users = new SharedArray("users", function () {
    return JSON.parse(open('./tokens.json'));
});

// ✅ 부하 옵션 (5000명 기준)
export const options = {
    stages: [
        { duration: '1m', target: 500 },    // 점진적으로 ramp-up
        { duration: '3m', target: 5000 },   // 고정 부하 유지
        { duration: '1m', target: 0 },      // ramp-down
    ],
};

export default function () {
    // ✅ 무작위 유저 선택
    const user = users[Math.floor(Math.random() * users.length)];
    const authHeader = {
        headers: {
            Authorization: `Bearer ${user.token}`,
            'Content-Type': 'application/json', // ➡️ JSON 요청 시 명시 권장
        },
    };

    // 1️⃣ 상품 목록 조회
    let res = http.get('http://localhost:8082/api/products', authHeader);
    check(res, { '✅ 상품 목록 조회 성공': (r) => r.status === 200 });

    // 👉 상품 목록 페이지에서 잠깐 체류 (UX 시뮬레이션)
    sleep(0.3);

    // 2️⃣ 상품 상세 조회 (예: 상품 ID 1)
    res = http.get('http://localhost:8082/api/products/1', authHeader);
    check(res, { '✅ 상품 상세 조회 성공': (r) => r.status === 200 });

    // 👉 상세 페이지 고민 시간
    sleep(Math.random() * 1);

    // 3️⃣ 결제 준비 (결제 화면 진입 시뮬레이션)
    res = http.post('http://localhost:8081/api/orders/prepare', JSON.stringify({
        productId: 1,
        quantity: 1,
    }), authHeader);
    check(res, { '✅ 결제 준비 성공': (r) => r.status === 200 });

    // 👉 결제 화면에서 고민 시간
    sleep(Math.random() * 1);

    // 4️⃣ 결제 시도
    if (Math.random() > 0.2) {
        // 👉 성공 시 결제 시도
        res = http.post('http://localhost:8081/api/orders/confirm', JSON.stringify({
            productId: 1,
            quantity: 1,
        }), authHeader);
        check(res, { '✅ 결제 시도 성공': (r) => r.status === 200 || r.status === 201 });
    } else {
        // 👉 20%는 이탈 (결제 시도 안 함)
    }

    // 👉 각 유저별 마지막 랜덤 딜레이
    sleep(Math.random() * 1);
}