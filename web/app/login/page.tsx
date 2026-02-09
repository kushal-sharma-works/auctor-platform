"use client"

import { useRouter } from "next/navigation"
import { useState } from "react"
import {
  Box,
  Button,
  Flex,
  Input,
  VStack,
  Heading,
  Text,
} from "@chakra-ui/react"
import {
  FormControl,
  FormLabel,
} from "@chakra-ui/react/form-control"
import { useAppDispatch } from "../../store/hooks"
import { setToken } from "../../store/sessionSlice"
import { setStoredToken } from "../../store/token"

export default function LoginPage() {
  const router = useRouter()
  const dispatch = useAppDispatch()
  const [token, setTokenValue] = useState("")

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (token) {
      dispatch(setToken(token))
      setStoredToken(token)
      router.push("/")
    }
  }

  return (
    <Flex minH="screen" align="center" justify="center" px={4}>
      <Box
        w="full"
        maxW="md"
        borderRadius="2xl"
        borderWidth={1}
        borderColor="slate.200"
        bg="white"
        p={8}
        boxShadow="lg"
      >
        <VStack align="start" spacing={2} mb={6}>
          <Text fontSize="xs" fontWeight="bold" textTransform="uppercase" letterSpacing="0.3em" color="blue.600">
            Auctor
          </Text>
          <Heading as="h2" size="lg" color="slate.900">
            Welcome back
          </Heading>
          <Text fontSize="sm" color="slate.600">
            Enter your authentication token to access the platform.
          </Text>
        </VStack>

        <form onSubmit={handleSubmit}>
          <VStack spacing={5}>
            <FormControl>
              <FormLabel fontSize="sm" fontWeight="medium" color="slate.700">
                Token
              </FormLabel>
              <Input
                id="token"
                name="token"
                type="text"
                required
                value={token}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setTokenValue(e.target.value)}
                placeholder="Paste your JWT token"
                borderColor="slate.300"
                _focus={{
                  borderColor: "blue.500",
                  boxShadow: "0 0 0 2px rgba(59, 130, 246, 0.1)",
                }}
              />
            </FormControl>

            <Button
              type="submit"
              w="full"
              colorScheme="blue"
              fontSize="sm"
              fontWeight="medium"
            >
              Sign in
            </Button>
          </VStack>
        </form>
      </Box>
    </Flex>
  )
}
