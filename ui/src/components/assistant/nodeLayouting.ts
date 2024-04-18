import dagre from "@dagrejs/dagre"
import { Edge, Node } from "reactflow"

const NODE_WIDTH = 128
const NODE_HEIGHT = 128

const graph = new dagre.graphlib.Graph()
graph.setDefaultEdgeLabel(() => ({}))

export const addLayout = (nodes: Node[], edges: Edge[]) => {
  graph.setGraph({ rankdir: "LR" })

  nodes.forEach((node) => {
    graph.setNode(node.id, { width: NODE_WIDTH, height: NODE_HEIGHT })
  })

  edges.forEach((edge) => graph.setEdge(edge.source, edge.target))

  dagre.layout(graph)

  nodes.forEach((node) => {
    const positionedNode = graph.node(node.id)

    // ReactFlow anchors the nodes at the top left rather than center,
    // an offset is added to compoensate.
    node.position = {
      x: positionedNode.x - NODE_WIDTH / 2,
      y: positionedNode.y - NODE_HEIGHT / 2,
    }

    return node
  })
}
