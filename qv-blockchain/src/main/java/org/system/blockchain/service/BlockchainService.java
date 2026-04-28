package org.system.blockchain.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.system.blockchain.config.BlockchainRabbitConfig;
import org.system.blockchain.contract.QVStorage;
import org.system.common.event.VoteArchivedEvent;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.tx.FastRawTransactionManager; // <--- НОВЫЙ ИМПОРТ
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Service
@Slf4j
public class BlockchainService {

    @Value("${blockchain.rpc-url}")
    private String rpcUrl;

    @Value("${blockchain.private-key}")
    private String privateKey;

    @Value("${blockchain.bytecode}")
    private String contractBytecode;

    @Value("${blockchain.contract-address}")
    private String contractAddress;

    private Web3j web3j;
    private QVStorage qvContract;
    private final RabbitTemplate rabbitTemplate;

    public BlockchainService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            this.web3j = Web3j.build(new HttpService(rpcUrl));
            Credentials credentials = Credentials.create(privateKey);
            long currentChainId = web3j.ethChainId().send().getChainId().longValue();

            TransactionManager txManager = new FastRawTransactionManager(web3j, credentials, currentChainId);

            StaticGasProvider gasProvider = new StaticGasProvider(
                    BigInteger.valueOf(22_000_000_000L),
                    BigInteger.valueOf(3_000_000L)
            );

            log.info("============== SMART CONTRACT INIT ==============");
            if (contractAddress == null || contractAddress.isBlank()) {
                log.info("Начинаю деплой смарт-контракта...");
                String hexBytecode = contractBytecode.startsWith("0x") ? contractBytecode : "0x" + contractBytecode;

                this.qvContract = QVStorage.deploy(web3j, txManager, gasProvider, hexBytecode).send();

                log.info("🎉 КОНТРАКТ УСПЕШНО РАЗВЕРНУТ!");
                log.warn("⚠️ АДРЕС: {} (Сохраните его в application.yml)", qvContract.getContractAddress());
            } else {
                log.info("Загрузка существующего контракта по адресу: {}", contractAddress);
                this.qvContract = QVStorage.load(contractAddress, web3j, txManager, gasProvider);
            }
            org.web3j.protocol.core.methods.request.EthFilter filter =
                    new org.web3j.protocol.core.methods.request.EthFilter(
                            DefaultBlockParameterName.LATEST,
                            DefaultBlockParameterName.LATEST,
                            this.contractAddress
                    );

            web3j.ethLogFlowable(filter).subscribe(eventLog -> {
                log.warn("🔥 [EVM EVENT] Блокчейн сгенерировал событие в блоке №{}!", eventLog.getBlockNumber());
                log.info("🔗 TxHash события: {}", eventLog.getTransactionHash());
            }, error -> {
                log.error("Ошибка при прослушивании EVM: ", error);
            });
            log.info("===============================================");
        } catch (Exception e) {
            log.error("❌ ОШИБКА ИНИЦИАЛИЗАЦИИ БЛОКЧЕЙНА: {}", e.getMessage());
            throw new RuntimeException("Не удалось инициализировать смарт-контракт", e);
        }
    }

    public void writeVoteToBlockchain(Long voteId, Long userId, Long optionId, Integer voteCount, BigDecimal cost,
                                      String pollTitle, String optionText, Long pollId) {

        log.info("🚀 Асинхронная отправка транзакции для голоса {}...", voteId);

        qvContract.saveVote(
                BigInteger.valueOf(voteId),
                BigInteger.valueOf(userId),
                BigInteger.valueOf(pollId),
                BigInteger.valueOf(optionId),
                BigInteger.valueOf(voteCount),
                BigInteger.valueOf(cost.longValue())
        ).sendAsync().thenAccept(receipt -> {

            if (!receipt.isStatusOK()) {
                log.error("СМАРТ-КОНТРАКТ ОТКЛОНИЛ ТРАНЗАКЦИЮ для голоса {}", voteId);
                return;
            }

            String txHash = receipt.getTransactionHash();
            log.info("БЛОК СМАЙНЕН! Голос {} зафиксирован. TxHash: {}", voteId, txHash);

            VoteArchivedEvent event = new VoteArchivedEvent(
                    voteId,
                    userId,
                    optionId,
                    voteCount,
                    cost,
                    txHash,
                    pollTitle,
                    optionText,
                    pollId,
                    LocalDateTime.now());

            rabbitTemplate.convertAndSend(BlockchainRabbitConfig.EXCHANGE_NAME, "vote.archived", event);

        }).exceptionally(ex -> {
            log.error("❌ Сетевая ошибка при асинхронной отправке транзакции: ", ex);
            return null;
        });
    }
}
