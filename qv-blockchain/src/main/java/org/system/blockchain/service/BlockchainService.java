package org.system.blockchain.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.system.blockchain.config.BlockchainRabbitConfig;
import org.system.common.event.VoteArchivedEvent;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@Slf4j
public class BlockchainService {

    @Value("${blockchain.rpc-url}")
    private String rpcUrl;

    @Value("${blockchain.private-key}")
    private String privateKey;

    private Web3j web3j;
    private Credentials credentials;

    private final RabbitTemplate rabbitTemplate;

    public BlockchainService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void init() {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = Credentials.create(privateKey);

        log.info("============== BLOCKCHAIN IDENTITY CHECK ==============");
        log.info("RPC URL: {}", rpcUrl);
        log.info("Java использует адрес: {}", credentials.getAddress());
        log.info("=======================================================");

        try {
            var balance = web3j.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
            log.info("💰 Баланс майнера (wei): {}", balance.getBalance());
        } catch (Exception e) {
            log.error("Не удалось проверить баланс при старте", e);
        }
    }

    public void writeVoteToBlockchain(Long voteId, Long userId, Long projectId, Integer voteCount, BigDecimal cost) {
        try {
            String auditData = String.format("VOTE-CONFIRMED: ID=%d | User=%d | Project=%d | Cost=%s",
                    voteId, userId, projectId, cost.toString());

            String hexData = toHex(auditData);

            long chainId = 777L;
            RawTransactionManager manager = new RawTransactionManager(web3j, credentials, chainId);

            // Отправляем транзакцию (0 ETH, но с данными)
            var ethSendTransaction = manager.sendTransaction(
                    BigInteger.valueOf(22_000_000_000L), // Gas Price
                    BigInteger.valueOf(500_000L),        // Gas Limit
                    credentials.getAddress(),            // Отправляем самому себе
                    hexData,                             // Данные
                    BigInteger.ZERO                      // Сумма 0
            );

            if (ethSendTransaction.hasError()) {
                log.error("ОШИБКА БЛОКЧЕЙНА: {}", ethSendTransaction.getError().getMessage());
                return;
            }

            String txHash = ethSendTransaction.getTransactionHash();
            log.info("ЗАПИСАНО В БЛОКЧЕЙН! TxHash: {}", txHash);
            log.info("Данные: {}", auditData);

            VoteArchivedEvent event = new VoteArchivedEvent(
                    voteId,
                    userId,
                    projectId,
                    voteCount,
                    cost,
                    txHash,
                    LocalDateTime.now()
            );

            rabbitTemplate.convertAndSend(
                    BlockchainRabbitConfig.EXCHANGE_NAME,
                    "vote.archived",
                    event
            );
            log.info("Отправлено событие vote.archived для ID: {}", voteId);

        } catch (Exception e) {
            log.error("Критическая ошибка Web3j: ", e);
        }
    }

    private String toHex(String arg) {
        return String.format("%040x", new BigInteger(1, arg.getBytes(StandardCharsets.UTF_8)));
    }
}