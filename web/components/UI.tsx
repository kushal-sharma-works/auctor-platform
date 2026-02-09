"use client"

import { ReactNode } from "react"
import { 
  Box, 
  Badge as ChakraBadge,
  Button as ChakraButton,
  BadgeProps,
  ButtonProps,
} from "@chakra-ui/react"

export function Card({ 
  children, 
  className = "" 
}: { 
  children: ReactNode
  className?: string 
}) {
  return (
    <Box
      bg="white"
      _dark={{ bg: "gray.900" }}
      borderRadius="lg"
      boxShadow="md"
      p={6}
      className={className}
    >
      {children}
    </Box>
  )
}

export function Badge({ 
  status,
  className = "" 
}: { 
  status: string
  className?: string 
}) {
  const statusColorScheme: Record<string, string> = {
    DRAFT: "yellow",
    PUBLISHED: "green",
    ARCHIVED: "gray",
  }
  
  const colorScheme = statusColorScheme[status] || "yellow"
  
  return (
    <ChakraBadge
      colorScheme={colorScheme}
      px={3}
      py={1}
      borderRadius="full"
      fontSize="sm"
      fontWeight="medium"
      className={className}
    >
      {status}
    </ChakraBadge>
  )
}

export function Button({
  children,
  variant = "primary",
  size = "md",
  className = "",
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "danger"
  size?: "sm" | "md" | "lg"
}) {
  const variantColorScheme = {
    primary: "blue",
    secondary: "gray",
    danger: "red",
  }
  
  const chakraVariant = variant === "primary" ? "solid" : variant === "danger" ? "solid" : "outline"
  const colorScheme = variantColorScheme[variant]
  
  const sizeMap = {
    sm: "sm",
    md: "md",
    lg: "lg",
  } as const
  
  return (
    <ChakraButton
      colorScheme={colorScheme}
      variant={chakraVariant}
      size={sizeMap[size]}
      className={className}
      {...(props as ButtonProps)}
    >
      {children}
    </ChakraButton>
  )
}
