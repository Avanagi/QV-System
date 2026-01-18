package org.system.voting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.system.voting.entity.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByAccessCode(String accessCode);

    List<Project> findAllByCreatorId(Long creatorId);
}