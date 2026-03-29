package org.system.wallet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.system.wallet.dto.AuthResponse;
import org.system.wallet.entity.Wallet;
import org.system.wallet.repository.WalletRepository;
import org.system.wallet.dto.LoginRequest;
import org.system.wallet.util.JwtUtil;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    @Value("${app.initial-balance:1000.00}")
    private BigDecimal initialBalance;

    private final WalletRepository walletRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/login/{telegramId}")
    @Transactional
    public ResponseEntity<AuthResponse> loginOrRegister(@PathVariable Long telegramId) {
        Wallet wallet = walletRepository.findById(telegramId)
                .orElseGet(() -> {
                    log.info("Регистрация нового пользователя: {}. Баланс: {}", telegramId, initialBalance);
                    return walletRepository.save(new Wallet(telegramId, initialBalance));
                });

        String token = jwtUtil.generateToken(wallet.getUserId());
        return ResponseEntity.ok(new AuthResponse(wallet, token));
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

    @PostMapping("/login/web")
    public ResponseEntity<?> webLogin(@RequestBody LoginRequest request) {
        if ("user".equals(request.getUsername()) && "password".equals(request.getPassword())) {
            return loginOrRegister(777L);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Неверный логин или пароль");
    }
}