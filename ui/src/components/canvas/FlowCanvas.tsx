import ReactFlow, {
  Background,
  BackgroundVariant,
  ControlButton,
  Controls,
  ReactFlowInstance,
  useReactFlow,
} from "reactflow"

import { useAppActions, useFlowStore } from "../../singletons/store"

import "reactflow/dist/base.css"

import { TrashCan } from "@carbon/icons-react"
import { DropTargetMonitor, useDrop } from "react-dnd"
import { NativeTypes } from "react-dnd-html5-backend"
import { EipId } from "../../api/id"
import { DragTypes } from "../draggable-panel/dragTypes"
import EipNode from "./EipNode"

interface FileDrop {
  files: File[]
}

type DropType = EipId | FileDrop

const acceptDroppedFile = (file: File, importFlow: (json: string) => void) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    e.target && importFlow(e.target.result as string)
  }
  reader.readAsText(file)
}

const getDropPosition = (
  monitor: DropTargetMonitor,
  reactFlowInstance: ReactFlowInstance
) => {
  let offset = monitor.getClientOffset()
  offset = offset ?? { x: 0, y: 0 }
  return reactFlowInstance.screenToFlowPosition(offset)
}

const nodeTypes = {
  eipNode: EipNode,
}

const FlowCanvas = () => {
  const reactFlowInstance = useReactFlow()
  const flowStore = useFlowStore()
  const {
    createDroppedNode,
    clearSelectedChildNode,
    clearFlow,
    importFlowFromJson,
  } = useAppActions()

  const [, drop] = useDrop(
    () => ({
      accept: [DragTypes.FLOWNODE, NativeTypes.FILE],
      drop: (item: DropType, monitor) => {
        if ("namespace" in item) {
          // Dropping a FLOWNODE creates a new node in the flow.
          const pos = getDropPosition(monitor, reactFlowInstance)
          createDroppedNode(item, pos)
        } else if ("files" in item) {
          // Dropping a JSON file imports it as a flow.
          acceptDroppedFile(item.files[0], importFlowFromJson)
        } else {
          console.warn("unknown drop type: ", item)
        }
      },
      canDrop: (item: DropType) => {
        if ("files" in item) {
          return (
            item.files.length == 1 && item.files[0].type == "application/json"
          )
        }
        return true
      },
    }),
    [reactFlowInstance]
  )

  // TODO: See if there is a better way to select and clear child nodes,
  // to avoid having to clear the selection in multiple components.

  return (
    <div className="canvas" ref={drop}>
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
        <Controls>
          <ControlButton title="clear" onClick={clearFlow}>
            <TrashCan />
          </ControlButton>
        </Controls>
        {/* <MiniMap /> */}
        <Background variant={BackgroundVariant.Dots} gap={12} size={1} />
      </ReactFlow>
    </div>
  )
}

export default FlowCanvas
