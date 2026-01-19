package org.system.voting.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "poll_access")
@Data
@NoArgsConstructor
public class PollAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne
    @JoinColumn(name = "poll_id")
    private Poll poll;

    public PollAccess(Long userId, Poll poll) {
        this.userId = userId;
        this.poll = poll;
    }
}