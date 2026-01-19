package org.system.voting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.system.voting.dto.PollCreationRequest;
import org.system.voting.entity.Option;
import org.system.voting.entity.Poll;
import org.system.voting.entity.PollAccess;
import org.system.voting.repository.PollAccessRepository;
import org.system.voting.repository.PollRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final PollAccessRepository pollAccessRepository;

    @Value("${app.wallet-url:http://wallet-service:8082/api/wallet/charge}")
    private String walletUrl;
    private final RestClient restClient = RestClient.create();

    private static final BigDecimal POLL_COST = new BigDecimal("50.00");

    @Transactional
    public Poll createPoll(PollCreationRequest request) {
        try {
            var response = restClient.post()
                    .uri(walletUrl + "?userId=" + request.getCreatorId() + "&amount=" + POLL_COST)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка оплаты создания опроса: " + e.getMessage());
        }

        Poll poll = new Poll(request.getTitle(), request.getDescription(), request.getCreatorId());

        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            for (String optionText : request.getOptions()) {
                Option option = new Option(optionText, poll);
                poll.getOptions().add(option);
            }
        }

        return pollRepository.save(poll);
    }

    public Poll getByCode(String code) {
        return pollRepository.findByAccessCode(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Опрос с таким кодом не найден"));
    }

    @Transactional
    public Poll unlockPoll(Long userId, String code) {
        Poll poll = getByCode(code);
        if (!pollAccessRepository.existsByUserIdAndPoll(userId, poll)) {
            pollAccessRepository.save(new PollAccess(userId, poll));
        }
        return poll;
    }

    public List<Poll> getMyUnlockedPolls(Long userId) {
        return pollAccessRepository.findPollsByUserId(userId);
    }

    public List<Poll> getCreatedByMe(Long creatorId) {
        return pollRepository.findAllByCreatorId(creatorId);
    }
}