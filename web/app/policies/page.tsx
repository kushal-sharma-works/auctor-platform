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
import { usePolicies } from "../../hooks/useGraphql"

export default function PoliciesPage() {
  const [page, setPage] = useState(0)
  const size = 10
  const { data, isLoading, error } = usePolicies(page, size)
  const policies = data?.policies

  return (
    <Layout>
      <VStack align="stretch" spacing={8}>
        {/* Header */}
        <Flex direction={{ base: "column", sm: "row" }} align={{ base: "start", sm: "center" }} justify="space-between" gap={6}>
          <VStack align="start" spacing={2}>
            <Heading as="h1" size="xl" color="slate.900">
              Policies
            </Heading>
            <Text color="slate.600" fontSize="sm">
              Define the conditions that drive approvals and enforcement.
            </Text>
          </VStack>
          <Link href="/policies/new">
            <Button colorScheme="blue" px={6} py={2}>
              Create Policy
            </Button>
          </Link>
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
                    Name
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
                    Version
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
                    Conditions
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
                    Created
                  </Th>
                  <Th px={6} py={3}></Th>
                </Tr>
              </Thead>
              <Tbody>
                {isLoading && (
                  <Tr>
                    <Td colSpan={6} px={6} py={6} textAlign="center" color="slate.500" fontSize="sm">
                      Loading policies…
                    </Td>
                  </Tr>
                )}
                {error && (
                  <Tr>
                    <Td colSpan={6} px={6} py={6} textAlign="center" color="red.600" fontSize="sm">
                      Unable to load policies.
                    </Td>
                  </Tr>
                )}
                {!isLoading && !error && policies?.content?.length === 0 && (
                  <Tr>
                    <Td colSpan={6} px={6} py={6} textAlign="center" color="slate.500" fontSize="sm">
                      No policies found. Create your first policy to get started.
                    </Td>
                  </Tr>
                )}
                {policies?.content?.map((policy) => (
                  <Tr key={policy.id} _hover={{ bg: "slate.50" }}>
                    <Td px={6} py={4} fontSize="sm" fontWeight="medium" color="slate.900">
                      {policy.name}
                    </Td>
                    <Td px={6} py={4} fontSize="sm" color="slate.600">
                      {policy.version}
                    </Td>
                    <Td px={6} py={4} fontSize="sm" color="slate.600">
                      <Badge status={policy.status} />
                    </Td>
                    <Td px={6} py={4} fontSize="sm" color="slate.600">
                      {policy.conditions.length} conditions
                    </Td>
                    <Td px={6} py={4} fontSize="sm" color="slate.600">
                      {new Date(policy.createdAt).toLocaleDateString()}
                    </Td>
                    <Td px={6} py={4} textAlign="right" fontSize="sm">
                      <Link href={`/policies/${policy.id}`}>
                        <Text as="span" fontWeight="medium" color="blue.600" _hover={{ color: "blue.700" }}>
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
            Page {(policies?.page ?? 0) + 1} of {policies?.totalPages ?? 1}
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
              onClick={() => setPage((prev) => Math.min(prev + 1, (policies?.totalPages ?? 1) - 1))}
              isDisabled={policies ? page >= policies.totalPages - 1 : true}
            >
              Next
            </Button>
          </HStack>
        </Flex>
      </VStack>
    </Layout>
  )
}
