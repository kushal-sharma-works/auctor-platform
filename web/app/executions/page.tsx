"use client"

import Link from "next/link"
import { useState } from "react"
import {
  Box,
  Button,
  Container,
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
import { StartExecutionModal } from "../../components/StartExecutionModal"

export default function ExecutionsPage() {
  const [page, setPage] = useState(0)
  const [isStartModalOpen, setIsStartModalOpen] = useState(false)
  const size = 10
  const { data, isLoading, error } = useExecutions(page, size)
  const executions = data?.items

  const formatDate = (dateString: string) => {
    try {
      return new Date(dateString).toLocaleDateString()
    } catch {
      return dateString
    }
  }

  const formatTime = (dateString: string) => {
    try {
      return new Date(dateString).toLocaleTimeString()
    } catch {
      return dateString
    }
  }

  return (
    <Layout>
      <VStack align="stretch" spacing={8}>
        {/* Header */}
        <Flex
          direction={{ base: "column", sm: "row" }}
          align={{ base: "start", sm: "center" }}
          justify="space-between"
          gap={6}
        >
          <VStack align="start" spacing={2}>
            <Heading as="h1" size="xl" color="slate.900">
              Executions
            </Heading>
            <Text color="slate.600" fontSize="sm">
              Monitor and manage workflow executions in real-time.
            </Text>
          </VStack>
          <Button
            colorScheme="blue"
            px={6}
            py={2}
            onClick={() => setIsStartModalOpen(true)}
          >
            Start Execution
          </Button>
        </Flex>

        {/* Table */}
        <Box
          borderRadius="xl"
          borderWidth={1}
          borderColor="slate.200"
          bg="white"
          boxShadow="sm"
          overflow="hidden"
        >
          <TableContainer>
            <Table variant="striped">
              <Thead bg="slate.50">
                <Tr>
                  <Th
                    px={6}
                    py={3}
                    textAlign="left"
                    fontSize="xs"
                    fontWeight="bold"
                    textTransform="uppercase"
                    letterSpacing="0.05em"
                    color="slate.500"
                  >
                    Execution ID
                  </Th>
                  <Th
                    px={6}
                    py={3}
                    textAlign="left"
                    fontSize="xs"
                    fontWeight="bold"
                    textTransform="uppercase"
                    letterSpacing="0.05em"
                    color="slate.500"
                  >
                    Workflow
                  </Th>
                  <Th
                    px={6}
                    py={3}
                    textAlign="left"
                    fontSize="xs"
                    fontWeight="bold"
                    textTransform="uppercase"
                    letterSpacing="0.05em"
                    color="slate.500"
                  >
                    Current State
                  </Th>
                  <Th
                    px={6}
                    py={3}
                    textAlign="left"
                    fontSize="xs"
                    fontWeight="bold"
                    textTransform="uppercase"
                    letterSpacing="0.05em"
                    color="slate.500"
                  >
                    Status
                  </Th>
                  <Th
                    px={6}
                    py={3}
                    textAlign="left"
                    fontSize="xs"
                    fontWeight="bold"
                    textTransform="uppercase"
                    letterSpacing="0.05em"
                    color="slate.500"
                  >
                    Started
                  </Th>
                  <Th px={6} py={3}></Th>
                </Tr>
              </Thead>
              <Tbody>
                {isLoading && (
                  <Tr>
                    <Td colSpan={6} px={6} py={6}>
                      <Center>
                        <Spinner color="blue.500" />
                      </Center>
                    </Td>
                  </Tr>
                )}
                {error && (
                  <Tr>
                    <Td
                      colSpan={6}
                      px={6}
                      py={6}
                      textAlign="center"
                      color="red.600"
                      fontSize="sm"
                    >
                      Unable to load executions.
                    </Td>
                  </Tr>
                )}
                {!isLoading && !error && executions?.length === 0 && (
                  <Tr>
                    <Td
                      colSpan={6}
                      px={6}
                      py={6}
                      textAlign="center"
                      color="slate.500"
                      fontSize="sm"
                    >
                      No executions found. Start a new execution to get started.
                    </Td>
                  </Tr>
                )}
                {executions?.map((execution) => (
                  <Tr key={execution.id} _hover={{ bg: "slate.50" }}>
                    <Td px={6} py={4} fontSize="sm" fontWeight="medium" color="slate.900">
                      <code>{execution.id.substring(0, 8)}...</code>
                    </Td>
                    <Td px={6} py={4} fontSize="sm" color="slate.600">
                      {execution.workflowId}
                    </Td>
                    <Td px={6} py={4} fontSize="sm" color="slate.600">
                      <code>{execution.currentState}</code>
                    </Td>
                    <Td px={6} py={4} fontSize="sm" color="slate.600">
                      <Badge status={execution.status.state} />
                    </Td>
                    <Td px={6} py={4} fontSize="sm" color="slate.600">
                      {formatDate(execution.createdAt)}
                    </Td>
                    <Td px={6} py={4} textAlign="right" fontSize="sm">
                      <Link href={`/executions/${execution.id}`}>
                        <Text
                          as="span"
                          fontWeight="medium"
                          color="blue.600"
                          _hover={{ color: "blue.700" }}
                        >
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
          <Text fontSize="sm" color="slate.500">
            Page {page + 1}
          </Text>
          <HStack gap={2}>
            <Button
              size="sm"
              variant="outline"
              borderColor="slate.200"
              color="slate.600"
              onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
              isDisabled={page === 0}
            >
              Previous
            </Button>
            <Button
              size="sm"
              variant="outline"
              borderColor="slate.200"
              color="slate.600"
              onClick={() => setPage((prev) => prev + 1)}
              isDisabled={!executions || executions.length < size}
            >
              Next
            </Button>
          </HStack>
        </Flex>
      </VStack>

      {/* Start Execution Modal */}
      <StartExecutionModal
        isOpen={isStartModalOpen}
        onClose={() => setIsStartModalOpen(false)}
      />
    </Layout>
  )
}
