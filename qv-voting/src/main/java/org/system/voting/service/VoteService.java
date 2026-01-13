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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

    private final VoteRepository voteRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.architecture:async}")
    private String architectureMode;

    @Value("${app.wallet-url}")
    private String walletUrl;

    private final RestClient restClient = RestClient.create();


    @Transactional
    public Vote createVote(VoteRequest request) {
        log.info("Принят голос от User: {}", request.getUserId());

        double costValue = Math.pow(request.getVoteCount(), 2);
        BigDecimal cost = BigDecimal.valueOf(costValue);

        Vote vote = new Vote();
        vote.setUserId(request.getUserId());
        vote.setProjectId(request.getProjectId());
        vote.setVoteCount(request.getVoteCount());
        vote.setCost(costValue);
        vote.setStatus(VoteStatus.PENDING);
        Vote savedVote = voteRepository.save(vote);

        log.info("Голос сохранен с ID: {}. Стоимость: {}. Ждем оплаты...", savedVote.getId(), costValue);

        if ("async".equalsIgnoreCase(architectureMode)) {
            VoteCreatedEvent event = new VoteCreatedEvent(savedVote.getId(), savedVote.getUserId(), cost);
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "vote.created", event);
            log.info("Async mode: Sent to RabbitMQ");

        } else {
            log.info("Sync mode: Calling Wallet via HTTP...");
            try {

                var response = restClient.post()
                        .uri(walletUrl + "?userId=" + request.getUserId() + "&amount=" + cost)
                        .retrieve()
                        .toBodilessEntity();

                if (response.getStatusCode().is2xxSuccessful()) {
                    savedVote.setStatus(VoteStatus.CONFIRMED);
                    voteRepository.save(savedVote);
                    log.info("Sync mode: Payment successful, vote CONFIRMED");
                }
            } catch (Exception e) {
                log.error("Sync mode: Error calling wallet: {}", e.getMessage());
                savedVote.setStatus(VoteStatus.REJECTED);
                voteRepository.save(savedVote);
            }
        }

        return savedVote;
    }

    @Transactional
    public void confirmVote(Long voteId) {
        Vote vote = voteRepository.findById(voteId).orElse(null);
        if (vote != null && vote.getStatus() == VoteStatus.PENDING) {
            vote.setStatus(VoteStatus.CONFIRMED);
            voteRepository.save(vote);
            log.info(">>> SAGA FINISHED: Голос {} подтвержден и оплачен! <<<", voteId);
        }
    }

    @Transactional
    public void cancelVote(Long voteId, String reason) {
        Vote vote = voteRepository.findById(voteId).orElse(null);
        if (vote != null) {
            vote.setStatus(VoteStatus.REJECTED);
            voteRepository.save(vote);
            log.warn(">>> SAGA FAILED: Голос {} отклонен. Причина: {} <<<", voteId, reason);
        }
    }
}