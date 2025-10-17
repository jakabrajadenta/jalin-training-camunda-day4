package co.id.jalin.camunda.training.day4.repository;

import co.id.jalin.camunda.training.day4.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction,Long> {
}
