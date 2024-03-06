import { Content, Header, HeaderName } from "@carbon/react"

import "./styles.scss"

import FlowCanvas from "./components/canvas/FlowCanvas"
import EipConfigSidePanel from "./components/config-panel/EipConfigSidePanel"
import NodeChooserPanel from "./components/draggable-panel/NodeChooserPanel"

const App = () => (
  <>
    <Header aria-label="Canvas Title" className="header-main">
      <HeaderName prefix="" className="header-main-text">
        Keip Canvas
      </HeaderName>
    </Header>

    <NodeChooserPanel />

    <Content className="canvas">
      <FlowCanvas />
    </Content>

    <EipConfigSidePanel />
  </>
)

export default App
