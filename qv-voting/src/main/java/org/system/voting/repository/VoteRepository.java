package org.system.voting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.system.voting.entity.Vote;

public interface VoteRepository extends JpaRepository<Vote, Long> {
}