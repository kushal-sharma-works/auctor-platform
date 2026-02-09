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
            <Heading as="h1" size="2xl" color="green.900" fontWeight="extrabold" letterSpacing="tight">
              Policies
            </Heading>
            <Text fontSize="lg" fontWeight="medium" color="green.700">
              Define the conditions that drive approvals and enforcement.
            </Text>
          </VStack>
          <Link href="/policies/new">
            <Button colorScheme="green" px={6} py={3} fontSize="lg" fontWeight="semibold" boxShadow="lg">
              Create Policy
            </Button>
          </Link>
        </Flex>

        {/* Table */}
        <Box
          borderRadius="3xl"
          borderWidth={2}
          borderColor="green.200"
          bg="white"
          overflow="hidden"
          boxShadow="lg"
        >
          <TableContainer>
            <Table variant="striped">
              <Thead bg="green.50">
                <Tr>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="green.700">
                    Name
                  </Th>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="green.700">
                    Version
                  </Th>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="green.700">
                    Status
                  </Th>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="green.700">
                    Conditions
                  </Th>
                  <Th px={8} py={4} textAlign="left" fontSize="sm" fontWeight="bold" textTransform="uppercase" letterSpacing="0.05em" color="green.700">
                    Created
                  </Th>
                  <Th px={8} py={4}></Th>
                </Tr>
              </Thead>
              <Tbody>
                {isLoading && (
                  <Tr>
                    <Td colSpan={6} px={8} py={8} textAlign="center" color="green.700" fontSize="lg">
                      Loading policies…
                    </Td>
                  </Tr>
                )}
                {error && (
                  <Tr>
                    <Td colSpan={6} px={8} py={8} textAlign="center" color="red.600" fontSize="lg">
                      Unable to load policies.
                    </Td>
                  </Tr>
                )}
                {!isLoading && !error && policies?.content?.length === 0 && (
                  <Tr>
                    <Td colSpan={6} px={8} py={8} textAlign="center" color="green.700" fontSize="lg">
                      No policies found. Create your first policy to get started.
                    </Td>
                  </Tr>
                )}
                {policies?.content?.map((policy) => (
                  <Tr key={policy.id} _hover={{ bg: "green.50", transition: "background-color 0.2s" }}>
                    <Td px={8} py={6} fontSize="lg" fontWeight="bold" color="green.900">
                      {policy.name}
                    </Td>
                    <Td px={8} py={6} fontSize="lg" color="green.700">
                      {policy.version}
                    </Td>
                    <Td px={8} py={6} fontSize="lg" color="green.700">
                      <Badge status={policy.status} />
                    </Td>
                    <Td px={8} py={6} fontSize="lg" color="green.700">
                      {policy.conditions.length} conditions
                    </Td>
                    <Td px={8} py={6} fontSize="lg" color="green.700">
                      {new Date(policy.createdAt).toLocaleDateString()}
                    </Td>
                    <Td px={8} py={6} textAlign="right" fontSize="lg">
                      <Link href={`/policies/${policy.id}`}>
                        <Text as="span" fontWeight="semibold" color="green.600" _hover={{ color: "green.700", textDecoration: "underline" }}>
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
          <Text fontSize="lg" fontWeight="medium" color="green.700">
            Page {(policies?.page ?? 0) + 1} of {policies?.totalPages ?? 1}
          </Text>
          <HStack gap={4}>
            <Button
              size="md"
              variant="outline"
              borderWidth={2}
              borderColor="green.200"
              color="green.700"
              bg="green.50"
              fontSize="lg"
              fontWeight="semibold"
              onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
              isDisabled={page === 0}
              _hover={{ bg: "green.100" }}
              boxShadow="sm"
            >
              Previous
            </Button>
            <Button
              size="md"
              variant="outline"
              borderWidth={2}
              borderColor="green.200"
              color="green.700"
              bg="green.50"
              fontSize="lg"
              fontWeight="semibold"
              onClick={() => setPage((prev) => Math.min(prev + 1, (policies?.totalPages ?? 1) - 1))}
              isDisabled={policies ? page >= policies.totalPages - 1 : true}
              _hover={{ bg: "green.100" }}
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
