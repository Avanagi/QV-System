package org.system.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.system.wallet.entity.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
}