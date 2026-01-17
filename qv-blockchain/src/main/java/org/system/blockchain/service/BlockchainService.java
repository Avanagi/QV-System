package org.system.blockchain.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class BlockchainService {

    @Value("${blockchain.rpc-url}")
    private String rpcUrl;

    @Value("${blockchain.private-key}")
    private String privateKey;

    private Web3j web3j;
    private Credentials credentials;

    @PostConstruct
    public void init() {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = Credentials.create(privateKey);

        log.info("============== BLOCKCHAIN IDENTITY CHECK ==============");
        log.info("RPC URL: {}", rpcUrl);
        log.info("Java использует адрес: {}", credentials.getAddress());
        log.info("Ожидаемый адрес (с деньгами): 0x123463a4b065722e99115d6c222f267d9cabb524");
        log.info(privateKey);
        log.info("=======================================================");

        try {
            var balance = web3j.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
            log.info("Баланс этого адреса (Java): {}", balance.getBalance());
        } catch (Exception e) {
            log.error("Не удалось проверить баланс при старте", e);
        }
    }

    public void writeVoteToBlockchain(Long voteId, Long userId, Long projectId, BigDecimal cost) {
        try {
            String auditData = String.format("VOTE-CONFIRMED: ID=%d | User=%d | Project=%d | Cost=%s",
                    voteId, userId, projectId, cost.toString());
            String hexData = toHex(auditData);

            long chainId = 777L;
            RawTransactionManager manager = new RawTransactionManager(web3j, credentials, chainId);

            // Отправляем транзакцию и ПОЛУЧАЕМ ОТВЕТ
            var ethSendTransaction = manager.sendTransaction(
                    BigInteger.valueOf(22_000_000_000L), // Gas Price (22 Gwei)
                    BigInteger.valueOf(500_000L),        // Gas Limit (подняли лимит)
                    credentials.getAddress(),            // To
                    hexData,                             // Data
                    BigInteger.ZERO                      // Value
            );

            // ПРОВЕРЯЕМ ОШИБКИ
            if (ethSendTransaction.hasError()) {
                log.error("❌ ОШИБКА БЛОКЧЕЙНА: {}", ethSendTransaction.getError().getMessage());
                return;
            }

            String txHash = ethSendTransaction.getTransactionHash();
            log.info("✅ ЗАПИСАНО В БЛОКЧЕЙН! TxHash: {}", txHash);
            log.info("📝 Данные: {}", auditData);

        } catch (Exception e) {
            log.error("Критическая ошибка Web3j: ", e);
        }
    }

    private String toHex(String arg) {
        return String.format("%040x", new BigInteger(1, arg.getBytes(StandardCharsets.UTF_8)));
    }
}