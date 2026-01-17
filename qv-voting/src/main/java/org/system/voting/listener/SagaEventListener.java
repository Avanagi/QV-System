package org.system.voting.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.system.common.event.FundsFailedEvent;
import org.system.common.event.FundsReservedEvent;
import org.system.common.event.VoteArchivedEvent;
import org.system.voting.config.RabbitConfig;
import org.system.voting.service.VoteService;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventListener {

    private final VoteService voteService;

    @RabbitListener(queues = RabbitConfig.SUCCESS_QUEUE)
    public void handleFundsReserved(FundsReservedEvent event) {
        log.info("💰 Оплата прошла успешно для voteId: {}. Ставим статус CONFIRMED.", event.getVoteId());
        voteService.confirmVote(event.getVoteId());
    }

    @RabbitListener(queues = RabbitConfig.FAIL_QUEUE)
    public void handleFundsFailed(FundsFailedEvent event) {
        log.warn("💸 Отказ оплаты для voteId: {}. Причина: {}", event.getVoteId(), event.getReason());
        voteService.cancelVote(event.getVoteId(), event.getReason());
    }

    @RabbitListener(queues = RabbitConfig.ARCHIVE_QUEUE)
    public void handleVoteArchived(VoteArchivedEvent event) {
        log.info("⛓️ Блокчейн подтвердил запись (Tx: {}). Удаляем voteId: {} из оперативной БД.",
                event.getTxHash(), event.getVoteId());

        voteService.deleteArchivedVote(event.getVoteId(), event.getTxHash());
    }
}