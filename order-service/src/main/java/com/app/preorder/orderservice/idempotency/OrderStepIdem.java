package com.app.preorder.orderservice.idempotency;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_steps_idem",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_order_step_key",
                columnNames = {"order_id","step","idem_key"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderStepIdem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "step", nullable = false, length = 32)
    private String step;      // ATTEMPT / COMPLETE / CANCEL / RETURN ...

    @Column(name = "idem_key", nullable = false, length = 128)
    private String idemKey;   // null/blank -> "auto:{STEP}:{orderId}"

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}