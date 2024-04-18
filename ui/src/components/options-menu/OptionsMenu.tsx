// @ts-expect-error: OverflowMenu is not expoerted properly from Carbon React
import { OverflowMenu } from "@carbon/react"
import { Menu } from "@carbon/react/icons"
import ExportPng from "./ExportPng"
import ExportXml from "./ExportXml"
import SaveDiagram from "./SaveDiagram"

const OptionsMenu = () => {
  return (
    <OverflowMenu
      className="options-menu"
      iconClass="options-menu-icon"
      aria-label="options-menu"
      iconDescription="options"
      align="bottom-right"
      renderIcon={() => <Menu size={24} />}
      size="lg"
      flipped
    >
      {/* OverflowMenu does not play nice when OverflowMenuItems are not direct children. Custom components will need to use forwardRef to avoid errors. */}
      <SaveDiagram />
      <ExportPng />
      <ExportXml />
    </OverflowMenu>
  )
}

export default OptionsMenu
