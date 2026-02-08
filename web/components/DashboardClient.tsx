"use client"

import Link from "next/link"
import { DefinitionCard } from "./DefinitionCard"
import { usePolicies, useWorkflows } from "../hooks/useGraphql"
import { Badge } from "./UI"
import { Box, Grid, Heading, Text, Stack, SimpleGrid } from "@chakra-ui/react"

export function DashboardClient() {
  const workflowsQuery = useWorkflows(0, 5)
  const policiesQuery = usePolicies(0, 5)

  if (workflowsQuery.isLoading || policiesQuery.isLoading) {
    return <Text fontSize="md" color="gray.500">Loading dashboard…</Text>
  }

  if (workflowsQuery.error || policiesQuery.error) {
    return (
      <Text fontSize="md" color="red.500">
        Unable to load dashboard data. Please try again.
      </Text>
    )
  }

  const workflows = workflowsQuery.data?.workflows
  const policies = policiesQuery.data?.policies

  return (
    <Grid templateColumns={{ base: "1fr", lg: "1fr" }} gap={10}>
      {/* Summary Cards */}
      <SimpleGrid columns={{ base: 1, md: 2 }} spacing={10} mb={8}>
        <DefinitionCard
          title="Total Workflows"
          value={`${workflows?.totalElements ?? 0}`}
          subtitle="Active workflow definitions in the catalog"
        />
        <DefinitionCard
          title="Total Policies"
          value={`${policies?.totalElements ?? 0}`}
          subtitle="Policy definitions governing approvals"
        />
      </SimpleGrid>

      {/* Recent Lists */}
      <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={14} mb={10} px={{ base: 0, lg: 2 }}>
        {/* Workflows List */}
        <Box 
          borderRadius="3xl" 
          borderWidth={2} 
          borderColor="blue.200" 
          bgGradient="linear(to-br, blue.50, white, blue.100)" 
          p={10} 
          boxShadow="lg"
        >
          <Stack direction="row" align="center" justify="space-between" mb={4}>
            <Heading fontSize="2xl" fontWeight="extrabold" color="blue.900" letterSpacing="tight">
              Recent Workflows
            </Heading>
            <Link href="/workflows">
              <Text fontSize="lg" fontWeight="semibold" color="blue.700" _hover={{ textDecoration: "underline" }}>
                View all
              </Text>
            </Link>
          </Stack>
          <Stack spacing={6} mt={6}>
            {workflows?.content?.length ? (
              workflows.content.map((workflow, idx) => (
                <Box
                  key={workflow.id}
                  borderRadius="2xl"
                  borderWidth={2}
                  px={6}
                  py={5}
                  boxShadow="md"
                  bg={idx % 2 === 0 ? "blue.50" : "white"}
                  borderColor={idx % 2 === 0 ? "blue.200" : "blue.50"}
                  display="flex"
                  alignItems="center"
                  justifyContent="space-between"
                  transition="background 0.2s"
                >
                  <Box>
                    <Text fontSize="lg" fontWeight="bold" color="blue.900" letterSpacing="tight">
                      {workflow.name}
                    </Text>
                    <Text fontSize="sm" color="blue.700" fontWeight="medium" mt={1}>
                      Version {workflow.version} · {workflow.states.length} states
                    </Text>
                  </Box>
                  <Box ml={2}>
                    <Badge status={workflow.status} />
                  </Box>
                </Box>
              ))
            ) : (
              <Text fontSize="lg" color="blue.700">No workflows yet.</Text>
            )}
          </Stack>
        </Box>

        {/* Policies List */}
        <Box 
          borderRadius="3xl" 
          borderWidth={2} 
          borderColor="green.200" 
          bgGradient="linear(to-br, green.50, white, green.100)" 
          p={10} 
          boxShadow="lg"
        >
          <Stack direction="row" align="center" justify="space-between" mb={4}>
            <Heading fontSize="2xl" fontWeight="extrabold" color="green.900" letterSpacing="tight">
              Recent Policies
            </Heading>
            <Link href="/policies">
              <Text fontSize="lg" fontWeight="semibold" color="green.700" _hover={{ textDecoration: "underline" }}>
                View all
              </Text>
            </Link>
          </Stack>
          <Stack spacing={6} mt={6}>
            {policies?.content?.length ? (
              policies.content.map((policy, idx) => (
                <Box
                  key={policy.id}
                  borderRadius="2xl"
                  borderWidth={2}
                  px={6}
                  py={5}
                  boxShadow="md"
                  bg={idx % 2 === 0 ? "green.50" : "white"}
                  borderColor={idx % 2 === 0 ? "green.200" : "green.50"}
                  display="flex"
                  alignItems="center"
                  justifyContent="space-between"
                  transition="background 0.2s"
                >
                  <Box>
                    <Text fontSize="lg" fontWeight="bold" color="green.900" letterSpacing="tight">
                      {policy.name}
                    </Text>
                    <Text fontSize="sm" color="green.700" fontWeight="medium" mt={1}>
                      Version {policy.version} · {policy.conditions.length} conditions
                    </Text>
                  </Box>
                  <Box ml={2}>
                    <Badge status={policy.status} />
                  </Box>
                </Box>
              ))
            ) : (
              <Text fontSize="lg" color="green.700">No policies yet.</Text>
            )}
          </Stack>
        </Box>
      </SimpleGrid>

      {/* Recent Activity */}
      <Box 
        borderRadius="3xl" 
        borderWidth={2} 
        borderColor="gray.300" 
        bgGradient="linear(to-r, gray.50, white, gray.100)" 
        p={10} 
        boxShadow="lg"
      >
        <Heading fontSize="2xl" fontWeight="extrabold" color="gray.900" letterSpacing="tight">
          Recent Activity
        </Heading>
        <Stack spacing={4} mt={6}>
          <Text fontSize="lg" color="gray.800">
            Track workflow approvals and policy updates here once executions are wired.
          </Text>
          <Text fontSize="lg" color="gray.700">
            Execution events will appear after execution-service integration is enabled.
          </Text>
        </Stack>
      </Box>
    </Grid>
  )
}
