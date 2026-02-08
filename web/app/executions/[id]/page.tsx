"use client"

import Link from "next/link"
import { useParams } from "next/navigation"
import {
  Box,
  Button,
  Flex,
  Heading,
  Text,
  VStack,
  HStack,
} from "@chakra-ui/react"
import { Layout } from "../../../components/Layout"

const timeline = [
  {
    title: "Execution created",
    time: "Just now",
    description: "Execution request received by the engine.",
  },
  {
    title: "Awaiting approvals",
    time: "Pending",
    description: "Policies will be evaluated once execution-service is connected.",
  },
]

export default function ExecutionDetailPage() {
  const params = useParams()
  const executionId = Array.isArray(params.id) ? params.id[0] : params.id

  return (
    <Layout>
      <Flex direction="row" align="flex-start" justify="space-between" mb={8}>
        <VStack align="flex-start" spacing={2}>
          <Text fontSize="xs" fontWeight="bold" textTransform="uppercase" letterSpacing="0.2em" color="blue.600">
            Execution
          </Text>
          <Heading as="h1" size="xl" color="slate.900">
            Execution {executionId}
          </Heading>
          <Text fontSize="sm" color="slate.600">
            Timeline view for execution state changes (placeholder).
          </Text>
        </VStack>
        <Link href="/executions">
          <Button variant="outline" borderColor="slate.200" colorScheme="gray" size="sm">
            Back
          </Button>
        </Link>
      </Flex>

      <Box borderRadius="xl" borderWidth={1} borderColor="slate.200" bg="white" p={6} boxShadow="sm">
        <Heading as="h2" size="md" color="slate.900" mb={4}>
          Audit Timeline
        </Heading>
        <VStack spacing={4} align="stretch">
          {timeline.map((item) => (
            <Box key={item.title} borderRadius="lg" borderWidth={1} borderColor="slate.200" p={4}>
              <Flex align="center" justify="space-between" mb={2}>
                <Text fontSize="sm" fontWeight="medium" color="slate.900">
                  {item.title}
                </Text>
                <Text fontSize="xs" color="slate.500">
                  {item.time}
                </Text>
              </Flex>
              <Text fontSize="sm" color="slate.600" mt={2}>
                {item.description}
              </Text>
            </Box>
          ))}
        </VStack>
      </Box>
    </Layout>
  )
}
