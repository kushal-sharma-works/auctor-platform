import { useMemo, useState } from "react"
import { useQueryClient } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import {
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalFooter,
  ModalBody,
  ModalCloseButton,
  Button,
  FormControl,
  FormLabel,
  Input,
  VStack,
  Text,
  useToast,
  Box,
  HStack,
} from "@chakra-ui/react"
import type { Workflow } from "../graphql/types"
import { usePolicies, useWorkflow, useWorkflows } from "../hooks/useGraphql"
import { useStartExecution } from "../hooks/useExecution"

interface StartExecutionModalProps {
  isOpen: boolean
  onClose: () => void
}

function parseStartExecutionError(error: unknown): string {
  const rawMessage = error instanceof Error ? error.message : "Failed to start execution"

  const withoutPrefix = rawMessage
    .replace(/^Exception while fetching data \(\/startExecution\)\s*:\s*/i, "")
    .split(': {"response"')[0]
    .trim()

  if (/is not PUBLISHED/i.test(withoutPrefix)) {
    return "Selected workflow is in Draft state. Publish it first, then start execution."
  }

  if (/Parent job is Cancelled/i.test(withoutPrefix)) {
    return "Execution request was interrupted. Please try again."
  }

  if (/FORBIDDEN|missing role|Unauthorized|UNAUTHENTICATED/i.test(withoutPrefix)) {
    return "You do not have permission to start executions."
  }

  return withoutPrefix || "Failed to start execution"
}

