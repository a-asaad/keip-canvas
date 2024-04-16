import { Ollama } from "@langchain/community/llms/ollama"
import { PromptTemplate } from "langchain/prompts"
import { Edge } from "reactflow"
import { EipFlowNode } from "../../api/flow"
import { EIP_SCHEMA } from "../../singletons/eipDefinitions"
import { getLayoutedNodes } from "./nodeLayouting"

interface ModelFlowResponse {
  nodes: EipFlowNode[]
  edges: Edge[]
  eipNodeConfigs: Record<string, object>
}

const responseJsonExample = `{
  "nodes": [
    {
      "id": "n1",
      "type": "eipNode",
      "data": {
        "eipId": {
          "namespace": "integration",
          "name": "inbound-channel-adapter"
        }
      }
    },
    {
        "id": "n2",
        "type": "eipNode",
        "data": {
          "eipId": {
            "namespace": "http",
            "name": "outbound-gateway"
          }
        }
      }
  ],
  "edges": [
    {
      "source": "n1",
      "target": "n2",
      "id": "edge_n1_n2"
    }
  ]
}
`

// TODO: Do this async instead?
const eipIds = Object.entries(EIP_SCHEMA).reduce((acc, curr) => {
  const [namespace, components] = curr
  return { ...acc, [namespace]: components.map((c) => c.name) }
}, {})

const eipIdsJson = JSON.stringify(eipIds)

const llm = new Ollama({
  baseUrl: "http://localhost:11434",
  maxRetries: 3,
  model: "mistral",
  format: "json",
  numCtx: 4096,
  // temperature: 0,
})

const promptTemplate = PromptTemplate.fromTemplate(
  `The response MUST only be a JSON that matches the following flow schema:
  START_FLOW_SCHEMA
  {responseFormat}
  END_FLOW_SCHEMA

  each node's data.eipId field corresponds to a Spring Integration component and MUST come from the following mapping of namespace to component name:

  START_EIP_COMPONENT_MAPPING
  {eipIds}
  END_EIP_COMPONENT_MAPPING

  Avoid adding any explicit channels to the flow

  Taking the above into account, respond to the following request:
  {userInput}`
)

const chain = promptTemplate.pipe(llm)

export const promptModel = async (userInput: string) => {
  const rawResponse = await chain.invoke({
    responseFormat: responseJsonExample,
    eipIds: eipIdsJson,
    userInput: userInput,
  })
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
