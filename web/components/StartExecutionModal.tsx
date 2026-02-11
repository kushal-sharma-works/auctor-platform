import { useState } from "react"
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
import { useWorkflows } from "../hooks/useGraphql"
import { useStartExecution } from "../hooks/useExecution"

interface StartExecutionModalProps {
  isOpen: boolean
  onClose: () => void
}

export function StartExecutionModal({ isOpen, onClose }: StartExecutionModalProps) {
  const router = useRouter()
  const toast = useToast()
  const queryClient = useQueryClient()
  const { data: workflowData } = useWorkflows(0, 100)
  const startExecutionMutation = useStartExecution()

  const [selectedWorkflow, setSelectedWorkflow] = useState<string>("")
  const [selectedVersion, setSelectedVersion] = useState<number>(1)
  const [inputs, setInputs] = useState<Record<string, string>>({})
  const [inputKey, setInputKey] = useState<string>("")
  const [inputValue, setInputValue] = useState<string>("")

  const workflows = (workflowData?.workflows?.content || []) as Workflow[]

  const selectedWorkflowObj = workflows.find((w: Workflow) => w.id === selectedWorkflow)

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
        title: "Error",
        description: error instanceof Error ? error.message : "Failed to start execution",
        status: "error",
        duration: 3000,
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
                Keys must match policy fields (case-sensitive). Example: Delivery=5, Item=New.
              </Text>
              <VStack align="stretch" spacing={2}>
                {/* Input Key-Value Adder */}
                <HStack spacing={2}>
                  <Input
                    placeholder="Key"
                    value={inputKey}
                    onChange={(e) => setInputKey(e.target.value)}
                    size="sm"
                  />
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
