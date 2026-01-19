package org.system.wallet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.system.wallet.entity.Wallet;
import org.system.wallet.repository.WalletRepository;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletRepository walletRepository;

    @PostMapping("/login/{telegramId}")
    @Transactional
    public ResponseEntity<Wallet> loginOrRegister(@PathVariable Long telegramId) {
        return ResponseEntity.ok(
                walletRepository.findById(telegramId)
                        .orElseGet(() -> {
                            log.info("Регистрация нового пользователя: {}", telegramId);
                            Wallet newWallet = new Wallet(telegramId, new BigDecimal("1000.00"));
                            return walletRepository.save(newWallet);
                        })
        );
    }

    @PostMapping("/charge")
    @Transactional
    public ResponseEntity<String> charge(@RequestParam Long userId, @RequestParam BigDecimal amount) {
        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (wallet.getBalance().compareTo(amount) < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Недостаточно средств (нужно " + amount + " QV)");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        log.info("Списано {} QV у пользователя {}", amount, userId);
        return ResponseEntity.ok("Списано");
    }
}