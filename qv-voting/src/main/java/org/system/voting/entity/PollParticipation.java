package org.system.voting.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "poll_participations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"userId", "pollId"})
})
@Data
@NoArgsConstructor
public class PollParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long pollId;

    public PollParticipation(Long userId, Long pollId) {
        this.userId = userId;
        this.pollId = pollId;
    }
}