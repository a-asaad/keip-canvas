import { OverflowMenuItem } from "@carbon/react"
import { OverflowMenuItemProps } from "@carbon/react/lib/components/OverflowMenuItem/OverflowMenuItem"
import { forwardRef } from "react"
import { useSerializedStore } from "../../singletons/store"

const downloadFile = (json: string) => {
  const blob = new Blob([json], { type: "application/json" })
  const url = URL.createObjectURL(blob)

  const a = document.createElement("a")
  a.href = url
  a.download = "eipFlowDiagram.json"
  a.click()
  URL.revokeObjectURL(url)
}

const SaveDiagram = forwardRef<HTMLElement>(
  (props: OverflowMenuItemProps, ref) => {
    const storeJson = useSerializedStore()

    return (
      <OverflowMenuItem
        {...props}
        ref={ref}
        itemText="Save"
        onClick={() => downloadFile(storeJson)}
      />
    )
  }
)

SaveDiagram.displayName = "SaveDiagram"

export default SaveDiagram
