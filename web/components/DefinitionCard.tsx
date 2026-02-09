import { Box, Text } from "@chakra-ui/react"

export function DefinitionCard({
  title,
  value,
  subtitle,
}: {
  title: string
  value: string
  subtitle: string
}) {
  return (
    <Box
      borderRadius="xl"
      borderWidth={2}
      borderColor="gray.200"
      bg="white"
      p={8}
      boxShadow="md"
      minH="140px"
      display="flex"
      flexDirection="column"
      justifyContent="center"
      alignItems="flex-start"
    >
      <Text fontSize="md" fontWeight="medium" color="gray.500" mb={1}>
        {title}
      </Text>
      <Text fontSize="3xl" fontWeight="bold" color="gray.900" mb={2}>
        {value}
      </Text>
      <Text fontSize="sm" color="gray.500">
        {subtitle}
      </Text>
    </Box>
  )
}
