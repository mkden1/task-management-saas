package org.example.controller;

import jakarta.validation.Valid;
import org.example.config.JwtUtil;
import org.example.dto.ProjectCreateRequest;
import org.example.model.Project;
import org.example.model.Task;
import org.example.repository.ProjectRepository;
import org.example.repository.TaskRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final JwtUtil jwtUtil;

    public ProjectController(ProjectRepository projectRepository, TaskRepository taskRepository, JwtUtil jwtUtil) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.jwtUtil = jwtUtil;
    }

    @PutMapping("/{id}")
    public Project updateProject(@PathVariable Long id, 
                               @Valid @RequestBody ProjectCreateRequest projectRequest,
                               @RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long organizationId = jwtUtil.extractOrganizationId(jwt);
        
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify project belongs to organization
        if (!project.getOrganization().getId().equals(organizationId)) {
            throw new RuntimeException("Unauthorized access to project");
        }

        project.setName(projectRequest.getName());
        project.setDescription(projectRequest.getDescription());

        return projectRepository.save(project);
    }

    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long organizationId = jwtUtil.extractOrganizationId(jwt);
        
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify project belongs to organization
        if (!project.getOrganization().getId().equals(organizationId)) {
            throw new RuntimeException("Unauthorized access to project");
        }

        // First, delete all tasks associated with this project
        List<Task> tasksToDelete = taskRepository.findByProjectId(id);
        if (!tasksToDelete.isEmpty()) {
            taskRepository.deleteAll(tasksToDelete);
        }

        // Then delete the project
        projectRepository.delete(project);
    }

    @GetMapping
    public List<Project> getAllProjects(@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long organizationId = jwtUtil.extractOrganizationId(jwt);
        
        return projectRepository.findByOrganizationId(organizationId);
    }
}
