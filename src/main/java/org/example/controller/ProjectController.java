package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.ProjectCreateRequest;
import org.example.model.Project;
import org.example.repository.ProjectRepository;
import org.example.repository.TaskRepository;
import org.example.security.AppUserDetails;
import org.example.security.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ProjectController(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @GetMapping
    public List<Project> getAllProjects(@AuthenticationPrincipal AppUserDetails me) {
        return projectRepository.findByOrganizationId(me.getOrganizationId());
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.CAN_WRITE)
    public Project updateProject(@PathVariable Long id, @Valid @RequestBody ProjectCreateRequest request,
                                 @AuthenticationPrincipal AppUserDetails me) {
        Project project = findProjectInOrganization(id, me);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        return projectRepository.save(project);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY)
    @Transactional
    public void deleteProject(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me) {
        Project project = findProjectInOrganization(id, me);
        taskRepository.deleteAll(taskRepository.findByProjectId(id));
        projectRepository.delete(project);
    }

    // A project in another organization is reported as "not found" so its existence is not revealed.
    private Project findProjectInOrganization(Long id, AppUserDetails me) {
        Project project = projectRepository.findByIdAndOrganizationId(id, me.getOrganizationId());
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        return project;
    }
}
