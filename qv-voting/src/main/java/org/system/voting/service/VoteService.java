package org.system.voting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.system.common.event.FundsReservedEvent;
import org.system.voting.config.RabbitConfig;
import org.system.voting.dto.VoteBatchRequest;
import org.system.voting.entity.*;
import org.system.voting.repository.OptionRepository;
import org.system.voting.repository.PollParticipationRepository;
import org.system.voting.repository.PollRepository;
import org.system.voting.repository.VoteRepository;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final OptionRepository optionRepository;
    private final PollRepository pollRepository;
    private final PollParticipationRepository participationRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.wallet-url:http://wallet-service:8082/api/wallet/charge}")
    private String walletUrl;

    private final RestClient restClient = RestClient.create();

    @Transactional
    public void submitBatchVote(VoteBatchRequest request) {
        Poll poll = pollRepository.findById(request.getPollId()).orElseThrow();

        try {
            participationRepository.saveAndFlush(new PollParticipation(request.getUserId(), request.getPollId()));
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("DOUBLE_VOTE");
        }

        double totalCost = 0;

        for (Map.Entry<Long, Integer> entry : request.getVotes().entrySet()) {
            Integer count = entry.getValue();
            if (count < 0) throw new RuntimeException("Negative vote");
            if (count == 0) continue;

            if (poll.getVoteType() == VoteType.LINEAR) {
                if (count > 1) throw new RuntimeException("Linear vote limit exceeded");
                totalCost += count;
            } else {
                totalCost += Math.pow(count, 2);
            }
        }

        try {
            restClient.post()
                    .uri(walletUrl + "?userId=" + request.getUserId() + "&amount=" + totalCost)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException("Insufficient global balance");
        }

        for (Map.Entry<Long, Integer> entry : request.getVotes().entrySet()) {
            Long optionId = entry.getKey();
            Integer count = entry.getValue();
            if (count == 0) continue;

            Option option = optionRepository.findById(optionId).orElseThrow();
            double cost = poll.getVoteType() == VoteType.LINEAR ? count : Math.pow(count, 2);

            Vote vote = new Vote();
            vote.setUserId(request.getUserId());
            vote.setOption(option);
            vote.setVoteCount(count);
            vote.setCost(cost);
            vote.setStatus(VoteStatus.CONFIRMED);

            Vote savedVote = voteRepository.save(vote);
            sendToBlockchain(savedVote, option);
        }
    }

    public boolean hasVoted(Long userId, Long pollId) {
        return participationRepository.existsByUserIdAndPollId(userId, pollId);
    }

    private void sendToBlockchain(Vote vote, Option option) {
        FundsReservedEvent event = new FundsReservedEvent(
                vote.getId(),
                vote.getUserId(),
                option.getId(),
                BigDecimal.valueOf(vote.getCost()),
                vote.getVoteCount(),
                option.getPoll().getTitle(),
                option.getText(),
                option.getPoll().getId()
        );
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "wallet.reserved", event);
    }

    @Transactional
    public void deleteArchivedVote(Long voteId, String txHash) {
        if (voteRepository.existsById(voteId)) {
            voteRepository.deleteById(voteId);
        }
    }
}