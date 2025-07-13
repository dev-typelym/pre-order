const axios = require('axios');
const fs = require('fs');

const NUM_USERS = 5000;
const tokens = [];
const failedUsers = []; // 실패한 유저 기록

async function main() {
    for (let i = 0; i < NUM_USERS; i++) {
        const loginId = `user${i}`;
        const email = `user${i}@example.com`;
        const password = `Password!${i}`;
        const name = `User${i}`;
        const phone = `010-0000-${String(i).padStart(4, '0')}`;

        // ✅ 유저별 고유 주소 생성
        const address = {
            roadAddress: `성남시 수정구 수정로 ${i + 1}길`,
            detailAddress: `${i + 1}층`,
            postalCode: `131${String(i % 100).padStart(2, '0')}`
        };

        let retry = 0;
        let success = false;

        while (retry < 3 && !success) {
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

                tokens.push({ token: res.data.data.accessToken });
                console.log(`✅ ${i + 1}/${NUM_USERS} 생성 완료 (retry: ${retry})`);
                success = true;
            } catch (error) {
                retry++;
                console.warn(`⚠️ ${i + 1}번째 실패 시도 (retry: ${retry}):`, error.response?.data || error.message);

                // 재시도 전 delay
                await new Promise(resolve => setTimeout(resolve, 500 * retry)); // 지수 백오프
            }
        }

        if (!success) {
            console.error(`❌ ${i + 1}번째 실패 (최대 재시도 초과)`);
            failedUsers.push(loginId);
        }

        // 각 유저 간 짧은 delay → 과부하 방지
        await new Promise(resolve => setTimeout(resolve, 50));
    }

    // 토큰 파일 저장
    fs.writeFileSync('./tokens.json', JSON.stringify(tokens, null, 2));
    console.log(`💾 tokens.json 저장 완료 (총 ${tokens.length}명)`);

    // 실패 유저 파일 저장
    if (failedUsers.length > 0) {
        fs.writeFileSync('./failed_users.json', JSON.stringify(failedUsers, null, 2));
        console.warn(`⚠️ 실패한 유저 ${failedUsers.length}명 기록됨 (failed_users.json)`);
    } else {
        console.log('✅ 모든 유저 성공적으로 생성');
    }
}

main();