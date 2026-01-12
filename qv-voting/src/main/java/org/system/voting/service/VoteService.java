package org.system.voting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.system.common.event.VoteCreatedEvent;
import org.system.voting.config.RabbitConfig;
import org.system.voting.dto.VoteRequest;
import org.system.voting.entity.Vote;
import org.system.voting.entity.VoteStatus;
import org.system.voting.repository.VoteRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

    private final VoteRepository voteRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Vote createVote(VoteRequest request) {
        log.info("Принят голос от User: {}", request.getUserId());

        double costValue = Math.pow(request.getVoteCount(), 2);

        Vote vote = new Vote();
        vote.setUserId(request.getUserId());
        vote.setProjectId(request.getProjectId());
        vote.setVoteCount(request.getVoteCount());
        vote.setCost(costValue);
        vote.setStatus(VoteStatus.PENDING); // Ждем оплаты

        Vote savedVote = voteRepository.save(vote);
        log.info("Голос сохранен с ID: {}. Стоимость: {}. Ждем оплаты...", savedVote.getId(), costValue);

        VoteCreatedEvent event = new VoteCreatedEvent(
                savedVote.getId(),
                savedVote.getUserId(),
                BigDecimal.valueOf(costValue)
        );

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "vote.created", event);
        log.info("Событие отправлено в RabbitMQ");

        return savedVote;
    }
}