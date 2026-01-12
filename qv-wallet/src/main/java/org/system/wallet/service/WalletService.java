package org.system.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.system.common.event.FundsFailedEvent;
import org.system.common.event.FundsReservedEvent;
import org.system.common.event.VoteCreatedEvent;
import org.system.wallet.config.WalletRabbitConfig;
import org.system.wallet.entity.Wallet;
import org.system.wallet.repository.WalletRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void processPayment(VoteCreatedEvent event) {
        log.info("Попытка списания: User={}, Cost={}", event.getUserId(), event.getCost());

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

        log.info("Успех! Списано {}. Остаток: {}", event.getCost(), wallet.getBalance());

        FundsReservedEvent successEvent = new FundsReservedEvent(event.getVoteId(), event.getUserId());
        rabbitTemplate.convertAndSend(WalletRabbitConfig.EXCHANGE_NAME, "wallet.reserved", successEvent);
    }

    private void sendFailure(VoteCreatedEvent event, String reason) {
        log.error("Отказ списания: {}", reason);
        FundsFailedEvent failEvent = new FundsFailedEvent(event.getVoteId(), event.getUserId(), reason);
        rabbitTemplate.convertAndSend(WalletRabbitConfig.EXCHANGE_NAME, "wallet.failed", failEvent);
    }
}