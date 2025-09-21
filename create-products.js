const axios = require('axios');

let accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwidXNlcm5hbWUiOiJhZG1pblVzZXIiLCJyb2xlIjoiUk9MRV9BRE1JTiIsImRldmljZUlkIjoiYTRkYThkODItYWI3OC00NWEzLTkzNzEtZTMwNmE3NjExNWNhIiwiaWF0IjoxNzU4NDc3MTExLCJleHAiOjE3NTg1NjM1MTF9.BPrfPa1UG1AjSvY-vp-qP2X9lJ_mDttXqwjbe3_l9JM";
let refreshToken = "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwidXNlcm5hbWUiOiJhZG1pblVzZXIiLCJyb2xlIjoiUk9MRV9BRE1JTiIsImRldmljZUlkIjoiYTRkYThkODItYWI3OC00NWEzLTkzNzEtZTMwNmE3NjExNWNhIiwiaWF0IjoxNzU4NDc3MTExLCJleHAiOjE3NTkwODE5MTF9.sz8udvxvgsX9As_yC3CbGA8MGfANn1m-WZbnSeaKBNA";

const NUM_PRODUCTS = parseInt(process.env.NUM_PRODUCTS || '10', 10);     // 상품 개수
const STOCK_QTY    = parseInt(process.env.STOCK_QTY    || '100000', 10); // 🔥 각 상품 재고

async function refreshAccessToken() {
    const res = await axios.post('http://localhost:8081/api/auth/refresh', { refreshToken });
    accessToken  = res.data.data.accessToken;
    refreshToken = res.data.data.refreshToken;
}

async function createProduct(index) {
    let retries = 2;
    while (retries-- >= 0) {
        try {
            const now = new Date();
            const startAt = now.toISOString();
            const endAt = new Date(now.getTime() + 10 * 24 * 60 * 60 * 1000).toISOString();

            await axios.post(
                '/api/admin/products',
                {
                    productName: `테스트상품${index}`,
                    productPrice: 10000 + index * 1000,
                    description: `테스트 상품 상세 설명 ${index}`,
                    stockQuantity: STOCK_QTY,             // ⬅️ 여기만 크게
                    category: 'FOOD',
                    saleStartAt: startAt,
                    saleEndAt: endAt,
                },
                { baseURL: 'http://localhost:8085', headers: { Authorization: `Bearer ${accessToken}` } }
            );
            console.log(`✅ 상품 ${index} 생성 (재고 ${STOCK_QTY})`);
            return;
        } catch (e) {
            if (e.response?.status === 401) {
                console.warn('⚠️ 토큰 만료 → 갱신');
                await refreshAccessToken();
                continue;
            }
            console.error('❌ 실패:', e.response?.data || e.message);
            return;
        }
    }
}

(async function main() {
    console.log('🚀 상품 생성 시작');
    for (let i = 1; i <= NUM_PRODUCTS; i++) {
        await createProduct(i);
    }
    console.log('🎉 완료');
})();
