package org.system.voting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.system.voting.entity.Vote;
import org.system.voting.entity.VoteStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    @Query(value = """
        SELECT count(*) > 0
        FROM votes v
        JOIN options o ON v.option_id = o.id
        WHERE v.user_id = :userId AND o.poll_id = :pollId
    """, nativeQuery = true)
    boolean existsByUserIdAndOptionPollId(Long userId, Long pollId);

    List<Vote> findAllByStatusAndCreatedAtBefore(VoteStatus status, LocalDateTime timestamp);
}