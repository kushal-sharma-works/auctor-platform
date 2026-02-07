package com.auctor.definition.infra.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for policies.
 */
@Repository
public interface PolicyJpaRepository extends JpaRepository<PolicyJpaEntity, PolicyDefinitionId> {
    
    /**
     * Find a policy by ID and version.
     */
    Optional<PolicyJpaEntity> findByIdAndVersion(String id, Integer version);
    
    /**
     * Find the latest version of a policy by ID.
     */
    Optional<PolicyJpaEntity> findFirstByIdOrderByVersionDesc(String id);
    
    /**
     * Find all policies (returns latest version per policy ID).
     */
    @Query("SELECT p FROM PolicyJpaEntity p WHERE (p.id, p.version) IN " +
           "(SELECT p2.id, MAX(p2.version) FROM PolicyJpaEntity p2 GROUP BY p2.id)")
    Page<PolicyJpaEntity> findAllLatestVersions(Pageable pageable);
}
