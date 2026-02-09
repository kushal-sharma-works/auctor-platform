"use client"

import { Box, Button, Flex, Heading, Text, VStack } from "@chakra-ui/react"

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  return (
    <Flex align="center" justify="center" minH="100vh">
      <VStack spacing={4} textAlign="center">
        <Heading as="h2" size="2xl" color="slate.900">
          Something went wrong
        </Heading>
        <Text fontSize="sm" color="slate.600" mt={3}>
          {error.message}
        </Text>
        <Button onClick={reset} colorScheme="blue" size="md" mt={6}>
          Try again
        </Button>
      </VStack>
    </Flex>
  )
}
