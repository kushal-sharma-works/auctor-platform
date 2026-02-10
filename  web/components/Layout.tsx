"use client"

import { Box, Container } from "@chakra-ui/react"
import { Navigation } from "./Navigation"

export function Layout({ children }: { children: React.ReactNode }) {
  return (
    <Box minH="100vh" bg="gray.50">
      <Navigation />
      <Container as="main" maxW="7xl" px={{ base: 4, sm: 6, lg: 8 }} py={12}>
        {children}
      </Container>
    </Box>
  )
}
