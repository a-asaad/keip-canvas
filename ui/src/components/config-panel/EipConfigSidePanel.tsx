import { HeaderPanel } from "@carbon/react"
import { useState } from "react"
import { Edge, useOnSelectionChange, useStoreApi } from "reactflow"
import { DYNAMIC_EDGE_TYPE, DynamicEdge, EipFlowNode } from "../../api/flow"
import {
  Attribute,
  EipChildElement,
  EipComponent,
} from "../../api/generated/eipComponentDef"
import { EipId } from "../../api/generated/eipFlow"
import {
  FLOW_CONTROLLED_ATTRIBUTES,
  lookupContentBasedRouterKeys,
  lookupEipComponent,
} from "../../singletons/eipDefinitions"
import { clearSelectedChildNode } from "../../singletons/store/appActions"
import { useGetSelectedChildNode } from "../../singletons/store/getterHooks"
import { getEipId } from "../../singletons/store/storeViews"
import DynamicEdgeConfig from "./DynamicEdgeConfig"
import { ChildNodeConfig, RootNodeConfig } from "./NodeConfigPanel"

const isDynamicRouterAttribute = (attribute: Attribute, eipId?: EipId) => {
  if (!eipId) {
    return false
  }

  const keyDef = lookupContentBasedRouterKeys(eipId)
  if (!keyDef) {
    return false
  }

  return keyDef.type === "attribute" && keyDef.name === attribute.name
}

const filterConfigurableAttributes = (attrs?: Attribute[], eipId?: EipId) => {
  return attrs
    ? attrs.filter(
        (attr) =>
          !FLOW_CONTROLLED_ATTRIBUTES.has(attr.name) &&
          !isDynamicRouterAttribute(attr, eipId)
      )
    : []
}

const isDynamicEdge = (edge: Edge) => edge?.type === DYNAMIC_EDGE_TYPE

// TODO: Should this be a utility method in the EipComponentDef module?
// TODO: Handle invalid path error case
const findChildDefinition = (
  rootComponent: EipComponent,
  childPath: string[]
) => {
  let children = rootComponent.childGroup?.children
  let child: EipChildElement | null = null

  for (const id of childPath.slice(1)) {
    const name = getEipId(id)?.name
    child = children?.find((c) => c.name === name) ?? null
    children = child?.childGroup?.children
  }

  return child
}

// TODO: Add breadcrumb menu at the top, showing the path to child.
const EipConfigSidePanel = () => {
  const reactFlowStore = useStoreApi()
  const [selectedNode, setSelectedNode] = useState<EipFlowNode | null>(null)
  const [selectedEdge, setSelectedEdge] = useState<DynamicEdge | null>(null)
  const selectedChildPath = useGetSelectedChildNode()

  useOnSelectionChange({
    onChange: ({ nodes, edges }) => {
      selectedChildPath && clearSelectedChildNode()
      const numSelected = nodes.length + edges.length
      setSelectedNode(numSelected === 1 ? nodes[0] : null)
      setSelectedEdge(
        numSelected === 1 && isDynamicEdge(edges[0]) ? edges[0] : null
      )
    },
  })

  const selectedNodeEipId = selectedNode && getEipId(selectedNode.id)
  const eipComponent = selectedNodeEipId
    ? lookupEipComponent(selectedNodeEipId)
    : null

  let sidePanelContent
  // TODO: Simplify conditionals
  if (selectedChildPath && selectedNode && eipComponent) {
    const childId = selectedChildPath[selectedChildPath.length - 1]

    // TODO: Handle error case if childElement is undefined
    const childElement = findChildDefinition(eipComponent, selectedChildPath)
    const configurableAttrs = filterConfigurableAttributes(
      childElement?.attributes
    )
    sidePanelContent = (
      <ChildNodeConfig
        key={childId}
        childPath={selectedChildPath}
        attributes={configurableAttrs}
      />
    )
  } else if (selectedNodeEipId && eipComponent) {
    const configurableAttrs = filterConfigurableAttributes(
      eipComponent.attributes,
      selectedNodeEipId
    )
    sidePanelContent = (
      <RootNodeConfig
        key={selectedNode.id}
        node={selectedNode}
        attributes={configurableAttrs}
      />
    )
  } else if (selectedEdge) {
    const { nodeInternals } = reactFlowStore.getState()
    const sourceNode = nodeInternals.get(selectedEdge.source)
    const targetNode = nodeInternals.get(selectedEdge.target)

    sidePanelContent = (
      <DynamicEdgeConfig
        key={selectedEdge.id}
        edge={{ ...selectedEdge, sourceNode, targetNode }}
      />
    )
  } else {
    // Returning an empty fragment because the HeaderPanel component spams
    // the logs with error messages if it doesn't have any children.
    sidePanelContent = <></>
  }

  const showPanel = Boolean(selectedNode || selectedChildPath || selectedEdge)

  return (
    <HeaderPanel
      className={showPanel ? "node-config-panel" : ""}
      expanded={showPanel}
    >
      {sidePanelContent}
    </HeaderPanel>
  )
}

export default EipConfigSidePanel
