package org.system.voting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.system.common.event.FundsReservedEvent;
import org.system.voting.config.RabbitConfig;
import org.system.voting.dto.VoteBatchRequest;
import org.system.voting.entity.Option;
import org.system.voting.entity.PollParticipation;
import org.system.voting.entity.Vote;
import org.system.voting.entity.VoteStatus;
import org.system.voting.repository.OptionRepository;
import org.system.voting.repository.PollParticipationRepository;
import org.system.voting.repository.VoteRepository;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

    private final VoteRepository voteRepository;
    private final OptionRepository optionRepository;
    private final PollParticipationRepository participationRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.max-budget:100.0}")
    private double maxBudget;

    @Transactional
    public void submitBatchVote(VoteBatchRequest request) {
        log.info("Batch Vote: User={}, Poll={}", request.getUserId(), request.getPollId());

        try {
            participationRepository.saveAndFlush(new PollParticipation(request.getUserId(), request.getPollId()));
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("DOUBLE_VOTE: Вы уже голосовали в этом опросе!");
        }

        double totalCost = 0;
        for (Integer count : request.getVotes().values()) {
            if (count < 0) throw new RuntimeException("Отрицательные голоса запрещены");
            totalCost += Math.pow(count, 2);
        }

        if (totalCost > maxBudget) {
            throw new RuntimeException("BUDGET_EXCEEDED: Превышен лимит 100 кредитов!");
        }

        for (Map.Entry<Long, Integer> entry : request.getVotes().entrySet()) {
            Long optionId = entry.getKey();
            Integer count = entry.getValue();

            if (count == 0) continue;

            Option option = optionRepository.findById(optionId)
                    .orElseThrow(() -> new RuntimeException("Option not found: " + optionId));

            if (!option.getPoll().getId().equals(request.getPollId())) {
                throw new RuntimeException("HACK_ATTEMPT: Опция не принадлежит этому опросу");
            }

            double cost = Math.pow(count, 2);

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
            log.info("🗑️ Голос {} перенесен в блокчейн и удален из SQL.", voteId);
        }
    }
}