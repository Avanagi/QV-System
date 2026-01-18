package org.system.voting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.system.voting.entity.Project;
import org.system.voting.repository.ProjectRepository;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;

    @PostMapping
    public Project createProject(@RequestBody Project project) {
        if (project.getAccessCode() == null) {
            project = new Project(project.getTitle(), project.getDescription(), project.getCreatorId());
        }
        return projectRepository.save(project);
    }

    @GetMapping("/search/{code}")
    public ResponseEntity<Project> getProjectByCode(@PathVariable String code) {
        return projectRepository.findByAccessCode(code.toUpperCase())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my/{creatorId}")
    public List<Project> getMyProjects(@PathVariable Long creatorId) {
        return projectRepository.findAllByCreatorId(creatorId);
    }
}