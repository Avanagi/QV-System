package org.system.voting.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "polls")
@Data
@NoArgsConstructor
public class Poll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Column(unique = true)
    private String accessCode;

    private Long creatorId;

    @Enumerated(EnumType.STRING)
    private VoteType voteType;

    private boolean isPublic;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Option> options = new ArrayList<>();

    public Poll(String title, String description, Long creatorId, VoteType voteType, boolean isPublic) {
        this.title = title;
        this.description = description;
        this.creatorId = creatorId;
        this.voteType = voteType;
        this.isPublic = isPublic;
        this.accessCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}