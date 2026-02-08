import Link from "next/link"
import { Box, Button, Flex, Heading, Text, VStack } from "@chakra-ui/react"

export default function NotFound() {
  return (
    <Flex align="center" justify="center" minH="100vh" bg="gray.50">
      <VStack spacing={4} textAlign="center">
        <Heading as="h1" size="4xl" color="gray.900" mb={4}>
          404
        </Heading>
        <Heading as="h2" size="xl" color="gray.700" mb={4}>
          Page Not Found
        </Heading>
        <Text color="gray.600" mb={6}>
          The page you are looking for does not exist.
        </Text>
        <Link href="/">
          <Button colorScheme="blue" size="md">
            Go Home
          </Button>
        </Link>
      </VStack>
    </Flex>
  )
}
