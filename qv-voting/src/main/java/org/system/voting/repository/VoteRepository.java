package org.system.voting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.system.voting.entity.Vote;
import org.system.voting.entity.VoteStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findAllByStatusAndCreatedAtBefore(VoteStatus status, LocalDateTime dateTime);

    boolean existsByUserIdAndProjectIdAndStatusIn(Long userId, Long projectId, List<VoteStatus> statuses);

}