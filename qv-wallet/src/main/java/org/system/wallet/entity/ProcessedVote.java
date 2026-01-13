package org.system.wallet.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_votes")
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedVote {
    @Id
    private Long voteId;
}