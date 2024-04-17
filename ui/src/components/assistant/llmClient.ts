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

interface PromptResponse {
  data: string
  success: boolean
  cause?: Error | "aborted"
}

class LlmClient {
  private llm
  private chain
  private abortCtrl

  constructor() {
    this.llm = new Ollama({
      baseUrl: "http://localhost:11434",
      maxRetries: 3,
      model: "mistral",
      format: "json",
      numCtx: 4096,
      temperature: 0,
    })

    this.chain = partialPrompt.pipe(this.llm)
    this.abortCtrl = new AbortController()
  }

  public async prompt(
    userInput: string,
    streamCallback: (chunk: string) => void
  ): Promise<PromptResponse> {
    let rawResponse = ""
    try {
      const responseStream = await this.chain.stream({
        userInput: userInput,
      })

      for await (const chunk of responseStream) {
        if (this.abortCtrl.signal.aborted) {
          this.abortCtrl = new AbortController()
          return { data: rawResponse, success: false, cause: "aborted" }
        }

        rawResponse += chunk
        streamCallback(chunk)
      }

      return { data: this.parseResponse(rawResponse), success: true }
    } catch (err) {
      console.error(err)
      return {
        data: rawResponse,
        success: false,
        cause: err as Error,
      }
    }
  }

  public abort(): void {
    this.abortCtrl.abort()
  }

  // TODO: Use Langchain custom output parser
  // TODO: Return an object rather than a JSON string
  private parseResponse(jsonResponse: string): string {
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
}

export default LlmClient
