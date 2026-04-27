package org.system.wallet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true)
    private String username;

    private String password;

    @Column(unique = true)
    private Long telegramId;

    private BigDecimal balance;

    public Wallet(String username, String password, Long telegramId, BigDecimal balance) {
        this.username = username;
        this.password = password;
        this.telegramId = telegramId;
        this.balance = balance;
    }
}