package org.example.repository;

import org.example.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    @Query("SELECT t FROM Task t WHERE t.project.organization.id = :organizationId")
    List<Task> findByOrganizationId(@Param("organizationId") Long organizationId);
    
    @Query("SELECT t FROM Task t WHERE t.assignedTo.id = :userId AND t.project.organization.id = :organizationId")
    List<Task> findByAssignedToAndOrganizationId(@Param("userId") Long userId, @Param("organizationId") Long organizationId);
    
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.project.organization.id = :organizationId")
    List<Task> findByProjectIdAndOrganizationId(@Param("projectId") Long projectId, @Param("organizationId") Long organizationId);
    
    @Query("SELECT t FROM Task t WHERE t.id = :id AND t.project.organization.id = :organizationId")
    Optional<Task> findByIdAndOrganizationId(@Param("id") Long id, @Param("organizationId") Long organizationId);

    List<Task> findByProjectId(Long projectId);
}
