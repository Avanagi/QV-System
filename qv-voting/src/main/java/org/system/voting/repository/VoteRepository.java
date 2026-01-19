package org.system.voting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.system.voting.entity.Vote;
import org.system.voting.entity.VoteStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    @Query("SELECT COUNT(v) > 0 FROM Vote v WHERE v.userId = :userId AND v.option.poll.id = :pollId")
    boolean existsByUserIdAndOptionPollId(Long userId, Long pollId);

    List<Vote> findAllByStatusAndCreatedAtBefore(VoteStatus status, LocalDateTime timestamp);
}