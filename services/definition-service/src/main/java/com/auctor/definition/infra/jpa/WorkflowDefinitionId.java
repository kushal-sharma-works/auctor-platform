package com.auctor.definition.infra.jpa;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for WorkflowJpaEntity.
 */
public class WorkflowDefinitionId implements Serializable {
    private String id;
    private Integer version;
    
    public WorkflowDefinitionId() {
    }
    
    public WorkflowDefinitionId(String id, Integer version) {
        this.id = id;
        this.version = version;
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowDefinitionId that = (WorkflowDefinitionId) o;
        return Objects.equals(id, that.id) && Objects.equals(version, that.version);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }
}
