import {
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
  Connection,
  EdgeChange,
  NodeChange,
  NodeRemoveChange,
} from "reactflow"
import { DYNAMIC_EDGE_TYPE } from "../../api/flow"
import { EipComponent } from "../../api/generated/eipComponentDef"
import {
  CHANNEL_ATTR_NAME,
  DYNAMIC_ROUTING_CHILDREN,
  lookupEipComponent,
} from "../eipDefinitions"
import { AppStore } from "./api"
import { useAppStore } from "./appStore"

export const onNodesChange = (changes: NodeChange[]) =>
  useAppStore.setState((state) => {
    const updates: Partial<AppStore> = {
      nodes: applyNodeChanges(changes, state.nodes),
    }

    const updatedEipConfigs = removeDeletedNodeConfigs(state, changes)
    if (updatedEipConfigs) {
      updates.eipNodeConfigs = updatedEipConfigs
    }

    return updates
  })

// TODO: Use the new 'selectable` DefaultEdgeProp after migrating to ReactFlow v12.
// Only dynamic router edges should be selectable.
export const onEdgesChange = (changes: EdgeChange[]) =>
  useAppStore.setState((state) => ({
    edges: applyEdgeChanges(changes, state.edges),
  }))

export const onConnect = (connection: Connection) =>
  useAppStore.setState((state) => {
    const sourceNode = state.nodes.find((n) => n.id === connection.source)
    const sourceComponent =
      sourceNode && lookupEipComponent(sourceNode.data.eipId)
    const edge =
      sourceComponent?.connectionType === "content_based_router"
        ? createDynamicRoutingEdge(connection, sourceComponent)
        : connection
    return {
      edges: addEdge(edge, state.edges),
    }
  })

const removeDeletedNodeConfigs = (state: AppStore, changes: NodeChange[]) => {
  const deletes: NodeRemoveChange[] = changes.filter((c) => c.type === "remove")

  if (deletes.length === 0) {
    return null
  }

  const updatedConfigs = { ...state.eipNodeConfigs }
  deletes.forEach((c) => delete updatedConfigs[c.id])
  return updatedConfigs
}

// TODO: Refactor
const createDynamicRoutingEdge = (
  connection: Connection,
  sourceComponent: EipComponent
) => {
  const channelMapper = sourceComponent?.childGroup?.children.find((child) =>
    DYNAMIC_ROUTING_CHILDREN.has(child.name)
  )
  if (!channelMapper) {
    throw new Error(
      `source component (${sourceComponent.name}) does not have a recognized channel mapping child`
    )
  }

  const matcher = channelMapper.attributes?.find(
    (attr) => attr.name !== CHANNEL_ATTR_NAME
  )
  if (!matcher) {
    throw new Error(
      `Channel mapping component (${channelMapper.name}) does not have a recognized value matcher attribute`
    )
  }

  // TODO: Avoid storing matcher attribute descriptions in the store.
  return {
    ...connection,
    type: DYNAMIC_EDGE_TYPE,
    data: {
      mapping: {
        mapperName: channelMapper.name,
        matcher,
      },
    },
    animated: true,
  }
}
