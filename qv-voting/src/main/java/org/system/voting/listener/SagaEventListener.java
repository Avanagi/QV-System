package org.system.voting.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.system.common.event.FundsFailedEvent;
import org.system.common.event.FundsReservedEvent;
import org.system.voting.config.RabbitConfig;
import org.system.voting.service.VoteService;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventListener {

    private final VoteService voteService;

    @RabbitListener(queues = RabbitConfig.SUCCESS_QUEUE)
    public void handleFundsReserved(FundsReservedEvent event) {
        log.info("Получено подтверждение оплаты для voteId: {}", event.getVoteId());
        voteService.confirmVote(event.getVoteId());
    }

    @RabbitListener(queues = RabbitConfig.FAIL_QUEUE)
    public void handleFundsFailed(FundsFailedEvent event) {
        log.info("Получен отказ оплаты для voteId: {}", event.getVoteId());
        voteService.cancelVote(event.getVoteId(), event.getReason());
    }
}