import ReactFlow, {
  Background,
  BackgroundVariant,
  Controls,
  useReactFlow,
} from "reactflow"

import { useAppActions, useFlowStore } from "../store"

import "reactflow/dist/base.css"

import { useDrop } from "react-dnd"
import EIPNode from "../custom-nodes/EIPNode"
import { DragTypes } from "../node-chooser/dragTypes"

export type FlowNodeData = {
  name: string
}

const nodeTypes = {
  eipNode: EIPNode,
}

const FlowCanvas = () => {
  const reactFlowInstance = useReactFlow()
  const flowStore = useFlowStore()
  const { createDroppedNode } = useAppActions()

  const [_, drop] = useDrop<FlowNodeData, unknown, unknown>(
    () => ({
      accept: DragTypes.FLOWNODE,
      drop: (item, monitor) => {
        let offset = monitor.getClientOffset()
        offset = offset === null ? { x: 0, y: 0 } : offset
        const pos = reactFlowInstance.screenToFlowPosition(offset)
        createDroppedNode(item.name, pos)
      },
    }),
    [reactFlowInstance]
  )

  return (
    <div style={{ width: "100%", height: "calc(100vh - 3rem)" }} ref={drop}>
      <ReactFlow
        nodes={flowStore.nodes}
        edges={flowStore.edges}
        onNodesChange={flowStore.onNodesChange}
        onEdgesChange={flowStore.onEdgesChange}
        onConnect={flowStore.onConnect}
        nodeTypes={nodeTypes}
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
