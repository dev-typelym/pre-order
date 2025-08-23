package com.app.preorder.orderservice.idempotency;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderStepIdempotency {

    @PersistenceContext
    private EntityManager em;

    private String safe(String key, Long orderId, String step) {
        return (key == null || key.isBlank()) ? "auto:" + step + ":" + orderId : key;
    }

    /** UNIQUE 제약 걸어 중복 재진입 차단. 이미 처리된 키면 AlreadyProcessedException */
    @Transactional
    public void begin(Long orderId, String step, String idemKey) {
        String k = safe(idemKey, orderId, step);
        try {
            em.persist(OrderStepIdem.builder()
                    .orderId(orderId).step(step).idemKey(k).build());
            em.flush(); // 제약 위반을 즉시 감지
        } catch (DataIntegrityViolationException | PersistenceException dup) {
            throw new AlreadyProcessedException();
        }
    }

    /** 실패 시 되돌릴 때만 사용(성공이면 남겨둬도 됨) */
    @Transactional
    public void undo(Long orderId, String step, String idemKey) {
        String k = safe(idemKey, orderId, step);
        em.createQuery("delete from OrderStepIdem e where e.orderId=:oid and e.step=:s and e.idemKey=:k")
                .setParameter("oid", orderId)
                .setParameter("s", step)
                .setParameter("k", k)
                .executeUpdate();
    }

    public static class AlreadyProcessedException extends RuntimeException {}
}
