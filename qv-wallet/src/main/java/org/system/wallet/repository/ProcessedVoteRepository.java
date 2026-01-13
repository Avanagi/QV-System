package org.system.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.system.wallet.entity.ProcessedVote;

public interface ProcessedVoteRepository extends JpaRepository<ProcessedVote, Long> {
}