package org.system.voting.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
@Data
@NoArgsConstructor
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne
    @JoinColumn(name = "option_id")
    private Option option;

    private Integer voteCount;
    private Double cost;

    @Enumerated(EnumType.STRING)
    private VoteStatus status;

    private LocalDateTime createdAt = LocalDateTime.now();
}