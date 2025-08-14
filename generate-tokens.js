const axios = require('axios');
const fs = require('fs');

const NUM_USERS = 5000;
const BATCH_SIZE = 30;
const tokens = [];
const failedUsers = [];

async function createUser(i) {
    const loginId = `user${i}`;
    const email = `user${i}@example.com`;
    const password = `Password!${i}`;
    const name = `User${i}`;
    const phone = `010-0000-${String(i + 1).padStart(4, '0')}`;

    const address = {
        roadAddress: `성남시 수정구 수정로 ${i + 1}길`,
        detailAddress: `${i + 1}층`,
        postalCode: `131${String(i % 100).padStart(2, '0')}`
    };

    let retry = 0;
    const maxRetry = 3;

    while (retry < maxRetry) {
        try {
            // 회원가입
            await axios.post('http://localhost:8082/api/members/signup', {
                loginId,
                email,
                password,
                name,
                phone,
                address
            });

            // 로그인
            const res = await axios.post('http://localhost:8081/api/auth/login', {
                loginId,
                password
            });

            tokens.push({
                loginId,
                token: res.data.data.accessToken,
                refreshToken: res.data.data.refreshToken
            });

            console.log(`✅ ${i + 1}/${NUM_USERS} 생성 완료 (retry: ${retry})`);
            return;
        } catch (error) {
            retry++;
            console.warn(`⚠️ ${i + 1}번째 실패 시도 (retry: ${retry}):`, error.response?.data || error.message);
            await new Promise(resolve => setTimeout(resolve, 500 * retry));
        }
    }

    console.error(`❌ ${i + 1}번째 실패 (최대 재시도 초과)`);
    failedUsers.push(loginId);
}

async function main() {
    for (let i = 0; i < NUM_USERS; i += BATCH_SIZE) {
        const batch = [];

        for (let j = i; j < i + BATCH_SIZE && j < NUM_USERS; j++) {
            batch.push(createUser(j));
        }

        await Promise.all(batch);
        console.log(`✅ Batch 완료: ${i + 1} ~ ${Math.min(i + BATCH_SIZE, NUM_USERS)}`);
    }

    // 결과 파일 저장
    fs.writeFileSync('./tokens.json', JSON.stringify(tokens, null, 2));
    console.log(`💾 tokens.json 저장 완료 (총 ${tokens.length}명)`);

    if (failedUsers.length > 0) {
        fs.writeFileSync('./failed_users.json', JSON.stringify(failedUsers, null, 2));
        console.warn(`⚠️ 실패한 유저 ${failedUsers.length}명 기록됨 (failed_users.json)`);
    } else {
        console.log('✅ 모든 유저 성공적으로 생성');
    }
}

main();