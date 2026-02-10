"use client"

import { useRouter } from "next/navigation"
import { useForm, useFieldArray, useWatch } from "react-hook-form"
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
  Alert,
  AlertIcon,
} from "@chakra-ui/react"
import {
  FormControl,
  FormLabel,
  FormErrorMessage,
} from "@chakra-ui/react/form-control"
import { Select } from "@chakra-ui/react/select"
import { Layout } from "../../../components/Layout"
import { useCreateWorkflow } from "../../../hooks/useGraphql"
import { useSelector } from "react-redux"
import type { RootState } from "../../../store"

const transitionSchema = z.object({
  fromState: z.string().min(1, "From state is required"),
  toState: z.string().min(1, "To state is required"),
  policyRef: z.string().optional(),
})

const workflowSchema = z.object({
  name: z.string().min(1, "Name is required").max(100, "Name too long"),
  states: z.string().min(1, "States are required (comma-separated)"),
  initialState: z.string().min(1, "Initial state is required"),
  transitions: z.array(transitionSchema).min(1, "At least one transition is required"),
})

type WorkflowFormData = z.infer<typeof workflowSchema>

export default function NewWorkflowPage() {
  const router = useRouter()
  const createWorkflow = useCreateWorkflow()
  const roles = useSelector((state: RootState) => state.session.roles)
  const canAdmin = roles.includes("ADMIN")
  const {
    register,
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
    watch,
  } = useForm<WorkflowFormData>({
    resolver: zodResolver(workflowSchema),
    defaultValues: {
      name: "",
      states: "",
      initialState: "",
      transitions: [{ fromState: "", toState: "", policyRef: "" }],
    },
  })

  // Watch the states field to dynamically generate dropdown options
  const statesInput = watch("states") || ""
  const parsedStates = statesInput
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0)

  const { fields, append, remove } = useFieldArray({
    control,
    name: "transitions",
  })

  const onSubmit = async (data: WorkflowFormData) => {
    if (!canAdmin) return
    try {
      const states = data.states.split(",").map((s) => s.trim())
      const payload = {
        name: data.name,
        states,
        initialState: data.initialState,
        transitions: data.transitions.map((transition) => ({
          fromState: transition.fromState,
          toState: transition.toState,
          policyRef: transition.policyRef || null,
        })),
      }

      await createWorkflow.mutateAsync(payload)
      router.push("/workflows")
    } catch (error) {
      console.error("Failed to create workflow:", error)
    }
  }

  return (
    <Layout>
      <VStack align="stretch" spacing={8}>
        <VStack align="start" spacing={2}>
          <Heading as="h1" size="xl" color="slate.900">
            Create Workflow
          </Heading>
          <Text fontSize="sm" color="slate.600">
            Define a new workflow with states and transitions. Example: States (PENDING, APPROVED, REJECTED), Initial State (must be one of the states), Transitions (e.g., PENDING → APPROVED).
          </Text>
        </VStack>

        {!canAdmin && (
          <Alert status="warning" borderRadius="md">
            <AlertIcon />
            You need ADMIN access to create workflows.
          </Alert>
        )}

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
          opacity={canAdmin ? 1 : 0.5}
          pointerEvents={canAdmin ? "auto" : "none"}
        >
          <VStack spacing={6}>
            {/* Workflow Name */}
            <FormControl isInvalid={!!errors.name} w="full">
              <FormLabel fontSize="sm" fontWeight="medium" color="gray.700">
                Workflow Name
              </FormLabel>
              <Input
                type="text"
                id="name"
                {...register("name")}
                placeholder="Enter workflow name"
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

            {/* States */}
            <FormControl isInvalid={!!errors.states} w="full">
              <FormLabel fontSize="sm" fontWeight="medium" color="gray.700">
                States (comma-separated)
              </FormLabel>
              <Input
                type="text"
                id="states"
                placeholder="PENDING, APPROVED, REJECTED"
                {...register("states")}
                borderColor="gray.300"
                _focus={{
                  borderColor: "blue.500",
                  boxShadow: "0 0 0 2px rgba(59, 130, 246, 0.1)",
                }}
              />
              {errors.states && (
                <FormErrorMessage>{errors.states.message}</FormErrorMessage>
              )}
            </FormControl>

            {/* Initial State */}
            <FormControl isInvalid={!!errors.initialState} w="full">
              <FormLabel fontSize="sm" fontWeight="medium" color="gray.700">
                Initial State
              </FormLabel>
              {parsedStates.length === 0 ? (
                <Alert status="info" borderRadius="md" fontSize="sm">
                  <AlertIcon />
                  Enter states first to select initial state
                </Alert>
              ) : (
                <Select
                  id="initialState"
                  {...register("initialState")}
                  borderColor="gray.300"
                  _focus={{
                    borderColor: "blue.500",
                    boxShadow: "0 0 0 2px rgba(59, 130, 246, 0.1)",
                  }}
                  placeholder="Select initial state"
                >
                  {parsedStates.map((state) => (
                    <option key={state} value={state}>
                      {state}
                    </option>
                  ))}
                </Select>
              )}
              {errors.initialState && (
                <FormErrorMessage>{errors.initialState.message}</FormErrorMessage>
              )}
            </FormControl>

            {/* Transitions */}
            <FormControl w="full">
              <HStack justify="space-between" mb={3}>
                <FormLabel fontSize="sm" fontWeight="medium" color="gray.700" m={0}>
                  Transitions
                </FormLabel>
                <Button
                  type="button"
                  onClick={() => append({ fromState: "", toState: "", policyRef: "" })}
                  size="sm"
                  colorScheme="blue"
                  variant="outline"
                  isDisabled={parsedStates.length === 0}
                >
                  + Add Transition
                </Button>
              </HStack>
              {parsedStates.length === 0 ? (
                <Alert status="info" borderRadius="md" fontSize="sm">
                  <AlertIcon />
                  Enter states first to add transitions
                </Alert>
              ) : (
                <VStack spacing={3} align="stretch">
                  {fields.map((field, index) => (
                    <HStack key={field.id} gap={2} align="flex-start">
                      <Select
                        {...register(`transitions.${index}.fromState`)}
                        borderColor="gray.300"
                        _focus={{
                          borderColor: "blue.500",
                          boxShadow: "0 0 0 2px rgba(59, 130, 246, 0.1)",
                        }}
                        placeholder="From State"
                      >
                        {parsedStates.map((state) => (
                          <option key={`from-${state}`} value={state}>
                            {state}
                          </option>
                        ))}
                      </Select>
                      <Text pt={2} px={2} fontWeight="bold" color="gray.500">
                        →
                      </Text>
                      <Select
                        {...register(`transitions.${index}.toState`)}
                        borderColor="gray.300"
                        _focus={{
                          borderColor: "blue.500",
                          boxShadow: "0 0 0 2px rgba(59, 130, 246, 0.1)",
                        }}
                        placeholder="To State"
                      >
                        {parsedStates.map((state) => (
                          <option key={`to-${state}`} value={state}>
                            {state}
                          </option>
                        ))}
                      </Select>
                      <Input
                        placeholder="Policy ID (optional)"
                        {...register(`transitions.${index}.policyRef`)}
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
              )}
            </FormControl>

            <Text fontSize="sm" color="gray.500">
              Use the policy ID from the policy detail page (not the name).
            </Text>

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
                isLoading={isSubmitting || createWorkflow.isPending}
                loadingText="Creating..."
              >
                Create Workflow
              </Button>
            </HStack>
          </VStack>
        </Box>
      </VStack>
    </Layout>
  )
}
