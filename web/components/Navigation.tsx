"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { useDispatch, useSelector } from "react-redux"
import { useEffect, useState } from "react"
import { Box, Flex, HStack, Button, Text } from "@chakra-ui/react"
import type { RootState } from "../store"
import { clearToken } from "../store/sessionSlice"
import { clearStoredToken } from "../store/token"

const navItems = [
  { href: "/", label: "Dashboard" },
  { href: "/workflows", label: "Workflows" },
  { href: "/policies", label: "Policies" },
  { href: "/executions", label: "Executions" },
]

export function Navigation() {
  const pathname = usePathname()
  const router = useRouter()
  const dispatch = useDispatch()
  const token = useSelector((state: RootState) => state.session.token)
  const roles = useSelector((state: RootState) => state.session.roles)
  const [isHydrated, setIsHydrated] = useState(false)

  useEffect(() => {
    setIsHydrated(true)
  }, [])

  const handleLogout = async () => {
    try {
      await fetch("/api/auth/logout", { method: "POST" })
    } catch {
      // Ignore logout errors and clear client state anyway.
    }
    clearStoredToken()
    dispatch(clearToken())
    router.push("/login")
  }

  return (
    <Box
      as="nav"
      bg="white"
      opacity={0.9}
      backdropFilter="blur(10px)"
      borderBottom="2px solid"
      borderColor="blue.200"
      boxShadow="md"
    >
      <Flex
        maxW="7xl"
        mx="auto"
        px={{ base: 4, sm: 6, lg: 12 }}
        h={20}
        align="center"
        justify="space-between"
      >
        <Flex align="center" gap={8}>
          <Link href="/" style={{ flex: "0 0 auto" }}>
            <Text
              fontSize="3xl"
              fontWeight="extrabold"
              color="blue.700"
              letterSpacing="tight"
              textShadow="0 1px 2px rgba(0,0,0,0.05)"
            >
              Auctor
            </Text>
          </Link>
          <HStack gap={6} display={{ base: "none", sm: "flex" }}>
            {navItems.map((item) => {
              const isActive = pathname === item.href
              return (
                <Link key={item.href} href={item.href}>
                  <Button
                    as="div"
                    variant={isActive ? "solid" : "ghost"}
                    colorScheme={isActive ? "blue" : "gray"}
                    px={4}
                    py={2}
                    fontSize="lg"
                    fontWeight="semibold"
                    borderRadius="lg"
                    transition="all 0.15s ease"
                    border={isActive ? "1px solid" : "none"}
                    borderColor={isActive ? "blue.300" : undefined}
                    cursor="pointer"
                  >
                    {item.label}
                  </Button>
                </Link>
              )
            })}
          </HStack>
        </Flex>
        {isHydrated && token && (
          <Flex align="center">
            <Text
              fontSize="sm"
              fontWeight="semibold"
              letterSpacing="0.06em"
              color="blue.700"
              mr={4}
            >
              ROLE: {roles.length ? roles.join(", ") : "UNKNOWN"}
            </Text>
            <Button
              onClick={handleLogout}
              colorScheme="gray"
              variant="outline"
              fontSize="lg"
              fontWeight="semibold"
              borderRadius="lg"
              ml={6}
              _hover={{ bg: "blue.100", color: "blue.700" }}
              transition="all 0.15s ease"
            >
              Sign out
            </Button>
          </Flex>
        )}
      </Flex>
    </Box>
  )
}
