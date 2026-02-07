package com.auctor.execution.domain

/**
 * Repository interface for Execution persistence.
 * Implementations provide the actual storage mechanism (e.g., Exposed, JPA, etc.)
 */
interface ExecutionRepository {
    /**
     * Save a new execution.
     * @return Saved execution with generated fields populated
     */
    suspend fun save(execution: Execution): Execution
    
    /**
     * Find execution by ID.
     * @return Execution if found, null otherwise
     */
    suspend fun findById(id: ExecutionId): Execution?
    
    /**
     * Find all executions with pagination.
     * @param limit Maximum number of results
     * @param offset Starting offset
     * @return List of executions
     */
    suspend fun findAll(limit: Int, offset: Int): List<Execution>
    
    /**
     * Update an existing execution.
     * @return Updated execution
     * @throws IllegalArgumentException if execution doesn't exist
     */
    suspend fun update(execution: Execution): Execution
}
