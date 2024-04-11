import {
  Column,
  Grid,
  Header,
  HeaderGlobalBar,
  HeaderName,
} from "@carbon/react"

import "./styles.scss"

import AssistantChatPanel from "./components/assistant/AssistantChatPanel"
import FlowCanvas from "./components/canvas/FlowCanvas"
import EipConfigSidePanel from "./components/config-panel/EipConfigSidePanel"
import NodeChooserPanel from "./components/draggable-panel/NodeChooserPanel"
import OptionsMenu from "./components/options-menu/OptionsMenu"

const App = () => (
  <>
    <Grid className="main-layout-grid">
      <Column span="100%">
        <Header aria-label="Canvas Title" className="header-main">
          <HeaderName prefix="" className="header-main-text">
            Keip Canvas
          </HeaderName>
          <HeaderGlobalBar className="header-action-bar">
            <OptionsMenu />
          </HeaderGlobalBar>
        </Header>
      </Column>

      <Column sm={1} md={2} lg={3}>
        <NodeChooserPanel />
      </Column>

      <Column sm={3} md={6} lg={13}>
        <FlowCanvas />
      </Column>

      <Column span="100%">
        <AssistantChatPanel />
      </Column>
    </Grid>

    <EipConfigSidePanel />
  </>
)

export default App
