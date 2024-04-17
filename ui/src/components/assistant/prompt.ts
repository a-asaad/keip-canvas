import { PromptTemplate } from "@langchain/core/prompts"
import { EipId } from "../../api/id"
import { EIP_SCHEMA } from "../../singletons/eipDefinitions"

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
  const ids = components.map((c) => ({ namespace: namespace, name: c.name }))
  return [...acc, ...ids]
}, [] as EipId[])

const eipIdsJson = JSON.stringify(eipIds)

const promptTemplate = PromptTemplate.fromTemplate(
  `The response MUST be a JSON that matches the following flow schema:
START_FLOW_SCHEMA
\`\`\`json
{responseFormat}
\`\`\`
END_FLOW_SCHEMA

each node's data.eipId field corresponds to a Spring Integration component and MUST be chosen from the following list of eipIds:

START_EIP_COMPONENT_LIST
{eipIds}
END_EIP_COMPONENT_LIST

Avoid adding any explicit channels to the flow

Taking the above into account, respond to the following request:
{userInput}`
)

const partialPrompt = await promptTemplate.partial({
  responseFormat: responseJsonExample,
  eipIds: eipIdsJson,
})

export { partialPrompt }
