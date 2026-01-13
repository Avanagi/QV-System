package org.system.wallet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.system.wallet.entity.Wallet;
import org.system.wallet.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.Random;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletRepository walletRepository;
    private final Random random = new Random();

    @Value("${app.chaos.enabled:false}")
    private boolean chaosEnabled;

    @Value("${app.chaos.rate:0.0}")
    private double chaosRate;

    @PostMapping("/charge")
    @Transactional
    public ResponseEntity<String> chargeSync(@RequestParam Long userId, @RequestParam BigDecimal amount) {

        if (chaosEnabled && random.nextDouble() < chaosRate) {
            log.error("💥 CHAOS MONKEY: HTTP 500 при списании!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("CHAOS_DB_ERROR");
        }

        Wallet wallet = walletRepository.findById(userId).orElse(null);
        if (wallet == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient funds");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        log.info("HTTP Charge Success: -{} from User {}", amount, userId);
        return ResponseEntity.ok("Charged");
    }
}