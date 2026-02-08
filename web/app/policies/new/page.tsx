"use client"

import { useRouter } from "next/navigation"
import { useForm, useFieldArray } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import {
  Box,
  Button,
  Input,
  VStack,
  HStack,
  Heading,
  Text,
} from "@chakra-ui/react"
import {
  FormControl,
  FormLabel,
  FormErrorMessage,
} from "@chakra-ui/react/form-control"
import { Select } from "@chakra-ui/react/select"
import { Layout } from "../../../components/Layout"
import { useCreatePolicy } from "../../../hooks/useGraphql"

const conditionSchema = z.object({
  field: z.string().min(1, "Field is required"),
  operator: z.enum(["EQ", "NEQ", "GT", "LT", "GTE", "LTE", "IN", "NOT_IN"], {
    required_error: "Operator is required",
  }),
  value: z.string().min(1, "Value is required"),
})

const policySchema = z.object({
  name: z.string().min(1, "Name is required").max(100, "Name too long"),
  conditions: z.array(conditionSchema).min(1, "At least one condition is required"),
})

type PolicyFormData = z.infer<typeof policySchema>

export default function NewPolicyPage() {
  const router = useRouter()
  const createPolicy = useCreatePolicy()
  const {
    register,
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<PolicyFormData>({
    resolver: zodResolver(policySchema),
    defaultValues: {
      name: "",
      conditions: [{ field: "", operator: "EQ", value: "" }],
    },
  })

  const { fields, append, remove } = useFieldArray({
    control,
    name: "conditions",
  })

  const onSubmit = async (data: PolicyFormData) => {
    try {
      await createPolicy.mutateAsync({
        name: data.name,
        conditions: data.conditions,
      })
      router.push("/policies")
    } catch (error) {
      console.error("Failed to create policy:", error)
    }
  }

  return (
    <Layout>
      <VStack align="stretch" spacing={8}>
        <VStack align="start" spacing={2}>
          <Heading as="h1" size="xl" color="slate.900">
            Create Policy
          </Heading>
          <VStack align="start" spacing={2}>
            <Text fontSize="sm" color="slate.600">
              Define a new policy with conditions. Example fields: <code>order.amount</code>, <code>user.tier</code>, <code>transaction.type</code>
            </Text>
            <Box fontSize="xs" color="slate.500" bg="blue.50" p={3} borderRadius="md" borderLeftWidth={3} borderLeftColor="blue.500">
              <Text fontWeight="bold" mb={1}>Operators:</Text>
              <Text>EQ = Equals | NEQ = Not Equals | GT = Greater Than | LT = Less Than | GTE = ≥ | LTE = ≤ | IN = In Set | NOT_IN = Not In Set</Text>
            </Box>
          </VStack>
        </VStack>

        <Box
          as="form"
          onSubmit={handleSubmit(onSubmit)}
          borderRadius="xl"
          borderWidth={1}
          borderColor="slate.200"
          bg="white"
          p={6}
          boxShadow="sm"
          maxW="4xl"
        >
          <VStack spacing={6}>
            {/* Policy Name */}
            <FormControl isInvalid={!!errors.name} w="full">
              <FormLabel fontSize="sm" fontWeight="medium" color="gray.700">
                Policy Name
              </FormLabel>
              <Input
                type="text"
                id="name"
                {...register("name")}
                placeholder="Enter policy name"
                borderColor="gray.300"
                _focus={{
                  borderColor: "blue.500",
                  boxShadow: "0 0 0 2px rgba(59, 130, 246, 0.1)",
                }}
              />
              {errors.name && (
                <FormErrorMessage>{errors.name.message}</FormErrorMessage>
              )}
            </FormControl>

            {/* Conditions */}
            <FormControl w="full">
              <HStack justify="space-between" mb={3}>
                <FormLabel fontSize="sm" fontWeight="medium" color="gray.700" m={0}>
                  Conditions
                </FormLabel>
                <Button
                  type="button"
                  onClick={() => append({ field: "", operator: "EQ", value: "" })}
                  size="sm"
                  colorScheme="blue"
                  variant="outline"
                >
                  + Add Condition
                </Button>
              </HStack>
              <VStack spacing={3} align="stretch">
                {fields.map((field, index) => (
                  <HStack key={field.id} gap={2} align="flex-start">
                    <Input
                      placeholder="Field (e.g., order.amount)"
                      {...register(`conditions.${index}.field`)}
                      borderColor="gray.300"
                      _focus={{
                        borderColor: "blue.500",
                        boxShadow: "0 0 0 2px rgba(59, 130, 246, 0.1)",
                      }}
                    />
                    <Select
                      {...register(`conditions.${index}.operator`)}
                      borderColor="gray.300"
                      _focus={{
                        borderColor: "blue.500",
                        boxShadow: "0 0 0 2px rgba(59, 130, 246, 0.1)",
                      }}
                      placeholder="Select operator"
                    >
                      <option value="EQ">Equals (=)</option>
                      <option value="NEQ">Not Equals (≠)</option>
                      <option value="GT">Greater Than (&gt;)</option>
                      <option value="LT">Less Than (&lt;)</option>
                      <option value="GTE">Greater or Equal (≥)</option>
                      <option value="LTE">Less or Equal (≤)</option>
                      <option value="IN">In Set (comma-separated)</option>
                      <option value="NOT_IN">Not In Set (comma-separated)</option>
                    </Select>
                    <Input
                      placeholder="Value"
                      {...register(`conditions.${index}.value`)}
                      borderColor="gray.300"
                      _focus={{
                        borderColor: "blue.500",
                        boxShadow: "0 0 0 2px rgba(59, 130, 246, 0.1)",
                      }}
                    />
                    {fields.length > 1 && (
                      <Button
                        type="button"
                        onClick={() => remove(index)}
                        size="sm"
                        colorScheme="red"
                        variant="ghost"
                      >
                        ✕
                      </Button>
                    )}
                  </HStack>
                ))}
              </VStack>
            </FormControl>

            {/* Form Actions */}
            <HStack justify="flex-end" gap={3} w="full" pt={4}>
              <Button
                type="button"
                onClick={() => router.back()}
                variant="outline"
                borderColor="gray.300"
                colorScheme="gray"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                colorScheme="blue"
                isLoading={isSubmitting || createPolicy.isPending}
                loadingText="Creating..."
              >
                Create Policy
              </Button>
            </HStack>
          </VStack>
        </Box>
      </VStack>
    </Layout>
  )
}
