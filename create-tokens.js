const axios = require('axios');
const fs = require('fs');

const NUM_USERS = 1000;
const BATCH_SIZE = 8;
const tokens = [];
const failedUsers = [];

const sleep = (ms) => new Promise(r => setTimeout(r, ms));

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

    // 1) 회원가입은 '한 번만' 시도 (409면 이미 존재 → OK로 간주)
    try {
        await axios.post('http://localhost:8082/api/members/signup', {
            loginId, email, password, name, phone, address
        }, { timeout: 5000 });
    } catch (error) {
        const status = error.response?.status;
        const code = error.response?.data?.errorCode;
        if (status === 409 || code === 'MEMBER_DUPLICATE_VALUE') {
            // 이미 만들어진 계정이면 가입은 통과로 본다
            console.warn(`⚠️ ${i + 1}: 이미 존재하는 계정(409) → 로그인만 진행`);
        } else {
            console.warn(`❌ ${i + 1}: 회원가입 실패:`, error.response?.data || error.message);
            failedUsers.push(loginId);
            return;
        }
    }

    // 2) 로그인은 별도 재시도 루프 (가입 재시도 금지!)
    let retry = 0;
    const maxRetry = 3;

    while (retry < maxRetry) {
        try {
            const res = await axios.post('http://localhost:8081/api/auth/login', {
                loginId, password
            }, { timeout: 5000 });

            tokens.push({
                loginId,
                token: res.data.data.accessToken,
                refreshToken: res.data.data.refreshToken
            });

            console.log(`✅ ${i + 1}/${NUM_USERS} 생성 완료 (login retry: ${retry})`);
            return;
        } catch (error) {
            retry++;
            const status = error.response?.status;
            console.warn(`⚠️ ${i + 1} 로그인 실패 (retry: ${retry}):`, error.response?.data || error.message);

            // 401은 자격 증명 실패 → 재가입 금지, 재시도해도 소용없으면 바로 종료
            if (status === 401) {
                console.warn(`⛔ ${i + 1}: 401(자격 증명 실패) → 중단`);
                break;
            }

            // 5xx/네트워크 지연 등만 짧게 백오프 후 재시도
            await sleep(500 * retry);
        }
    }

    console.error(`❌ ${i + 1}번째 실패 (로그인 최대 재시도 초과)`);
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
