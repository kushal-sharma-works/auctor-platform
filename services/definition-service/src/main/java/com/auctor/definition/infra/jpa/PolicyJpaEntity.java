package com.auctor.definition.infra.jpa;

import com.auctor.definition.infra.jpa.converter.PolicyConditionListConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * JPA entity for policy_definitions table.
 */
@Entity
@Table(name = "policy_definitions")
@IdClass(PolicyDefinitionId.class)
public class PolicyJpaEntity {
    
    @Id
    @Column(length = 36, nullable = false)
    private String id;
    
    @Id
    @Column(nullable = false)
    private Integer version;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(nullable = false, length = 20)
    private String status;
    
    @Convert(converter = PolicyConditionListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<PolicyConditionDto> conditions;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Version
    @Column(name = "jpa_version", nullable = false)
    private Long jpaVersion;
    
    public PolicyJpaEntity() {
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<PolicyConditionDto> getConditions() {
        return conditions;
    }
    
    public void setConditions(List<PolicyConditionDto> conditions) {
        this.conditions = conditions;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getJpaVersion() {
        return jpaVersion;
    }
    
    public void setJpaVersion(Long jpaVersion) {
        this.jpaVersion = jpaVersion;
    }
}
