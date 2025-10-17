package co.id.jalin.camunda.training.day4.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transaction")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long processInstanceKey;
    private String cardNumber;
    private String merchant;
    private String issuer;
    private String status;
    private Double amount;

    @Column(length = 500)
    private String message;
    private LocalDateTime createdAt = LocalDateTime.now();
}
