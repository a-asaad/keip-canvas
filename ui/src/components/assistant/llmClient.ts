import { Ollama } from "@langchain/community/llms/ollama"
import { PromptTemplate } from "langchain/prompts"

const llm = new Ollama({
  baseUrl: "http://localhost:11434",
  model: "llama2",
  maxRetries: 3,
})

const promptTemplate = PromptTemplate.fromTemplate(
  "Answer in once sentence. {userInput}"
)

const chain = promptTemplate.pipe(llm)

export const promptModel = async (userInput: string) => {
  return await chain.invoke({ userInput: userInput })
}
