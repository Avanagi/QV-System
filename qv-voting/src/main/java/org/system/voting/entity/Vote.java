package org.system.voting.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Integer voteCount;

    @Column(nullable = false)
    private Double cost;

    @Enumerated(EnumType.STRING)
    private VoteStatus status;

    private LocalDateTime createdAt = LocalDateTime.now();
}