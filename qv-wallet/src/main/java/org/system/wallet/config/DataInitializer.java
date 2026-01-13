package org.system.wallet.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.system.wallet.entity.Wallet;
import org.system.wallet.repository.WalletRepository;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final WalletRepository walletRepository;

    @Override
    public void run(String... args) {
        if (!walletRepository.existsById(101L)) {
            walletRepository.save(new Wallet(101L, new BigDecimal("1000000.00")));
            System.out.println(">>> WALLET CREATED: User 101 has 1000 credits <<<");
        }
    }
}