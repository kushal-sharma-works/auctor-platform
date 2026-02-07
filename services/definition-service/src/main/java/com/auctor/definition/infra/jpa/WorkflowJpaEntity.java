package com.auctor.definition.infra.jpa;

import com.auctor.definition.infra.jpa.converter.StringListConverter;
import com.auctor.definition.infra.jpa.converter.TransitionListConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * JPA entity for workflow_definitions table.
 */
@Entity
@Table(name = "workflow_definitions")
@IdClass(WorkflowDefinitionId.class)
public class WorkflowJpaEntity {
    
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
    
    @Convert(converter = StringListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> states;
    
    @Column(name = "initial_state", nullable = false, length = 100)
    private String initialState;
    
    @Convert(converter = TransitionListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<TransitionDto> transitions;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    @Column(name = "jpa_version", nullable = false)
    private Long jpaVersion;
    
    public WorkflowJpaEntity() {
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
    
    public List<String> getStates() {
        return states;
    }
    
    public void setStates(List<String> states) {
        this.states = states;
    }
    
    public String getInitialState() {
        return initialState;
    }
    
    public void setInitialState(String initialState) {
        this.initialState = initialState;
    }
    
    public List<TransitionDto> getTransitions() {
        return transitions;
    }
    
    public void setTransitions(List<TransitionDto> transitions) {
        this.transitions = transitions;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getJpaVersion() {
        return jpaVersion;
    }
    
    public void setJpaVersion(Long jpaVersion) {
        this.jpaVersion = jpaVersion;
    }
}
