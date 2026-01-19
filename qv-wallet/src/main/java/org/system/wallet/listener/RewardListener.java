package org.system.wallet.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.system.common.event.VoteArchivedEvent;
import org.system.wallet.config.WalletRabbitConfig;
import org.system.wallet.service.WalletService;

@Component
@RequiredArgsConstructor
public class RewardListener {

    private final WalletService walletService;

    @RabbitListener(queues = WalletRabbitConfig.REWARD_QUEUE)
    public void handleVoteArchived(VoteArchivedEvent event) {
        walletService.processReward(event.getUserId(), event.getVoteId());
    }
}