package org.system.voting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.system.voting.entity.Poll;
import java.util.List;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {
    Optional<Poll> findByAccessCode(String accessCode);
    List<Poll> findAllByCreatorId(Long creatorId);
}