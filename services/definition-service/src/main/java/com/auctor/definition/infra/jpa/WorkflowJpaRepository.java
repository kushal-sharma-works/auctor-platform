package com.auctor.definition.infra.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for workflows.
 */
@Repository
public interface WorkflowJpaRepository extends JpaRepository<WorkflowJpaEntity, WorkflowDefinitionId> {
    
    /**
     * Find a workflow by ID and version.
     */
    Optional<WorkflowJpaEntity> findByIdAndVersion(String id, Integer version);
    
    /**
     * Find the latest version of a workflow by ID.
     */
    @Query("SELECT w FROM WorkflowJpaEntity w WHERE w.id = :id ORDER BY w.version DESC LIMIT 1")
    Optional<WorkflowJpaEntity> findLatestByIdOrderByVersionDesc(@Param("id") String id);
    
    /**
     * Find all workflows (returns latest version per workflow ID).
     */
    @Query("SELECT w FROM WorkflowJpaEntity w WHERE (w.id, w.version) IN " +
           "(SELECT w2.id, MAX(w2.version) FROM WorkflowJpaEntity w2 GROUP BY w2.id)")
    Page<WorkflowJpaEntity> findAllLatestVersions(Pageable pageable);
    
    /**
     * Find the latest published version of a workflow.
     */
    @Query("SELECT w FROM WorkflowJpaEntity w WHERE w.id = :id AND w.status = 'PUBLISHED' ORDER BY w.version DESC LIMIT 1")
    Optional<WorkflowJpaEntity> findLatestPublishedById(@Param("id") String id);
}
