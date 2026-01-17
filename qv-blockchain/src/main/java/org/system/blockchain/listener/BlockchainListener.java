package org.system.blockchain.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.system.common.event.FundsReservedEvent;
import org.system.blockchain.service.BlockchainService;

@Component
@RequiredArgsConstructor
public class BlockchainListener {

    private final BlockchainService blockchainService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("blockchain-queue"),
            exchange = @Exchange(value = "saga-exchange", type = ExchangeTypes.TOPIC),
            key = "wallet.reserved"
    ))
    public void handleConfirmedVote(FundsReservedEvent event) {
        blockchainService.writeVoteToBlockchain(
                event.getVoteId(),
                event.getUserId(),
                event.getProjectId(),
                event.getCost()
        );
    }
}