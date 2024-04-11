import { OverflowMenuItem } from "@carbon/react"
import { OverflowMenuItemProps } from "@carbon/react/lib/components/OverflowMenuItem/OverflowMenuItem"
import { layer01 } from "@carbon/themes"
import { toPng } from "html-to-image"
import { forwardRef } from "react"
import { getNodesBounds, getViewportForBounds } from "reactflow"
import { useGetNodes } from "../../singletons/store"

const IMAGE_WIDTH = 1024
const IMAGE_HEIGHT = 768

const downloadImage = (dataUrl: string) => {
  const a = document.createElement("a")
  a.setAttribute("download", "flowDiagram.png")
  a.setAttribute("href", dataUrl)
  a.click()
  a.remove()
}

const ExportPng = forwardRef<HTMLElement>(
  (props: OverflowMenuItemProps, ref) => {
    const nodes = useGetNodes()

    const handleClick = () => {
      const nodeBounds = getNodesBounds(nodes)
      const transform = getViewportForBounds(
        nodeBounds,
        IMAGE_WIDTH,
        IMAGE_HEIGHT,
        0.5,
        2
      )

      toPng(document.querySelector(".react-flow__viewport")!, {
        backgroundColor: layer01 as string,
        width: IMAGE_WIDTH,
        height: IMAGE_HEIGHT,
        style: {
          width: IMAGE_WIDTH.toString(),
          height: IMAGE_WIDTH.toString(),
          transform: `translate(${transform.x}px, ${transform.y}px) scale(${transform.zoom})`,
        },
      })
        .then(downloadImage)
        .catch((err) => console.error("Failed to convert diagram to PNG", err))
    }

    return (
      <OverflowMenuItem
        {...props}
        ref={ref}
        itemText="Export image"
        onClick={handleClick}
      />
    )
  }
)

ExportPng.displayName = "ExportPng"

export default ExportPng
