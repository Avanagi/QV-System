package org.system.wallet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.system.wallet.repository.WalletRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletScheduler {

    private final WalletRepository walletRepository;

    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void restoreBalancesMonthly() {
        walletRepository.restoreBalances(new BigDecimal("1000.00"));
    }
}