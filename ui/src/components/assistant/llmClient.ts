import { Ollama } from "@langchain/community/llms/ollama"
import { Edge } from "reactflow"
import { EipFlowNode } from "../../api/flow"
import { getLayoutedNodes } from "./nodeLayouting"
import { partialPrompt } from "./prompt"

interface ModelFlowResponse {
  nodes: EipFlowNode[]
  edges: Edge[]
  eipNodeConfigs: Record<string, object>
}

const llm = new Ollama({
  baseUrl: "http://localhost:11434",
  maxRetries: 3,
  model: "mistral",
  format: "json",
  numCtx: 4096,
  // temperature: 0,
})

const chain = partialPrompt.pipe(llm)

export const promptModel = async (
  userInput: string,
  streamCallback: (chunk: string) => void
) => {
  const responseStream = await chain.stream({
    userInput: userInput,
  })

  let rawResponse = ""
  for await (const chunk of responseStream) {
    rawResponse += chunk
    streamCallback(chunk)
  }

  return responseParser(rawResponse)
}

// TODO: Use Langchain custom output parser
// TODO: Return an object rather than a JSON string
const responseParser = (jsonResponse: string) => {
  const response = JSON.parse(jsonResponse) as ModelFlowResponse
  if (!response.nodes) {
    throw new Error("No nodes provided in model response: " + jsonResponse)
  }

  if (!response.edges) {
    if (response.nodes.length == 1) {
      response.edges = []
    } else {
      throw new Error("No edges provided in model response: " + jsonResponse)
    }
  }

  if (!response.eipNodeConfigs) {
    response.eipNodeConfigs = {}
  }

  response.nodes = getLayoutedNodes(response.nodes, response.edges)

  return JSON.stringify(response)
}
