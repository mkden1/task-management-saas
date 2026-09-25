package org.example.controller;

import org.example.config.JwtUtil;
import org.example.dto.TaskCreateRequest;
import org.example.model.Task;
import org.example.model.Project;
import org.example.model.User;
import org.example.repository.TaskRepository;
import org.example.repository.ProjectRepository;
import org.example.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public TaskController(TaskRepository taskRepository, ProjectRepository projectRepository, 
                         UserRepository userRepository, JwtUtil jwtUtil) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public List<Task> getTasks(@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long organizationId = jwtUtil.extractOrganizationId(jwt);
        return taskRepository.findByOrganizationId(organizationId);
    }

    @GetMapping("/my-tasks")
    public List<Task> getMyTasks(@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long userId = jwtUtil.extractUserId(jwt);
        Long organizationId = jwtUtil.extractOrganizationId(jwt);
        return taskRepository.findByAssignedToAndOrganizationId(userId, organizationId);
    }

    @PostMapping
    public Task createTask(@RequestBody TaskCreateRequest taskRequest, @RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long organizationId = jwtUtil.extractOrganizationId(jwt);
        Long userId = jwtUtil.extractUserId(jwt);
        
        // Debug logging
        System.out.println("Creating task with projectId: " + taskRequest.getProjectId() + " for organization: " + organizationId);
        
        // First, let's just check if the project exists at all
        Project project = projectRepository.findById(taskRequest.getProjectId()).orElse(null);
        if (project == null) {
            System.out.println("Project not found with ID: " + taskRequest.getProjectId());
            throw new RuntimeException("Project not found");
        }
        
        // Check if project belongs to organization
        if (project.getOrganization() == null) {
            System.out.println("Project organization is null");
            throw new RuntimeException("Project organization is null");
        }
        
        if (!project.getOrganization().getId().equals(organizationId)) {
            System.out.println("Project organization ID: " + project.getOrganization().getId() + " does not match user organization ID: " + organizationId);
            throw new RuntimeException("Unauthorized access to project");
        }
        
        // Create task
        Task task = new Task();
        task.setTitle(taskRequest.getTitle());
        task.setDescription(taskRequest.getDescription());
        task.setStatus(Task.TaskStatus.valueOf(taskRequest.getStatus()));
        task.setPriority(Task.Priority.valueOf(taskRequest.getPriority()));
        task.setProject(project);
        
        // Set assigned user if provided
        if (taskRequest.getAssignedToId() != null) {
            User assignedUser = userRepository.findById(taskRequest.getAssignedToId()).orElse(null);
            task.setAssignedTo(assignedUser);
        }
        
        // Set created by
        User createdBy = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        task.setCreatedBy(createdBy);
        
        // Set due date if provided
        if (taskRequest.getDueDate() != null && !taskRequest.getDueDate().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                task.setDueDate(LocalDateTime.parse(taskRequest.getDueDate(), formatter));
            } catch (Exception e) {
                // If parsing fails, ignore due date
            }
        }
        
        return taskRepository.save(task);
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Long id, @RequestBody TaskCreateRequest taskDetails, 
                          @RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long organizationId = jwtUtil.extractOrganizationId(jwt);
        
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getProject().getOrganization().getId().equals(organizationId)) {
            throw new RuntimeException("Unauthorized access to task");
        }

        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setStatus(Task.TaskStatus.valueOf(taskDetails.getStatus()));
        task.setPriority(Task.Priority.valueOf(taskDetails.getPriority()));
        
        if (taskDetails.getAssignedToId() != null) {
            User assignedUser = userRepository.findById(taskDetails.getAssignedToId()).orElse(null);
            task.setAssignedTo(assignedUser);
        }
        
        if (taskDetails.getDueDate() != null && !taskDetails.getDueDate().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                task.setDueDate(LocalDateTime.parse(taskDetails.getDueDate(), formatter));
            } catch (Exception e) {
                // If parsing fails, ignore due date
            }
        }

        return taskRepository.save(task);
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long organizationId = jwtUtil.extractOrganizationId(jwt);
        
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getProject().getOrganization().getId().equals(organizationId)) {
            throw new RuntimeException("Unauthorized access to task");
        }

        taskRepository.delete(task);
    }
}
