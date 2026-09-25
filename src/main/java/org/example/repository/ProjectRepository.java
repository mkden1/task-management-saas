package org.example.repository;

import org.example.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    @Query("SELECT p FROM Project p WHERE p.organization.id = :organizationId")
    List<Project> findByOrganizationId(@Param("organizationId") Long organizationId);
    
    @Query("SELECT p FROM Project p WHERE p.id = :projectId AND p.organization.id = :organizationId")
    Project findByIdAndOrganizationId(@Param("projectId") Long projectId, @Param("organizationId") Long organizationId);
}
