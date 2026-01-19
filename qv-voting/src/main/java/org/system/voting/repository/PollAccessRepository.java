package org.system.voting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.system.voting.entity.Poll;
import org.system.voting.entity.PollAccess;
import java.util.List;

public interface PollAccessRepository extends JpaRepository<PollAccess, Long> {
    boolean existsByUserIdAndPoll(Long userId, Poll poll);

    @Query("SELECT pa.poll FROM PollAccess pa WHERE pa.userId = :userId")
    List<Poll> findPollsByUserId(Long userId);
}