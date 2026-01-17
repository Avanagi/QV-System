package org.system.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.system.common.event.FundsFailedEvent;
import org.system.common.event.FundsReservedEvent;
import org.system.common.event.VoteCreatedEvent;
import org.system.wallet.config.WalletRabbitConfig;
import org.system.wallet.entity.ProcessedVote;
import org.system.wallet.entity.Wallet;
import org.system.wallet.repository.ProcessedVoteRepository;
import org.system.wallet.repository.WalletRepository;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final ProcessedVoteRepository processedVoteRepository;

    private final WalletRepository walletRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Random random = new Random();

    @Value("${app.chaos.enabled:false}")
    private boolean chaosEnabled;

    @Value("${app.chaos.rate:0.0}")
    private double chaosRate;

    @Transactional
    public void processPayment(VoteCreatedEvent event) {
        if (processedVoteRepository.existsById(event.getVoteId())) {
            log.warn("Голос {} уже был обработан. Пропускаем.", event.getVoteId());
            sendSuccess(event);
            return;
        }

        log.info("Обработка платежа: User={}, Cost={}", event.getUserId(), event.getCost());

        if (chaosEnabled && random.nextDouble() < chaosRate) {
            log.error("💥 CHAOS MONKEY: Имитация сбоя базы данных для голоса {}!", event.getVoteId());
            sendFailure(event, "CHAOS_DB_ERROR");
            return;
        }

        Wallet wallet = walletRepository.findById(event.getUserId()).orElse(null);

        if (wallet == null) {
            sendFailure(event, "User wallet not found");
            return;
        }

        if (wallet.getBalance().compareTo(event.getCost()) < 0) {
            sendFailure(event, "Insufficient funds");
            return;
        }

        wallet.setBalance(wallet.getBalance().subtract(event.getCost()));
        walletRepository.save(wallet);

        processedVoteRepository.save(new ProcessedVote(event.getVoteId()));

        sendSuccess(event);

        log.info("Успех! Списано {}. Остаток: {}", event.getCost(), wallet.getBalance());

        FundsReservedEvent successEvent = new FundsReservedEvent(event.getVoteId(), event.getUserId(),
                event.getProjectId(),  event.getCost(), event.getVoteCount());
        rabbitTemplate.convertAndSend(WalletRabbitConfig.EXCHANGE_NAME, "wallet.reserved", successEvent);
    }

    private void sendFailure(VoteCreatedEvent event, String reason) {
        FundsFailedEvent failEvent = new FundsFailedEvent(event.getVoteId(), event.getUserId(), reason);
        rabbitTemplate.convertAndSend(WalletRabbitConfig.EXCHANGE_NAME, "wallet.failed", failEvent);
    }

    private void sendSuccess(VoteCreatedEvent event) {
        FundsReservedEvent successEvent = new FundsReservedEvent(
                event.getVoteId(),
                event.getUserId(),
                event.getProjectId(),
                event.getCost(),
                event.getVoteCount()
        );
        rabbitTemplate.convertAndSend(WalletRabbitConfig.EXCHANGE_NAME, "wallet.reserved", successEvent);
    }
}