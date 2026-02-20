"use client"

import { useParams } from "next/navigation"
import Link from "next/link"
import { useSelector } from "react-redux"
import {
  Box,
  Button,
  Flex,
  Grid,
  GridItem,
  Heading,
  Text,
  VStack,
  HStack,
  Stack,
  Badge as ChakraBadge,
  Wrap,
  WrapItem,
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
import { useWorkflow, usePublishWorkflow } from "../../../hooks/useGraphql"
import type { RootState } from "../../../store"

export default function WorkflowDetailPage() {
  const params = useParams()
  const workflowId = Array.isArray(params.id) ? params.id[0] : params.id
  const { data, isLoading, refetch } = useWorkflow(workflowId ?? "")
  const publishMutation = usePublishWorkflow()
  const roles = useSelector((state: RootState) => state.session.roles)
  const canAdmin = roles.includes("ADMIN")

  const workflow = data?.workflow

  const handlePublish = async () => {
    if (!workflowId) return
    try {
      await publishMutation.mutateAsync(workflowId)
      await refetch()
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Failed to publish workflow"
      console.error("Failed to publish workflow", message)
    }
  }

  return (
    <>
    <Layout>
      <Flex direction="row" align="flex-start" justify="space-between" mb={8}>
        <VStack align="flex-start" spacing={2}>
          <Text fontSize="xs" fontWeight="bold" textTransform="uppercase" letterSpacing="0.2em" color="blue.600">
            Workflow
          </Text>
          <Heading as="h1" size="xl" color="slate.900">
            {workflow?.name ?? "Workflow"}
          </Heading>
          <Text fontSize="sm" color="slate.600">
            Definition details, transitions, and linked policies.
          </Text>
        </VStack>
        <HStack gap={2}>
          <Link href="/workflows">
            <Button variant="outline" borderColor="slate.200" colorScheme="gray" size="sm">
              Back
            </Button>
          </Link>
          {workflow && canAdmin && (
            <Box
              title={workflow.status !== "DRAFT" ? `Workflow is ${workflow.status} - only DRAFT workflows can be published` : ""}
            >
              <Button
                onClick={handlePublish}
                isLoading={publishMutation.isPending}
                loadingText="Publishing..."
                colorScheme="blue"
                size="sm"
                isDisabled={workflow.status !== "DRAFT"}
              >
                Publish
              </Button>
            </Box>
          )}
        </HStack>
      </Flex>

      {isLoading && (
        <Text mt={8} fontSize="sm" color="slate.500">
          Loading workflow…
        </Text>
      )}

      {workflow && (
        <Grid templateColumns={{ base: "1fr", lg: "2fr 1fr" }} gap={6}>
          <GridItem>
            <Stack spacing={6}>
              {/* Details Section */}
              <Box borderRadius="xl" borderWidth={1} borderColor="slate.200" bg="white" p={6} boxShadow="sm">
                <Flex align="center" justify="space-between" mb={4}>
                  <Heading as="h2" size="md" color="slate.900">
                    Details
                  </Heading>
                  <Badge status={workflow.status} />
                </Flex>
                <Grid templateColumns={{ base: "1fr", sm: "1fr 1fr" }} gap={4} mt={4}>
                  <Box>
                    <Text fontSize="xs" fontWeight="bold" textTransform="uppercase" color="slate.500" mb={1}>
                      Version
                    </Text>
                    <Text fontSize="sm" fontWeight="medium" color="slate.900">
                      {workflow.version}
                    </Text>
                  </Box>
                  <Box>
                    <Text fontSize="xs" fontWeight="bold" textTransform="uppercase" color="slate.500" mb={1}>
                      Initial State
                    </Text>
                    <Text fontSize="sm" fontWeight="medium" color="slate.900">
                      {workflow.initialState}
                    </Text>
                  </Box>
                  <Box>
                    <Text fontSize="xs" fontWeight="bold" textTransform="uppercase" color="slate.500" mb={1}>
                      Created
                    </Text>
                    <Text fontSize="sm" fontWeight="medium" color="slate.900">
                      {new Date(workflow.createdAt).toLocaleString()}
                    </Text>
                  </Box>
                  <Box>
                    <Text fontSize="xs" fontWeight="bold" textTransform="uppercase" color="slate.500" mb={1}>
                      Updated
                    </Text>
                    <Text fontSize="sm" fontWeight="medium" color="slate.900">
                      {new Date(workflow.updatedAt).toLocaleString()}
                    </Text>
                  </Box>
                </Grid>
              </Box>

              {/* Transitions Section */}
              <Box borderRadius="xl" borderWidth={1} borderColor="slate.200" bg="white" p={6} boxShadow="sm">
                <Heading as="h2" size="md" color="slate.900" mb={4}>
                  Transitions
                </Heading>
                <TableContainer>
                  <Table variant="striped" size="sm">
                    <Thead bg="slate.50">
                      <Tr>
                        <Th px={4} py={2} fontSize="xs" fontWeight="bold" textTransform="uppercase" color="slate.500">
                          From
                        </Th>
                        <Th px={4} py={2} fontSize="xs" fontWeight="bold" textTransform="uppercase" color="slate.500">
                          To
                        </Th>
                        <Th px={4} py={2} fontSize="xs" fontWeight="bold" textTransform="uppercase" color="slate.500">
                          Policy
                        </Th>
                      </Tr>
                    </Thead>
                    <Tbody>
                      {workflow.transitions.map((transition, index) => (
                        <Tr key={`${transition.fromState}-${transition.toState}-${index}`}>
                          <Td px={4} py={2} fontSize="sm" color="slate.700">
                            {transition.fromState}
                          </Td>
                          <Td px={4} py={2} fontSize="sm" color="slate.700">
                            {transition.toState}
                          </Td>
                          <Td px={4} py={2} fontSize="sm" color="slate.700">
                            {transition.policyRef ? (
                              <Link href={`/policies/${transition.policyRef}`}>
                                <Text as="span" color="blue.600" _hover={{ color: "blue.700" }}>
                                  {transition.policyRef}
                                </Text>
                              </Link>
                            ) : (
                              <Text color="slate.400">None</Text>
                            )}
                          </Td>
                        </Tr>
                      ))}
                    </Tbody>
                  </Table>
                </TableContainer>
              </Box>
            </Stack>
          </GridItem>

          {/* Sidebar */}
          <GridItem>
            <Stack spacing={6}>
              {/* States Section */}
              <Box borderRadius="xl" borderWidth={1} borderColor="slate.200" bg="white" p={6} boxShadow="sm">
                <Heading as="h2" size="md" color="slate.900" mb={4}>
                  States
                </Heading>
                <Wrap spacing={2}>
                  {workflow.states.map((state) => (
                    <WrapItem key={state}>
                      <ChakraBadge
                        colorScheme={state === workflow.initialState ? "blue" : "gray"}
                        px={3}
                        py={1}
                      >
                        {state}
                      </ChakraBadge>
                    </WrapItem>
                  ))}
                </Wrap>
              </Box>

              {/* Linked Policies Section */}
              <Box borderRadius="xl" borderWidth={1} borderColor="slate.200" bg="white" p={6} boxShadow="sm">
                <Heading as="h2" size="md" color="slate.900" mb={4}>
                  Linked Policies
                </Heading>
                <Stack spacing={3} fontSize="sm" color="slate.600">
                  {workflow.transitions.some((transition) => transition.policyRef) ? (
                    workflow.transitions
                      .filter((transition) => transition.policyRef)
                      .map((transition) => (
                        <Link key={transition.policyRef ?? transition.toState} href={`/policies/${transition.policyRef}`}>
                          <Box
                            borderRadius="md"
                            borderWidth={1}
                            borderColor="slate.200"
                            px={3}
                            py={2}
                            _hover={{ color: "blue.700" }}
                          >
                            <Text color="blue.600" _hover={{ color: "blue.700" }}>
                              {transition.policyRef}
                            </Text>
                          </Box>
                        </Link>
                      ))
                  ) : (
                    <Text color="slate.500">No policies linked.</Text>
                  )}
                </Stack>
              </Box>
            </Stack>
          </GridItem>
        </Grid>
      )}
    </Layout>
    </>
  )
}
