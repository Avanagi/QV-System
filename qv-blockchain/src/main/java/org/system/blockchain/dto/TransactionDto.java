package org.system.blockchain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransactionDto {
    private String blockNumber;
    private String txHash;
    private String data;
}