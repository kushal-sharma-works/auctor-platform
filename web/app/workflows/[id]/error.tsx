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
    <Flex align="center" justify="center" py={20}>
      <VStack spacing={4} textAlign="center">
        <Heading as="h2" size="lg" color="slate.900">
          Unable to load workflow
        </Heading>
        <Text fontSize="sm" color="slate.600">
          {error.message}
        </Text>
        <Button onClick={reset} colorScheme="blue" size="md" mt={2}>
          Try again
        </Button>
      </VStack>
    </Flex>
  )
}
