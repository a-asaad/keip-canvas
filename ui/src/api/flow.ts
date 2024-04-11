import { Node } from "reactflow"
import { EipId } from "./id"

export interface EipNodeData {
  eipId: EipId
  label: string
}

export type EipFlowNode = Node<EipNodeData>

export const EIP_NODE_KEY = "eipNode"
