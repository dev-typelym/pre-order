const axios = require('axios');
const fs = require('fs');

const NUM_USERS = 5000;
const tokens = [];

async function main() {
    for (let i = 0; i < NUM_USERS; i++) {
        const loginId = `user${i}`;
        const email = `user${i}@example.com`;
        const password = `Password!${i}`;
        const name = `User${i}`;
        const phone = `010-0000-${String(i).padStart(4, '0')}`;
        const address = 'Seoul';

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

            tokens.push({ token: res.data.accessToken });
            console.log(`✅ ${i + 1}/${NUM_USERS} 생성 완료`);
        } catch (error) {
            console.error(`❌ ${i + 1}번째 실패:`, error.response?.data || error.message);
        }
    }

    fs.writeFileSync('./tokens.json', JSON.stringify(tokens, null, 2));
}

main();