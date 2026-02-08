"use client"

import { useParams } from "next/navigation"
import Link from "next/link"
import { useState } from "react"
import {
  Box,
  Button,
  Flex,
  Grid,
  GridItem,
  Heading,
  Text,
  VStack,
  Stack,
  HStack,
  Badge as ChakraBadge,
  Alert,
  AlertIcon,
  AlertTitle,
  AlertDescription,
} from "@chakra-ui/react"
import { Layout } from "../../../components/Layout"
import { Badge } from "../../../components/UI"
import { usePolicy, usePublishPolicy } from "../../../hooks/useGraphql"

export default function PolicyDetailPage() {
  const params = useParams()
  const policyId = Array.isArray(params.id) ? params.id[0] : params.id
  const { data, isLoading, error, refetch } = usePolicy(policyId ?? "")
  const publishMutation = usePublishPolicy()
  const [publishError, setPublishError] = useState<string | null>(null)

  const policy = data?.policy

  const handlePublish = async () => {
    if (!policyId) return
    setPublishError(null)
    try {
      await publishMutation.mutateAsync(policyId)
      await refetch()
    } catch (err: any) {
      const errorMessage = err?.response?.errors?.[0]?.message || "Failed to publish policy"
      setPublishError(errorMessage)
    }
  }

  return (
    <Layout>
      <Flex direction="row" align="flex-start" justify="space-between" mb={8}>
        <VStack align="flex-start" spacing={2}>
          <Text fontSize="xs" fontWeight="bold" textTransform="uppercase" letterSpacing="0.2em" color="blue.600">
            Policy
          </Text>
          <Heading as="h1" size="xl" color="slate.900">
            {policy?.name ?? "Policy"}
          </Heading>
          <Text fontSize="sm" color="slate.600">
            Review conditions, version history, and approval rules.
          </Text>
        </VStack>
        <HStack gap={2}>
          <Link href="/policies">
            <Button variant="outline" borderColor="slate.200" colorScheme="gray" size="sm">
              Back
            </Button>
          </Link>
          {policy && (
            <Box
              title={policy.status !== "DRAFT" ? `Policy is ${policy.status} - only DRAFT policies can be published` : ""}
            >
              <Button
                onClick={handlePublish}
                isLoading={publishMutation.isPending}
                loadingText="Publishing..."
                colorScheme="blue"
                size="sm"
                isDisabled={policy.status !== "DRAFT"}
              >
                Publish
              </Button>
            </Box>
          )}
        </HStack>
      </Flex>

      {isLoading && (
        <Text mt={8} fontSize="sm" color="slate.500">
          Loading policy…
        </Text>
      )}
      {error && (
        <Alert status="error" borderRadius="md" mb={6}>
          <AlertIcon />
          <Box>
            <AlertTitle>Unable to load policy</AlertTitle>
          </Box>
        </Alert>
      )}
      {publishError && (
        <Alert status="error" borderRadius="md" mb={6}>
          <AlertIcon />
          <Box>
            <AlertTitle>Publish Failed</AlertTitle>
            <AlertDescription>{publishError}</AlertDescription>
          </Box>
        </Alert>
      )}

      {policy && (
        <Grid templateColumns={{ base: "1fr", lg: "2fr 1fr" }} gap={6}>
          <GridItem>
            <Stack spacing={6}>
              {/* Details Section */}
              <Box borderRadius="xl" borderWidth={1} borderColor="slate.200" bg="white" p={6} boxShadow="sm">
                <Flex align="center" justify="space-between" mb={4}>
                  <Heading as="h2" size="md" color="slate.900">
                    Details
                  </Heading>
                  <Badge status={policy.status} />
                </Flex>
                <Grid templateColumns={{ base: "1fr", sm: "1fr 1fr" }} gap={4} mt={4}>
                  <Box>
                    <Text fontSize="xs" fontWeight="bold" textTransform="uppercase" color="slate.500" mb={1}>
                      Version
                    </Text>
                    <Text fontSize="sm" fontWeight="medium" color="slate.900">
                      {policy.version}
                    </Text>
                  </Box>
                  <Box>
                    <Text fontSize="xs" fontWeight="bold" textTransform="uppercase" color="slate.500" mb={1}>
                      Created
                    </Text>
                    <Text fontSize="sm" fontWeight="medium" color="slate.900">
                      {new Date(policy.createdAt).toLocaleString()}
                    </Text>
                  </Box>
                </Grid>
              </Box>

              {/* Conditions Section */}
              <Box borderRadius="xl" borderWidth={1} borderColor="slate.200" bg="white" p={6} boxShadow="sm">
                <Heading as="h2" size="md" color="slate.900" mb={4}>
                  Conditions
                </Heading>
                <Stack spacing={3}>
                  {policy.conditions.map((condition, index) => (
                    <Flex
                      key={`${condition.field}-${index}`}
                      align="center"
                      justify="space-between"
                      borderRadius="lg"
                      borderWidth={1}
                      borderColor="slate.200"
                      px={4}
                      py={3}
                    >
                      <Box>
                        <Text fontSize="sm" fontWeight="medium" color="slate.900">
                          {condition.field}
                        </Text>
                        <Text fontSize="xs" color="slate.500">
                          {condition.operator}
                        </Text>
                      </Box>
                      <ChakraBadge variant="outline" colorScheme="gray">
                        {condition.value}
                      </ChakraBadge>
                    </Flex>
                  ))}
                </Stack>
              </Box>
            </Stack>
          </GridItem>

          {/* Sidebar */}
          <GridItem>
            <Stack spacing={6}>
              <Box borderRadius="xl" borderWidth={1} borderColor="slate.200" bg="white" p={6} boxShadow="sm">
                <Heading as="h2" size="md" color="slate.900" mb={3}>
                  Usage
                </Heading>
                <Text fontSize="sm" color="slate.600">
                  Link this policy to workflow transitions to control approvals.
                </Text>
              </Box>
            </Stack>
          </GridItem>
        </Grid>
      )}
    </Layout>
  )
}
