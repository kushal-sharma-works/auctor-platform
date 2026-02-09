"use client"

import { useParams, useRouter } from "next/navigation"
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
  Grid,
  GridItem,
  useToast,
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
import { Layout } from "../../../components/Layout"
import { Badge } from "../../../components/UI"
import { useExecution, useExecutionAuditTrail, useAdvanceExecution } from "../../../hooks/useExecution"

export default function ExecutionDetailPage() {
  const params = useParams()
  const router = useRouter()
  const toast = useToast()
  const executionId = Array.isArray(params.id) ? params.id[0] : (params.id as string)

  const { data: execution, isLoading, error } = useExecution(executionId)
  const { data: auditTrail, isLoading: auditLoading } = useExecutionAuditTrail(executionId)
  const advanceExecutionMutation = useAdvanceExecution()

  const handleAdvance = async () => {
    try {
      await advanceExecutionMutation.mutateAsync({
        executionId,
      })

      toast({
        title: "Success",
        description: "Execution advanced to next state",
        status: "success",
        duration: 3000,
        isClosable: true,
      })
    } catch (error) {
      toast({
        title: "Error",
        description: error instanceof Error ? error.message : "Failed to advance execution",
        status: "error",
        duration: 3000,
        isClosable: true,
      })
    }
  }

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

  const isRunning = execution?.status.state?.toLowerCase() === "running"

  if (isLoading) {
    return (
      <Layout>
        <Center py={10}>
          <Spinner color="blue.500" size="lg" />
        </Center>
      </Layout>
    )
  }

  if (error || !execution) {
    return (
      <Layout>
        <VStack align="stretch" spacing={8}>
          <Flex direction="row" align="center" gap={4}>
            <Button variant="outline" onClick={() => router.back()}>
              Back
            </Button>
            <VStack align="start" spacing={2}>
              <Heading as="h1" size="lg" color="red.600">
                Execution Not Found
              </Heading>
              <Text color="slate.600" fontSize="sm">
                {error?.message || "Unable to load execution"}
              </Text>
            </VStack>
          </Flex>
        </VStack>
      </Layout>
    )
  }

  return (
    <Layout>
      <VStack align="stretch" spacing={8}>
        {/* Header */}
        <Flex direction="row" align="center" gap={4}>
          <Button variant="outline" onClick={() => router.back()}>
            Back
          </Button>
          <VStack align="start" spacing={2}>
            <Heading as="h1" size="lg" color="slate.900">
              Execution Details
            </Heading>
            <Text color="slate.600" fontSize="sm">
              ID: <code>{execution.id}</code>
            </Text>
          </VStack>
        </Flex>

        {/* Overview Grid */}
        <Grid templateColumns={{ base: "1fr", md: "1fr 1fr" }} gap={6}>
          {/* Execution Info */}
          <Box
            borderRadius="xl"
            borderWidth={1}
            borderColor="slate.200"
            bg="white"
            p={6}
            boxShadow="sm"
          >
            <VStack align="stretch" spacing={4}>
              <Heading as="h2" size="md" color="slate.900">
                Execution Info
              </Heading>

              <VStack align="stretch" spacing={2}>
                <Flex justify="space-between" align="center">
                  <Text fontSize="sm" fontWeight="medium" color="slate.600">
                    Workflow ID:
                  </Text>
                  <Text fontSize="sm" color="slate.900" fontWeight="medium">
                    {execution.workflowId}
                  </Text>
                </Flex>

                <Flex justify="space-between" align="center">
                  <Text fontSize="sm" fontWeight="medium" color="slate.600">
                    Workflow Version:
                  </Text>
                  <Text fontSize="sm" color="slate.900" fontWeight="medium">
                    v{execution.workflowVersion}
                  </Text>
                </Flex>

                <Flex justify="space-between" align="center">
                  <Text fontSize="sm" fontWeight="medium" color="slate.600">
                    Current State:
                  </Text>
                  <Text
                    fontSize="sm"
                    color="slate.900"
                    fontWeight="medium"
                    fontFamily="monospace"
                  >
                    {execution.currentState}
                  </Text>
                </Flex>

                <Flex justify="space-between" align="start">
                  <Text fontSize="sm" fontWeight="medium" color="slate.600">
                    Status:
                  </Text>
                  <Badge status={execution.status.state} />
                </Flex>

                {execution.status.reason && (
                  <Flex justify="space-between" align="start">
                    <Text fontSize="sm" fontWeight="medium" color="slate.600">
                      Reason:
                    </Text>
                    <Text fontSize="sm" color="slate.900">
                      {execution.status.reason}
                    </Text>
                  </Flex>
                )}

                <Flex justify="space-between" align="center">
                  <Text fontSize="sm" fontWeight="medium" color="slate.600">
                    Started:
                  </Text>
                  <VStack align="end" spacing={0}>
                    <Text fontSize="sm" color="slate.900" fontWeight="medium">
                      {formatDate(execution.createdAt)}
                    </Text>
                    <Text fontSize="xs" color="slate.500">
                      {formatTime(execution.createdAt)}
                    </Text>
                  </VStack>
                </Flex>

                <Flex justify="space-between" align="center">
                  <Text fontSize="sm" fontWeight="medium" color="slate.600">
                    Last Updated:
                  </Text>
                  <VStack align="end" spacing={0}>
                    <Text fontSize="sm" color="slate.900" fontWeight="medium">
                      {formatDate(execution.updatedAt)}
                    </Text>
                    <Text fontSize="xs" color="slate.500">
                      {formatTime(execution.updatedAt)}
                    </Text>
                  </VStack>
                </Flex>
              </VStack>

              {isRunning && (
                <Button
                  colorScheme="blue"
                  onClick={handleAdvance}
                  isLoading={advanceExecutionMutation.isPending}
                  width="full"
                >
                  Advance to Next State
                </Button>
              )}
            </VStack>
          </Box>

          {/* Input Parameters */}
          <Box
            borderRadius="xl"
            borderWidth={1}
            borderColor="slate.200"
            bg="white"
            p={6}
            boxShadow="sm"
          >
            <VStack align="stretch" spacing={4}>
              <Heading as="h2" size="md" color="slate.900">
                Input Parameters
              </Heading>

              {Object.keys(execution.input).length === 0 ? (
                <Text fontSize="sm" color="slate.500">
                  No input parameters
                </Text>
              ) : (
                <VStack align="stretch" spacing={2}>
                  {Object.entries(execution.input).map(([key, value]) => (
                    <Box
                      key={key}
                      p={3}
                      bg="slate.50"
                      borderRadius="md"
                      borderLeft="4px solid"
                      borderLeftColor="blue.200"
                    >
                      <Text fontSize="xs" fontWeight="bold" color="slate.700" mb={1}>
                        {key}
                      </Text>
                      <Text fontSize="xs" color="slate.600" fontFamily="monospace">
                        {value}
                      </Text>
                    </Box>
                  ))}
                </VStack>
              )}
            </VStack>
          </Box>
        </Grid>

        {/* Audit Trail */}
        <Box
          borderRadius="xl"
          borderWidth={1}
          borderColor="slate.200"
          bg="white"
          boxShadow="sm"
          overflow="hidden"
        >
          <Box p={6} borderBottom="1px solid" borderBottomColor="slate.200">
            <Heading as="h2" size="md" color="slate.900">
              Audit Trail
            </Heading>
          </Box>

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
                    Event Type
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
                    Actor
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
                    Timestamp
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
                    Details
                  </Th>
                </Tr>
              </Thead>
              <Tbody>
                {!auditLoading && (!auditTrail || auditTrail.length === 0) && (
                  <Tr>
                    <Td
                      colSpan={4}
                      px={6}
                      py={6}
                      textAlign="center"
                      color="slate.500"
                      fontSize="sm"
                    >
                      No audit events found
                    </Td>
                  </Tr>
                )}
                {auditLoading && (
                  <Tr>
                    <Td colSpan={4} px={6} py={6}>
                      <Center>
                        <Spinner color="blue.500" size="sm" />
                      </Center>
                    </Td>
                  </Tr>
                )}
                {auditTrail?.map((event) => (
                  <Tr key={event.id} _hover={{ bg: "slate.50" }}>
                    <Td px={6} py={4} fontSize="sm" fontWeight="medium" color="slate.900">
                      {event.eventType}
                    </Td>
                    <Td px={6} py={4} fontSize="sm" color="slate.600">
                      {event.actor}
                    </Td>
                    <Td px={6} py={4} fontSize="sm" color="slate.600">
                      {formatTime(event.timestamp)}
                    </Td>
                    <Td px={6} py={4} fontSize="xs" color="slate.600" fontFamily="monospace">
                      {event.details ? JSON.stringify(event.details).substring(0, 50) : 'N/A'}...
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          </TableContainer>
        </Box>
      </VStack>
    </Layout>
  )
}
