const axios = require('axios');

// ✅ 최신 발급된 토큰으로 교체
let accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwidXNlcm5hbWUiOiJhZG1pblVzZXIiLCJyb2xlIjoiUk9MRV9BRE1JTiIsImRldmljZUlkIjoiZDUzZDc1N2EtNzA5OC00MmI0LWFjY2YtZDBjNzE1MDcxMjU3IiwiaWF0IjoxNzU4MTEyMjU1LCJleHAiOjE3NTgxOTg2NTV9.V6fp781r8uD1cnOWnuOoGsxJkG1kxQbvXWyq-sze3-Y";
let refreshToken = "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwidXNlcm5hbWUiOiJhZG1pblVzZXIiLCJyb2xlIjoiUk9MRV9BRE1JTiIsImRldmljZUlkIjoiZDUzZDc1N2EtNzA5OC00MmI0LWFjY2YtZDBjNzE1MDcxMjU3IiwiaWF0IjoxNzU4MTEyMjU1LCJleHAiOjE3NTg3MTcwNTV9.rgrXyoR-h_hQkLpS3XqVUQE6eC2WWH5-j7vxqoK2jmQ";

async function refreshAccessToken() {
    console.log('🔄 Access token 갱신 시도');
    try {
        const res = await axios.post('http://localhost:8081/api/auth/refresh', {
            refreshToken
        });
        accessToken = res.data.data.accessToken;
        refreshToken = res.data.data.refreshToken;
        console.log('✅ Access token 갱신 완료');
    } catch (error) {
        console.error('❌ 토큰 갱신 실패:', error.response?.data || error.message);
        throw new Error('Refresh token 갱신 실패');
    }
}

async function createProduct(index) {
    let retries = 3;
    while (retries > 0) {
        try {
            const now = new Date();
            const startAt = now.toISOString();
            const endAt = new Date(now.getTime() + 10 * 24 * 60 * 60 * 1000).toISOString(); // 10일 뒤

            await axios.post(
                '/api/admin/products',
                {
                    productName: `테스트상품${index}`,
                    productPrice: 10000 + index * 1000,
                    description: `테스트 상품 상세 설명 ${index}`,
                    stockQuantity: 6000,
                    category: 'FOOD',
                    saleStartAt: startAt,
                    saleEndAt: endAt
                },
                {
                    baseURL: 'http://localhost:8085',
                    headers: {
                        Authorization: `Bearer ${accessToken}`
                    }
                }
            );
            console.log(`✅ 상품 ${index} 생성 완료`);
            return;
        } catch (error) {
            if (error.response && error.response.status === 401) {
                console.warn(`⚠️ 상품 ${index} 생성 실패 (Access token 만료) → 토큰 갱신 시도`);
                await refreshAccessToken();
                retries -= 1;
                console.log(`🔁 상품 ${index} 생성 재시도 (남은 횟수: ${retries})`);
                console.log('💬 재시도 직후 accessToken:', accessToken);
            } else {
                console.error(`❌ 상품 ${index} 생성 실패:`, error.response?.data || error.message);
                if (error.response) {
                    console.error(`🔎 [상태코드]: ${error.response.status}`);
                    console.error(`🔎 [응답내용]: ${JSON.stringify(error.response.data)}`);
                }
                return;
            }
        }
    }
    console.log(`⚠️ 상품 ${index} 생성 실패: 재시도 횟수 초과`);
}

async function main() {
    console.log('🚀 상품 생성 시작');

    for (let i = 1; i <= 10; i++) {
        console.log(`\n=== 상품 ${i} 생성 시도 ===`);
        console.log('💬 현재 accessToken:', accessToken);
        await createProduct(i);
    }

    console.log('🎉 모든 상품 등록 시도 완료!');
}

main();