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
import { useWorkflows } from "../../hooks/useGraphql"

export default function WorkflowsPage() {
  const [page, setPage] = useState(0)
  const size = 10
  const { data, isLoading, error } = useWorkflows(page, size)

  const workflows = data?.workflows

  return (
    <Layout>
      <VStack align="stretch" spacing={8}>
        {/* Header */}
        <Flex direction={{ base: "column", sm: "row" }} align={{ base: "start", sm: "center" }} justify="space-between" gap={6}>
          <VStack align="start" spacing={2}>
            <Heading as="h1" size="2xl" color="blue.900" fontWeight="extrabold" letterSpacing="tight">
              Workflows
            </Heading>
            <Text fontSize="lg" fontWeight="medium" color="blue.700">
              Manage workflow definitions, versions, and lifecycle status.
            </Text>
          </VStack>
          <Link href="/workflows/new">
            <Button colorScheme="blue" px={6} py={3} fontSize="lg" fontWeight="semibold" boxShadow="lg">
              Create Workflow
            </Button>
          </Link>
        </Flex>

        {/* Table */}
        <Box
          borderRadius="3xl"
          borderWidth={2}
          borderColor="blue.200"
          bg="white"
          overflow="hidden"
          boxShadow="lg"
        >
          <TableContainer>
            <Table variant="striped">
              <Thead bg="blue.50">
                <Tr>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="blue.700">
                    Name
                  </Th>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="blue.700">
                    Version
                  </Th>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="blue.700">
                    Status
                  </Th>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="blue.700">
                    States
                  </Th>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="blue.700">
                    Created
                  </Th>
                  <Th px={8} py={4}></Th>
                </Tr>
              </Thead>
              <Tbody>
                {isLoading && (
                  <Tr>
                    <Td colSpan={6} px={8} py={8} textAlign="center" color="blue.700" fontSize="lg">
                      Loading workflows…
                    </Td>
                  </Tr>
                )}
                {error && (
                  <Tr>
                    <Td colSpan={6} px={8} py={8} textAlign="center" color="red.600" fontSize="lg">
                      Unable to load workflows.
                    </Td>
                  </Tr>
                )}
                {!isLoading && !error && workflows?.content?.length === 0 && (
                  <Tr>
                    <Td colSpan={6} px={8} py={8} textAlign="center" color="blue.700" fontSize="lg">
                      No workflows found. Create your first workflow to get started.
                    </Td>
                  </Tr>
                )}
                {workflows?.content?.map((workflow) => (
                  <Tr key={workflow.id} _hover={{ bg: "blue.50", transition: "background-color 0.2s" }}>
                    <Td px={8} py={6} fontSize="lg" fontWeight="bold" color="blue.900">
                      {workflow.name}
                    </Td>
                    <Td px={8} py={6} fontSize="lg" color="blue.700">
                      {workflow.version}
                    </Td>
                    <Td px={8} py={6} fontSize="lg" color="blue.700">
                      <Badge status={workflow.status} />
                    </Td>
                    <Td px={8} py={6} fontSize="lg" color="blue.700">
                      {workflow.states.join(", ")}
                    </Td>
                    <Td px={8} py={6} fontSize="lg" color="blue.700">
                      {new Date(workflow.createdAt).toLocaleDateString()}
                    </Td>
                    <Td px={8} py={6} textAlign="right" fontSize="lg">
                      <Link href={`/workflows/${workflow.id}`}>
                        <Text as="span" fontWeight="semibold" color="blue.600" _hover={{ color: "blue.700", textDecoration: "underline" }}>
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
          <Text fontSize="lg" fontWeight="medium" color="blue.700">
            Page {(workflows?.page ?? 0) + 1} of {workflows?.totalPages ?? 1}
          </Text>
          <HStack gap={4}>
            <Button
              size="md"
              variant="outline"
              borderWidth={2}
              borderColor="blue.200"
              color="blue.700"
              bg="blue.50"
              fontSize="lg"
              fontWeight="semibold"
              onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
              isDisabled={page === 0}
              _hover={{ bg: "blue.100" }}
              boxShadow="sm"
            >
              Previous
            </Button>
            <Button
              size="md"
              variant="outline"
              borderWidth={2}
              borderColor="blue.200"
              color="blue.700"
              bg="blue.50"
              fontSize="lg"
              fontWeight="semibold"
              onClick={() => setPage((prev) => Math.min(prev + 1, (workflows?.totalPages ?? 1) - 1))}
              isDisabled={workflows ? page >= workflows.totalPages - 1 : true}
              _hover={{ bg: "blue.100" }}
              boxShadow="sm"
            >
              Next
            </Button>
          </HStack>
        </Flex>
      </VStack>
    </Layout>
  )
}