export function StartExecutionModal({ isOpen, onClose }: StartExecutionModalProps) {
  const router = useRouter()
  const toast = useToast()
  const queryClient = useQueryClient()

  const [selectedWorkflow, setSelectedWorkflow] = useState<string>("")
  const [selectedVersion, setSelectedVersion] = useState<number>(1)
  const [inputs, setInputs] = useState<Record<string, string>>({})
  const [inputKey, setInputKey] = useState<string>("")
  const [inputValue, setInputValue] = useState<string>("")

  const { data: workflowData } = useWorkflows(0, 100)
  const { data: workflowDetails } = useWorkflow(selectedWorkflow)
  const { data: policyData } = usePolicies(0, 200)
  const startExecutionMutation = useStartExecution()

  const workflows = (workflowData?.workflows?.content || []) as Workflow[]

  const selectedWorkflowObj = workflows.find((w: Workflow) => w.id === selectedWorkflow)

  const policyVariableOptions = useMemo(() => {
    const workflow = workflowDetails?.workflow
    if (!workflow) return [] as string[]

    const policyRefs = new Set<string>(
      (workflow.transitions || [])
        .map((transition: { policyRef?: string | null }) => transition.policyRef)
        .filter((policyRef: string | null | undefined): policyRef is string => Boolean(policyRef))
    )

    if (policyRefs.size === 0) return [] as string[]

    const policies = policyData?.policies?.content || []
    const fields: string[] = policies
      .filter((policy: { id: string }) => policyRefs.has(policy.id))
      .flatMap((policy: { conditions?: Array<{ field?: string }> }) =>
        (policy.conditions || [])
          .map((condition) => condition.field)
          .filter((field): field is string => Boolean(field))
      )

    return Array.from(new Set(fields)).sort((a, b) => a.localeCompare(b))
  }, [policyData?.policies?.content, workflowDetails?.workflow])

  const handleAddInput = () => {
    if (inputKey && inputValue) {
      setInputs({ ...inputs, [inputKey]: inputValue })
      setInputKey("")
      setInputValue("")
    }
  }

  const handleRemoveInput = (key: string) => {
    const newInputs = { ...inputs }
    delete newInputs[key]
    setInputs(newInputs)
  }

  const handleStartExecution = async () => {
    if (!selectedWorkflow) {
      toast({
        title: "Error",
        description: "Please select a workflow",
        status: "error",
        duration: 3000,
        isClosable: true,
      })
      return
    }

    try {
      await startExecutionMutation.mutateAsync({
        workflowId: selectedWorkflow,
        workflowVersion: selectedVersion,
        input: inputs,
      })

      toast({
        title: "Success",
        description: "Execution started successfully",
        status: "success",
        duration: 3000,
        isClosable: true,
      })

      // Invalidate executions query to refetch
      queryClient.invalidateQueries({ queryKey: ["executions"] })

      // Reset form
      setSelectedWorkflow("")
      setSelectedVersion(1)
      setInputs({})
      setInputKey("")
      setInputValue("")

      onClose()

      // Redirect to executions page
      router.push("/executions")
    } catch (error) {
      toast({
        title: "Cannot start execution",
        description: parseStartExecutionError(error),
        status: "error",
        duration: 5000,
        isClosable: true,
      })
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="lg">
      <ModalOverlay />
      <ModalContent>
        <ModalHeader color="slate.900" fontSize="lg" fontWeight="bold">
          Start New Execution
        </ModalHeader>
        <ModalCloseButton />
        <ModalBody>
          <VStack align="stretch" spacing={4}>
            {/* Workflow Selection */}
            <FormControl isRequired>
              <FormLabel fontSize="sm" fontWeight="medium" color="slate.700">
                Workflow
              </FormLabel>
              <select
                value={selectedWorkflow}
                onChange={(e) => {
                  setSelectedWorkflow(e.target.value)
                  const workflow = workflows.find((w: Workflow) => w.id === e.target.value)
                  if (workflow) {
                    setSelectedVersion(workflow.version)
                  }
                  setInputs({})
                  setInputKey("")
                  setInputValue("")
                }}
                style={{
                  width: "100%",
                  padding: "8px 12px",
                  borderRadius: "6px",
                  border: "1px solid #e2e8f0",
                  fontSize: "14px",
                  fontFamily: "inherit",
                }}
              >
                <option value="">Select a workflow...</option>
                {workflows.map((workflow: Workflow) => (
                  <option key={workflow.id} value={workflow.id}>
                    {workflow.name} (v{workflow.version})
                  </option>
                ))}
              </select>
            </FormControl>

            {/* Version Info */}
            {selectedWorkflowObj && (
              <Box
                p={3}
                bg="slate.50"
                borderRadius="md"
                borderLeft="4px solid"
                borderLeftColor="blue.500"
              >
                <Text fontSize="xs" color="slate.600" fontWeight="medium">
                  Version: {selectedVersion}
                </Text>
              </Box>
            )}

            {/* Input Parameters */}
            <FormControl>
              <FormLabel fontSize="sm" fontWeight="medium" color="slate.700">
                Input Parameters
              </FormLabel>
              <Text fontSize="xs" color="slate.500">
                Select keys from workflow-linked policy fields and provide values.
              </Text>
              <VStack align="stretch" spacing={2}>
                {/* Input Key-Value Adder */}
                <HStack spacing={2}>
                  {policyVariableOptions.length > 0 ? (
                    <select
                      value={inputKey}
                      onChange={(e) => setInputKey(e.target.value)}
                      style={{
                        width: "100%",
                        padding: "8px 12px",
                        borderRadius: "6px",
                        border: "1px solid #e2e8f0",
                        fontSize: "14px",
                        fontFamily: "inherit",
                      }}
                    >
                      <option value="">Select policy variable...</option>
                      {policyVariableOptions.map((field) => (
                        <option key={field} value={field}>
                          {field}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <Input
                      placeholder="Key"
                      value={inputKey}
                      onChange={(e) => setInputKey(e.target.value)}
                      size="sm"
                    />
                  )}
                  <Input
                    placeholder="Value"
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    size="sm"
                  />
                  <Button
                    onClick={handleAddInput}
                    size="sm"
                    colorScheme="blue"
                    isDisabled={!inputKey || !inputValue}
                    width="60px"
                  >
                    + Add
                  </Button>
                </HStack>

                {/* Display Added Inputs */}
                {Object.entries(inputs).map(([key, value]) => (
                  <HStack
                    key={key}
                    p={2}
                    bg="slate.50"
                    borderRadius="md"
                    justify="space-between"
                  >
                    <VStack align="start" spacing={0}>
                      <Text fontSize="xs" fontWeight="bold" color="slate.700">
                        {key}
                      </Text>
                      <Text fontSize="xs" color="slate.600">
                        {value}
                      </Text>
                    </VStack>
                    <Button
                      onClick={() => handleRemoveInput(key)}
                      size="xs"
                      colorScheme="red"
                      variant="ghost"
                    >
                      ✕
                    </Button>
                  </HStack>
                ))}
              </VStack>
            </FormControl>
          </VStack>
        </ModalBody>

        <ModalFooter>
          <HStack spacing={2}>
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button
              colorScheme="blue"
              onClick={handleStartExecution}
              isLoading={startExecutionMutation.isPending}
              isDisabled={!selectedWorkflow}
            >
              Start Execution
            </Button>
          </HStack>
        </ModalFooter>
      </ModalContent>
    </Modal>
  )
}
