package org.system.wallet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.system.wallet.dto.AuthResponse;
import org.system.wallet.dto.LoginRequest;
import org.system.wallet.dto.RegisterRequest;
import org.system.wallet.entity.Wallet;
import org.system.wallet.repository.WalletRepository;
import org.system.wallet.util.JwtUtil;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletRepository walletRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.initial-balance:1000.00}")
    private BigDecimal initialBalance;

    @PostMapping("/login/tg/{telegramId}")
    @Transactional
    public ResponseEntity<AuthResponse> loginTg(@PathVariable Long telegramId, @RequestParam String username) {
        Wallet wallet = walletRepository.findByTelegramId(telegramId)
                .orElseGet(() -> walletRepository.save(
                        new Wallet(username, null, telegramId, initialBalance)
                ));

        String token = jwtUtil.generateToken(wallet.getUserId());
        return ResponseEntity.ok(new AuthResponse(wallet, token));
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> registerWeb(@RequestBody RegisterRequest request) {
        if (walletRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username exists");
        }

        String hashedPassword = DigestUtils.md5DigestAsHex(request.getPassword().getBytes());
        Wallet wallet = new Wallet(request.getUsername(), hashedPassword, null, initialBalance);
        walletRepository.save(wallet);

        String token = jwtUtil.generateToken(wallet.getUserId());
        return ResponseEntity.ok(new AuthResponse(wallet, token));
    }

    @PostMapping("/login/web")
    public ResponseEntity<?> loginWeb(@RequestBody LoginRequest request) {
        Optional<Wallet> walletOpt = walletRepository.findByUsername(request.getUsername());

        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            String hashedPassword = DigestUtils.md5DigestAsHex(request.getPassword().getBytes());

            if (hashedPassword.equals(wallet.getPassword())) {
                String token = jwtUtil.generateToken(wallet.getUserId());
                return ResponseEntity.ok(new AuthResponse(wallet, token));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @PostMapping("/charge")
    @Transactional
    public ResponseEntity<String> charge(@RequestParam Long userId, @RequestParam BigDecimal amount) {
        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (wallet.getBalance().compareTo(amount) < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient funds");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        return ResponseEntity.ok("Charged");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Wallet> getWallet(@PathVariable Long userId) {
        return walletRepository.findById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}