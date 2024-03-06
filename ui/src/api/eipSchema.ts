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

export interface EipChildren {
  indicator: "all" | "choice" | "sequence"
  elements: EipElement[]
}

export interface EipElement {
  name: string
  description?: string
  attributes?: Attribute[]
  children?: EipChildren
}

export interface EipComponent extends EipElement {
  role: Role
  flowType: FlowType
}

export type EipSchema = Record<string, EipComponent[]>
