interface Restriction {
  enum?: string[]
}

export type AttributeTypes = string | boolean

export interface Attribute {
  name: string
  type: "string" | "boolean"
  description?: string
  default?: string | number | boolean
  required?: boolean
  restriction?: Restriction
}

export type FlowType = "source" | "sink" | "passthru"

export type Role = "endpoint" | "channel"

export interface EipChildGroup {
  indicator: "all" | "choice" | "sequence"
  elements: EipElement[]
}

interface EipElement {
  name: string
  description?: string
  attributes?: Attribute[]
  children?: EipChildGroup
}

interface Occurrence {
  min?: number
  max?: number | "unbounded"
}

export interface EipChildElement extends EipElement {
  occurrence?: Occurrence
}

export interface EipComponent extends EipElement {
  role: Role
  flowType: FlowType
}

export type EipSchema = Record<string, EipComponent[]>
