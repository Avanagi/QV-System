package org.system.voting.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.system.common.event.VoteArchivedEvent;
import org.system.voting.config.RabbitConfig;
import org.system.voting.service.VoteService;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventListener {

    private final VoteService voteService;

    // Слушаем только успешную запись в блокчейн для очистки базы
    @RabbitListener(queues = RabbitConfig.ARCHIVE_QUEUE)
    public void handleVoteArchived(VoteArchivedEvent event) {
        voteService.deleteArchivedVote(event.getVoteId(), event.getTxHash());
    }
}