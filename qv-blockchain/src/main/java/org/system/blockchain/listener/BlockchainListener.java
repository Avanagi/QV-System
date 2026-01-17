package org.system.blockchain.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.system.blockchain.config.BlockchainRabbitConfig;
import org.system.blockchain.service.BlockchainService;
import org.system.common.event.FundsReservedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlockchainListener {

    private final BlockchainService blockchainService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("blockchain-queue"),
            exchange = @Exchange(value = BlockchainRabbitConfig.EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = "wallet.reserved"
    ))
    public void handleConfirmedVote(FundsReservedEvent event) {
        log.info("📥 Получен оплаченный голос ID: {}", event.getVoteId());

        blockchainService.writeVoteToBlockchain(
                event.getVoteId(),
                event.getUserId(),
                event.getProjectId(),
                event.getCost()
        );
    }
}