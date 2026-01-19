package org.system.voting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.system.voting.dto.PollCreationRequest;
import org.system.voting.entity.Option;
import org.system.voting.entity.Poll;
import org.system.voting.entity.PollAccess;
import org.system.voting.repository.PollAccessRepository;
import org.system.voting.repository.PollRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final PollAccessRepository pollAccessRepository;

    @Transactional
    public Poll createPoll(PollCreationRequest request) {
        Poll poll = new Poll(request.getTitle(), request.getDescription(), request.getCreatorId());

        if (request.getOptions() != null) {
            for (String text : request.getOptions()) {
                poll.getOptions().add(new Option(text, poll));
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