package com.auctor.execution.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ExecutionStatusTest {

    @Test
    fun `should serialize Running status`() {
        val status = ExecutionStatus.Running
        assertEquals("RUNNING", status.toStorageString())
    }

    @Test
    fun `should serialize Completed status`() {
        val status = ExecutionStatus.Completed
        assertEquals("COMPLETED", status.toStorageString())
    }

    @Test
    fun `should serialize Failed status with reason`() {
        val status = ExecutionStatus.Failed("Policy denied")
        val encoded = java.util.Base64.getEncoder().encodeToString("Policy denied".toByteArray(Charsets.UTF_8))
        assertEquals("FAILED:$encoded", status.toStorageString())
    }

    @Test
    fun `should serialize Suspended status`() {
        val status = ExecutionStatus.Suspended
        assertEquals("SUSPENDED", status.toStorageString())
    }

    @Test
    fun `should deserialize Running status`() {
        val status = ExecutionStatus.fromStorageString("RUNNING")
        assertEquals(ExecutionStatus.Running, status)
    }

    @Test
    fun `should deserialize Completed status`() {
        val status = ExecutionStatus.fromStorageString("COMPLETED")
        assertEquals(ExecutionStatus.Completed, status)
    }

    @Test
    fun `should deserialize Failed status with reason`() {
        val encoded = java.util.Base64.getEncoder().encodeToString("Network error".toByteArray(Charsets.UTF_8))
        val status = ExecutionStatus.fromStorageString("FAILED:$encoded")
        assertEquals(ExecutionStatus.Failed("Network error"), status)
    }

    @Test
    fun `should deserialize Suspended status`() {
        val status = ExecutionStatus.fromStorageString("SUSPENDED")
        assertEquals(ExecutionStatus.Suspended, status)
    }

    @Test
    fun `should throw on unknown status`() {
        assertThrows<IllegalArgumentException> {
            ExecutionStatus.fromStorageString("UNKNOWN")
        }
    }

    @Test
    fun `should maintain immutability for sealed class hierarchy`() {
        val running1 = ExecutionStatus.Running
        val running2 = ExecutionStatus.Running
        assertEquals(running1, running2)

        val failed1 = ExecutionStatus.Failed("error1")
        val failed2 = ExecutionStatus.Failed("error2")
        assertNotEquals(failed1, failed2)
    }
}
