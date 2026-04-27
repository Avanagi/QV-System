package org.system.blockchain.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.system.blockchain.dto.TransactionDto;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExplorerService {

    @Value("${blockchain.rpc-url}")
    private String rpcUrl;

    @Value("${blockchain.contract-address}")
    private String contractAddress;

    private Web3j web3j;

    @PostConstruct
    public void init() {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
    }

    public List<TransactionDto> getAllTransactions() {
        List<TransactionDto> result = new ArrayList<>();
        try {
            BigInteger latestBlock = web3j.ethBlockNumber().send().getBlockNumber();

            BigInteger startBlock = latestBlock.subtract(BigInteger.valueOf(1000));
            if (startBlock.compareTo(BigInteger.ZERO) < 0) {
                startBlock = BigInteger.ZERO;
            }

            log.info("Загрузка эксплорера с блока {} до {}", startBlock, latestBlock);

            for (BigInteger i = latestBlock; i.compareTo(startBlock) >= 0; i = i.subtract(BigInteger.ONE)) {
                EthBlock.Block block = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(i), true).send().getBlock();

                if (block != null && block.getTransactions() != null) {
                    for (EthBlock.TransactionResult txResult : block.getTransactions()) {
                        Transaction tx = (Transaction) txResult.get();

                        if (tx.getTo() != null && contractAddress != null && tx.getTo().equalsIgnoreCase(contractAddress.trim())) {

                            String data = "Вызов смарт-контракта QVStorage. Method ABI: " +
                                    (tx.getInput().length() > 10 ? tx.getInput().substring(0, 10) + "..." : "0x");

                            result.add(new TransactionDto(block.getNumber().toString(), tx.getHash(), data));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при чтении блокчейна", e);
        }
        return result;
    }
}