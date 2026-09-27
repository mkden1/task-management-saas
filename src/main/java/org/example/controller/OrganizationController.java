package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.ProjectCreateRequest;
import org.example.model.Organization;
import org.example.model.Project;
import org.example.repository.OrganizationRepository;
import org.example.repository.ProjectRepository;
import org.example.repository.UserRepository;
import org.example.security.AppUserDetails;
import org.example.security.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Projects of the caller's own organization. Organizations are created through
 * registration only; there is no endpoint to list or create them.
 */
@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public OrganizationController(OrganizationRepository organizationRepository,
                                  ProjectRepository projectRepository,
                                  UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{organizationId}/projects")
    public List<Project> getProjects(@PathVariable Long organizationId, @AuthenticationPrincipal AppUserDetails me) {
        requireOwnOrganization(organizationId, me);
        return projectRepository.findByOrganizationId(organizationId);
    }

    @PostMapping("/{organizationId}/projects")
    @PreAuthorize(Roles.CAN_WRITE)
    public Project createProject(@PathVariable Long organizationId,
                                 @Valid @RequestBody ProjectCreateRequest request,
                                 @AuthenticationPrincipal AppUserDetails me) {
        requireOwnOrganization(organizationId, me);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOrganization(organization);
        project.setCreatedBy(userRepository.findById(me.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")));

        return projectRepository.save(project);
    }

    // Another organization is reported as "not found" so its existence is not revealed.
    private static void requireOwnOrganization(Long organizationId, AppUserDetails me) {
        if (!organizationId.equals(me.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found");
        }
    }
}
