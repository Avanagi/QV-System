package org.system.wallet.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.system.wallet.entity.Wallet;

@Data
@AllArgsConstructor
public class AuthResponse {
    private Wallet user;
    private String token;
}