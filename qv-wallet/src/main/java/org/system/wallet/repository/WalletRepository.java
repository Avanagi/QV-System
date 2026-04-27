package org.system.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.system.wallet.entity.Wallet;
import java.math.BigDecimal;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByTelegramId(Long telegramId);
    Optional<Wallet> findByUsername(String username);

    @Modifying
    @Query("UPDATE Wallet w SET w.balance = :maxBalance WHERE w.balance < :maxBalance")
    void restoreBalances(BigDecimal maxBalance);
}