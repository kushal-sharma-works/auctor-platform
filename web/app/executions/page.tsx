"use client"

import Link from "next/link"
import { useState } from "react"
import {
  Box,
  Button,
  Flex,
  Heading,
  Text,
  VStack,
  HStack,
  Spinner,
  Center,
} from "@chakra-ui/react"
import {
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  TableContainer,
} from "@chakra-ui/react/table"
import { Layout } from "../../components/Layout"
import { Badge } from "../../components/UI"
import { useExecutions } from "../../hooks/useExecution"
import { useWorkflows } from "../../hooks/useGraphql"
import { StartExecutionModal } from "../../components/StartExecutionModal"
import { useSelector } from "react-redux"
import type { RootState } from "../../store"

export default function ExecutionsPage() {
  const [page, setPage] = useState(0)
  const [isStartModalOpen, setIsStartModalOpen] = useState(false)
  const size = 10
  const { data, isLoading, error } = useExecutions(page, size)
  const { data: workflowData } = useWorkflows(0, 200)
  const executions = data?.items
    const workflowNameById = new Map(
      (workflowData?.workflows?.content || []).map((workflow) => [workflow.id, workflow.name])
    )

    const formatWorkflowName = (workflowId: string) =>
      workflowNameById.get(workflowId) || workflowId
  const roles = useSelector((state: RootState) => state.session.roles)
  const canExecute = roles.includes("EXECUTOR") || roles.includes("ADMIN")

  const formatDate = (dateString: string) => {
    try {
      return new Date(dateString).toLocaleDateString()
    } catch {
      return dateString
    }
  }

  return (
    <Layout>
      <VStack align="stretch" spacing={8}>
        {/* Header */}
        <Flex direction={{ base: "column", sm: "row" }} align={{ base: "start", sm: "center" }} justify="space-between" gap={6}>
          <VStack align="start" spacing={2}>
            <Heading as="h1" size="2xl" color="purple.900" fontWeight="extrabold" letterSpacing="tight">
              Executions
            </Heading>
            <Text fontSize="lg" fontWeight="medium" color="purple.700">
              Monitor and manage workflow executions in real-time.
            </Text>
          </VStack>
          {canExecute && (
            <Button colorScheme="purple" px={6} py={3} fontSize="lg" fontWeight="semibold" onClick={() => setIsStartModalOpen(true)} boxShadow="lg">
              Start Execution
            </Button>
          )}
        </Flex>
        {/* Table */}
        <Box
          borderRadius="3xl"
          borderWidth={2}
          borderColor="purple.200"
          bg="white"
          overflow="hidden"
          boxShadow="lg"
        >
          <TableContainer>
            <Table variant="striped">
              <Thead bg="purple.50">
                <Tr>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="purple.700">
                    Execution ID
                  </Th>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="purple.700">
                    Workflow
                  </Th>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="purple.700">
                    Current State
                  </Th>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="purple.700">
                    Status
                  </Th>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="purple.700">
                    Started
                  </Th>
                  <Th px={8} py={4}></Th>
                </Tr>
              </Thead>
              <Tbody>
                {isLoading && (
                  <Tr>
                    <Td colSpan={6} px={8} py={8}>
                      <Center>
                        <Spinner color="purple.500" size="lg" />
                      </Center>
                    </Td>
                  </Tr>
                )}
                {error && (
                  <Tr>
                    <Td colSpan={6} px={8} py={8} textAlign="center" color="red.600" fontSize="lg">
                      Unable to load executions.
                    </Td>
                  </Tr>
                )}
                {!isLoading && !error && executions?.length === 0 && (
                  <Tr>
                    <Td colSpan={6} px={8} py={8} textAlign="center" color="purple.700" fontSize="lg">
                      No executions found. Start a new execution to get started.
                    </Td>
                  </Tr>
                )}
                {executions?.map((execution) => (
                  <Tr key={execution.id} _hover={{ bg: "purple.50", transition: "background-color 0.2s" }}>
                    <Td px={8} py={6} fontSize="lg" fontWeight="bold" color="purple.900">
                      <code>{execution.id.substring(0, 8)}...</code>
                    </Td>
                    <Td px={8} py={6} fontSize="lg" color="purple.700">
                      {formatWorkflowName(execution.workflowId)}
                    </Td>
                    <Td px={8} py={6} fontSize="lg" color="purple.700">
                      <code>{execution.currentState}</code>
                    </Td>
                    <Td px={8} py={6} fontSize="lg" color="purple.700">
                      <Badge status={execution.status.type} />
                    </Td>
                    <Td px={8} py={6} fontSize="lg" color="purple.700">
                      {formatDate(execution.createdAt)}
                    </Td>
                    <Td px={8} py={6} textAlign="right" fontSize="lg">
                      <Link href={`/executions/${execution.id}`}>
                        <Text as="span" fontWeight="semibold" color="purple.600" _hover={{ color: "purple.700", textDecoration: "underline" }}>
                          View
                        </Text>
                      </Link>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          </TableContainer>
        </Box>

        {/* Pagination */}
        <Flex justify="space-between" align="center" gap={6}>
          <Text fontSize="lg" fontWeight="medium" color="purple.700">
            Page {page + 1}
          </Text>
          <HStack gap={4}>
            <Button
              size="md"
              variant="outline"
              borderWidth={2}
              borderColor="purple.200"
              color="purple.700"
              bg="purple.50"
              fontSize="lg"
              fontWeight="semibold"
              onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
              isDisabled={page === 0}
              _hover={{ bg: "purple.100" }}
              boxShadow="sm"
            >
              Previous
            </Button>
            <Button
              size="md"
              variant="outline"
              borderWidth={2}
              borderColor="purple.200"
              color="purple.700"
              bg="purple.50"
              fontSize="lg"
              fontWeight="semibold"
              onClick={() => setPage((prev) => prev + 1)}
              isDisabled={!executions || executions.length < size}
              _hover={{ bg: "purple.100" }}
              boxShadow="sm"
            >
              Next
            </Button>
          </HStack>
        </Flex>
      </VStack>

      {/* Start Execution Modal */}
      {canExecute && (
        <StartExecutionModal isOpen={isStartModalOpen} onClose={() => setIsStartModalOpen(false)} />
      )}
    </Layout>
  )
}
