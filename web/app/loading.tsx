import { Flex, Text, Spinner } from "@chakra-ui/react"

export default function Loading() {
  return (
    <Flex align="center" justify="center" minH="100vh" gap={4}>
      <Spinner size="md" color="blue.500" />
      <Text fontSize="sm" color="slate.500">
        Loading application…
      </Text>
    </Flex>
  )
}
