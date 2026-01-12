package org.system.wallet.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.system.common.event.VoteCreatedEvent;
import org.system.wallet.config.WalletRabbitConfig;
import org.system.wallet.service.WalletService;

@Component
@RequiredArgsConstructor
public class WalletEventListener {

    private final WalletService walletService;

    @RabbitListener(queues = WalletRabbitConfig.QUEUE_NAME)
    public void handleVoteCreated(VoteCreatedEvent event) {
        walletService.processPayment(event);
    }
}