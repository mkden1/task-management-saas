package com.mkden.tasksaas.controller;

import jakarta.validation.Valid;
import com.mkden.tasksaas.dto.TaskCreateRequest;
import com.mkden.tasksaas.dto.TaskUpdateRequest;
import com.mkden.tasksaas.model.Project;
import com.mkden.tasksaas.model.Task;
import com.mkden.tasksaas.model.User;
import com.mkden.tasksaas.repository.ProjectRepository;
import com.mkden.tasksaas.repository.TaskRepository;
import com.mkden.tasksaas.repository.UserRepository;
import com.mkden.tasksaas.security.AppUserDetails;
import com.mkden.tasksaas.security.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public TaskController(TaskRepository taskRepository, ProjectRepository projectRepository,
                          UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Task> getTasks(@AuthenticationPrincipal AppUserDetails me) {
        return taskRepository.findByOrganizationId(me.getOrganizationId());
    }

    @GetMapping("/my-tasks")
    public List<Task> getMyTasks(@AuthenticationPrincipal AppUserDetails me) {
        return taskRepository.findByAssignedToAndOrganizationId(me.getId(), me.getOrganizationId());
    }

    @PostMapping
    @PreAuthorize(Roles.CAN_WRITE)
    public Task createTask(@Valid @RequestBody TaskCreateRequest request, @AuthenticationPrincipal AppUserDetails me) {
        Project project = projectRepository.findByIdAndOrganizationId(request.getProjectId(), me.getOrganizationId());
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(parseEnum(Task.TaskStatus.class, request.getStatus(), Task.TaskStatus.TODO, "status"));
        task.setPriority(parseEnum(Task.Priority.class, request.getPriority(), Task.Priority.MEDIUM, "priority"));
        task.setProject(project);
        if (request.getAssignedToId() != null) {
            task.setAssignedTo(resolveAssignee(request.getAssignedToId(), me));
        }
        task.setCreatedBy(userRepository.findById(me.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")));
        task.setDueDate(parseDueDate(request.getDueDate()));

        return taskRepository.save(task);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.CAN_WRITE)
    public Task updateTask(@PathVariable Long id, @RequestBody TaskUpdateRequest request,
                           @AuthenticationPrincipal AppUserDetails me) {
        Task task = findTaskInOrganization(id, me);

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title must not be blank");
            }
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(parseEnum(Task.TaskStatus.class, request.getStatus(), null, "status"));
        }
        if (request.getPriority() != null) {
            task.setPriority(parseEnum(Task.Priority.class, request.getPriority(), null, "priority"));
        }
        if (request.getAssignedToId() != null) {
            task.setAssignedTo(resolveAssignee(request.getAssignedToId(), me));
        }
        if (request.getDueDate() != null) {
            task.setDueDate(parseDueDate(request.getDueDate()));
        }

        return taskRepository.save(task);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.CAN_WRITE)
    public void deleteTask(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me) {
        taskRepository.delete(findTaskInOrganization(id, me));
    }

    // A task in another organization is reported as "not found" so its existence is not revealed.
    private Task findTaskInOrganization(Long id, AppUserDetails me) {
        return taskRepository.findByIdAndOrganizationId(id, me.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private User resolveAssignee(Long assigneeId, AppUserDetails me) {
        return userRepository.findByIdAndOrganizationId(assigneeId, me.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Assignee must belong to your organization"));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E defaultValue, String field) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + field);
        }
    }

    private static LocalDateTime parseDueDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // fall through and try a plain date
        }
        try {
            return LocalDate.parse(value).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid due date");
        }
    }
}
