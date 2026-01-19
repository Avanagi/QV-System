package org.system.voting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.system.voting.entity.PollParticipation;

public interface PollParticipationRepository extends JpaRepository<PollParticipation, Long> {
    boolean existsByUserIdAndPollId(Long userId, Long pollId);
}