package org.system.voting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.system.common.event.VoteCreatedEvent;
import org.system.voting.config.RabbitConfig;
import org.system.voting.entity.Vote;
import org.system.voting.entity.VoteStatus;
import org.system.voting.repository.VoteRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteRepairJob {

    private final VoteRepository voteRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void reprocessStuckVotes() {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        List<Vote> stuckVotes = voteRepository.findAllByStatusAndCreatedAtBefore(VoteStatus.PENDING, oneMinuteAgo);

        if (stuckVotes.isEmpty()) {
            return;
        }

        log.info("🧹 НАЙДЕНО ЗАВИСШИХ ГОЛОСОВ: {}", stuckVotes.size());

        for (Vote vote : stuckVotes) {
            log.info("Retrying vote ID: {}", vote.getId());

            VoteCreatedEvent event = new VoteCreatedEvent(
                    vote.getId(),
                    vote.getUserId(),
                    vote.getProjectId(),
                    BigDecimal.valueOf(vote.getCost())
            );

            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "vote.created", event);
        }
    }
}