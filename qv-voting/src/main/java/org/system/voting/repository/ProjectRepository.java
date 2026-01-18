package org.system.voting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.system.voting.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}