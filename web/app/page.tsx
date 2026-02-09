import { Suspense } from "react"
import { Box, Container, VStack, Heading, Text } from "@chakra-ui/react"
import { Layout } from "../components/Layout"
import { DashboardClient } from "../components/DashboardClient"

export default function DashboardPage() {
  return (
    <Layout>
      <VStack align="stretch" spacing={8}>
        {/* Page header */}
        <Box
          bg="white"
          borderRadius="lg"
          p={8}
          boxShadow="md"
          borderLeft="4px solid"
          borderColor="blue.500"
        >
          <Text
            fontSize="sm"
            fontWeight="bold"
            color="blue.700"
            textTransform="uppercase"
            letterSpacing="0.1em"
            mb={2}
          >
            Overview
          </Text>
          <Heading as="h1" size="2xl" color="blue.900" mb={4}>
            Workflow & Policy Command Center
          </Heading>
          <Text fontSize="md" color="blue.800" maxW="2xl">
            Monitor the latest definitions and keep tabs on execution readiness across teams. Beautiful, fast, and actionable.
          </Text>
        </Box>

        {/* Dashboard summary and lists */}
        <Suspense fallback={<Text fontSize="md" color="blue.700">Loading insights…</Text>}>
          <DashboardClient />
        </Suspense>
      </VStack>
    </Layout>
  )
}
