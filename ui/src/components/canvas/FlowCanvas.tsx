import ReactFlow, {
  Background,
  BackgroundVariant,
  Controls,
  useReactFlow,
} from "reactflow"

import { useAppActions, useFlowStore } from "../../singletons/store"

import "reactflow/dist/base.css"

import { useDrop } from "react-dnd"
import { EipId } from "../../api/id"
import { DragTypes } from "../draggable-panel/dragTypes"
import EipNode from "./EipNode"

const nodeTypes = {
  eipNode: EipNode,
}

const FlowCanvas = () => {
  const reactFlowInstance = useReactFlow()
  const flowStore = useFlowStore()
  const { createDroppedNode, clearSelectedChildNode } = useAppActions()

  const [, drop] = useDrop<EipId, unknown, unknown>(
    () => ({
      accept: DragTypes.FLOWNODE,
      drop: (eipId, monitor) => {
        let offset = monitor.getClientOffset()
        offset = offset ?? { x: 0, y: 0 }
        const pos = reactFlowInstance.screenToFlowPosition(offset)
        createDroppedNode(eipId, pos)
      },
    }),
    [reactFlowInstance]
  )

  // TODO: See if there is a better way to select and clear child nodes,
  // to avoid having to clear the selection in multiple components.

  return (
    <div style={{ width: "100%", height: "calc(100vh - 3rem)" }} ref={drop}>
      <ReactFlow
        nodes={flowStore.nodes}
        edges={flowStore.edges}
        onNodesChange={flowStore.onNodesChange}
        onEdgesChange={flowStore.onEdgesChange}
        onConnect={flowStore.onConnect}
        nodeTypes={nodeTypes}
        onPaneClick={() => clearSelectedChildNode()}
        fitView
      >
        <Controls />
        {/* <MiniMap /> */}
        <Background variant={BackgroundVariant.Dots} gap={12} size={1} />
      </ReactFlow>
    </div>
  )
}

export default FlowCanvas
