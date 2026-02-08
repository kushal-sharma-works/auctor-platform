"use client"

import Link from "next/link"
import {
  Box,
  Container,
  Flex,
  Grid,
  GridItem,
  Heading,
  Text,
  VStack,
} from "@chakra-ui/react"
import { Layout } from "../../components/Layout"

export default function ExecutionsPage() {
  return (
    <Layout>
      <VStack align="stretch" spacing={8}>
        {/* Header */}
        <Flex direction={{ base: "column", sm: "row" }} align={{ base: "start", sm: "center" }} gap={6}>
          <VStack align="start" spacing={2}>
            <Heading as="h1" size="xl" color="slate.900">
              Executions
            </Heading>
            <Text fontSize="sm" color="slate.600">
              Track workflow execution history and status.
            </Text>
          </VStack>
        </Flex>

        {/* Grid */}
        <Grid templateColumns={{ base: "1fr", lg: "1fr 1fr" }} gap={6}>
          <Box
            borderRadius="xl"
            borderWidth={1}
            borderColor="slate.200"
            bg="white"
            p={6}
            boxShadow="sm"
          >
            <Heading as="h2" size="md" color="slate.900" mb={2}>
              Execution feed
            </Heading>
            <Text fontSize="sm" color="slate.600">
              Execution data will come from the execution-service.
            </Text>
          </Box>
          <Box
            borderRadius="xl"
            borderWidth={1}
            borderColor="slate.200"
            bg="white"
            p={6}
            boxShadow="sm"
          >
            <Heading as="h2" size="md" color="slate.900" mb={2}>
              Quick access
            </Heading>
            <Text fontSize="sm" color="slate.600" mb={4}>
              View an execution timeline once events are available.
            </Text>
            <Link href="/executions/example">
              <Text as="span" fontSize="sm" fontWeight="medium" color="blue.600" _hover={{ textDecoration: "underline" }}>
                Preview execution detail
              </Text>
            </Link>
          </Box>
        </Grid>
      </VStack>
    </Layout>
  )
}
