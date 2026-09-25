package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.ProjectCreateRequest;
import org.example.model.Organization;
import org.example.model.Project;
import org.example.model.User;
import org.example.repository.OrganizationRepository;
import org.example.repository.ProjectRepository;
import org.example.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/create")
    public Organization createOrganization(@Valid @RequestBody Organization organization) {
        if (organizationRepository.existsBySlug(organization.getSlug())) {
            throw new RuntimeException("Organization slug already exists");
        }
        return organizationRepository.save(organization);
    }

    @GetMapping
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    @GetMapping("/{slug}")
    public Organization getOrganizationBySlug(@PathVariable String slug) {
        return organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
    }

    @PostMapping("/{organizationId}/projects")
    public Project createProject(@PathVariable Long organizationId, 
                               @Valid @RequestBody ProjectCreateRequest projectRequest,
                               @RequestHeader("Authorization") String token) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        Project project = new Project();
        project.setName(projectRequest.getName());
        project.setDescription(projectRequest.getDescription());
        project.setOrganization(organization);
        
        // For now, set created by as the first user in the organization
        List<User> users = userRepository.findByOrganizationId(organizationId);
        if (!users.isEmpty()) {
            project.setCreatedBy(users.get(0));
        }

        return projectRepository.save(project);
    }

    @GetMapping("/{organizationId}/projects")
    public List<Project> getProjects(@PathVariable Long organizationId) {
        return projectRepository.findByOrganizationId(organizationId);
    }
}
