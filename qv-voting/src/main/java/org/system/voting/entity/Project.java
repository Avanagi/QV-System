package org.system.voting.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    private Long creatorId;

    public Project(String title, String description, Long creatorId) {
        this.title = title;
        this.description = description;
        this.creatorId = creatorId;
    }
}