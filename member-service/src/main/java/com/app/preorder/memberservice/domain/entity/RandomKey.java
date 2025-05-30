package com.app.preorder.memberservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@ToString
@Table(name = "tbl_random_key")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RandomKey {
    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @NonNull
    private String randomKey;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    public String getTmpPassword() {
        char[] charSet = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
                'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

        String pwd = "";

        /* 문자 배열 길이의 값을 랜덤으로 10개를 뽑아 조합 */
        int idx = 0;
        for (int i = 0; i < 10; i++) {
            idx = (int) (charSet.length * Math.random());
            pwd += charSet[idx];
        }

        return pwd;
    }

    public RandomKey(Long id, String randomKey, Member member) {
        this.id = id;
        this.randomKey = randomKey;
        this.member = member;
    }

    public RandomKey(Member member) {
        this.id = getId();
        this.randomKey = getTmpPassword();
        this.member = member;
    }
}