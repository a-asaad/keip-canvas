import { create } from "zustand"
import { createJSONStorage, persist } from "zustand/middleware"

import { nanoid } from "nanoid/non-secure"
import {
  Connection,
  Edge,
  EdgeChange,
  NodeChange,
  NodeRemoveChange,
  OnConnect,
  OnEdgesChange,
  OnNodesChange,
  XYPosition,
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
} from "reactflow"
import { useShallow } from "zustand/react/shallow"
import { AttributeTypes } from "../api/eipSchema"
import { EIP_NODE_KEY, EipFlowNode } from "../api/flow"
import { ChildNodeId, EipId, areChildIdsEqual } from "../api/id"
import { lookupEipComponent } from "./eipDefinitions"

export const ROOT_PARENT = "root"

const NO_PERSIST = new Set(["appActions", "reactFlowActions"])

interface ReactFlowActions {
  onNodesChange: OnNodesChange
  onEdgesChange: OnEdgesChange
  onConnect: OnConnect
}

type AttributeMapping = Record<string, AttributeTypes>

interface EipNodeConfig {
  attributes: AttributeMapping
  children: Record<string, AttributeMapping>
}

interface AppActions {
  createDroppedNode: (eipId: EipId, position: XYPosition) => void

  updateNodeLabel: (nodeId: string, label: string) => void

  updateEipAttribute: (
    id: string,
    parentId: string,
    attrName: string,
    value: AttributeTypes
  ) => void

  updateEnabledChildren: (nodeId: string, children: string[]) => void

  updateSelectedChildNode: (childId: ChildNodeId) => void

  clearSelectedChildNode: () => void

  clearFlow: () => void
}

interface AppStore {
  nodes: EipFlowNode[]
  edges: Edge[]
  eipNodeConfigs: Record<string, EipNodeConfig>
  selectedChildNode: ChildNodeId | null

  reactFlowActions: ReactFlowActions
  appActions: AppActions
}

// If app becomes too slow, might need to switch to async storage.
const useStore = create<AppStore>()(
  persist(
    (set) => ({
      nodes: [],
      edges: [],
      eipNodeConfigs: {},
      selectedChildNode: null,

      reactFlowActions: {
        onNodesChange: (changes: NodeChange[]) =>
          set((state) => {
            const updates: Partial<AppStore> = {
              nodes: applyNodeChanges(changes, state.nodes),
            }

            const updatedEipConfigs = removeDeletedNodeConfigs(state, changes)
            if (updatedEipConfigs) {
              updates.eipNodeConfigs = updatedEipConfigs
            }

            return updates
          }),

        onEdgesChange: (changes: EdgeChange[]) =>
          set((state) => ({
            edges: applyEdgeChanges(changes, state.edges),
          })),

        onConnect: (connection: Connection) =>
          set((state) => ({
            edges: addEdge(connection, state.edges),
          })),
      },

      appActions: {
        createDroppedNode: (eipId, position) =>
          set((state) => {
            const node = newNode(eipId, position)
            return {
              nodes: [...state.nodes, node],
              eipNodeConfigs: {
                ...state.eipNodeConfigs,
                [node.id]: { attributes: {}, children: {} },
              },
            }
          }),
        updateNodeLabel: (id, label) =>
          set((state) => ({
            nodes: state.nodes.map((node) =>
              node.id === id ? { ...node, data: { ...node.data, label } } : node
            ),
          })),

        updateEipAttribute: (id, parentId, attrName, value) =>
          set((state) => {
            const configs = { ...state.eipNodeConfigs }
            if (parentId === ROOT_PARENT) {
              configs[id].attributes[attrName] = value
            } else {
              configs[parentId].children[id][attrName] = value
            }
            return { eipNodeConfigs: configs }
          }),

        updateEnabledChildren: (nodeId, children) =>
          set((state) => {
            const configs = { ...state.eipNodeConfigs }
            configs[nodeId].children = children.reduce((accum, child) => {
              accum[child] = {}
              return accum
            }, {} as Record<string, AttributeMapping>)

            return { eipNodeConfigs: configs }
          }),

        updateSelectedChildNode: (childId) =>
          set(() => ({ selectedChildNode: childId })),

        clearSelectedChildNode: () => set(() => ({ selectedChildNode: null })),

        clearFlow: () =>
          set(() => ({
            nodes: [],
            edges: [],
            eipNodeConfigs: {},
            selectedChildNode: null,
          })),
      },
    }),
    {
      name: "eipFlow",
      storage: createJSONStorage(() => localStorage),
      partialize: (state) =>
        Object.fromEntries(
          Object.entries(state).filter(([key]) => !NO_PERSIST.has(key))
        ),
    }
  )
)

const newNode = (eipId: EipId, position: XYPosition) => {
  const id = nanoid(10)
  const nodeSchema = lookupEipComponent(eipId)!
  const node: EipFlowNode = {
    id: id,
    type: EIP_NODE_KEY,
    position: position,
    data: {
      eipId: eipId,
      label: "New Node",
      flowType: nodeSchema.flowType,
      role: nodeSchema.role,
    },
  }
  return node
}

const removeDeletedNodeConfigs = (state: AppStore, changes: NodeChange[]) => {
  const deletes: NodeRemoveChange[] = changes.filter(
    (c) => c.type === "remove"
  ) as NodeRemoveChange[]

  if (deletes.length === 0) {
    return null
  }

  const updatedConfigs = { ...state.eipNodeConfigs }
  deletes.forEach((c) => delete updatedConfigs[c.id])
  return updatedConfigs
}

export const useNodeCount = () => useStore((state) => state.nodes.length)

export const useGetNodes = () => useStore((state) => state.nodes)

export const useGetEipAttribute = (
  id: string,
  parentId: string,
  attrName: string
) =>
  useStore((state) => {
    if (parentId === ROOT_PARENT) {
      return state.eipNodeConfigs[id]?.attributes[attrName]
    }
    return state.eipNodeConfigs[parentId]?.children[id][attrName]
  })

export const useGetChildren = (id: string) =>
  useStore(
    useShallow((state) =>
      state.eipNodeConfigs[id]
        ? Object.keys(state.eipNodeConfigs[id].children)
        : []
    )
  )

export const useGetSelectedChildNode = () =>
  useStore(useShallow((state) => state.selectedChildNode))

export const useIsChildSelected = (childId: ChildNodeId) =>
  useStore((state) => {
    if (state.selectedChildNode === null) {
      return false
    }
    return areChildIdsEqual(state.selectedChildNode, childId)
  })

export const useFlowStore = () =>
  useStore(
    useShallow((state: AppStore) => ({
      nodes: state.nodes,
      edges: state.edges,
      onNodesChange: state.reactFlowActions.onNodesChange,
      onEdgesChange: state.reactFlowActions.onEdgesChange,
      onConnect: state.reactFlowActions.onConnect,
    }))
  )

export const useAppActions = () => useStore((state) => state.appActions)
