import { Button, TableToolbar, TableToolbarContent } from "@carbon/react"
import { useState } from "react"

const AssistantChatPanel = () => {
  const [isOpen, setOpen] = useState(false)

  const display = isOpen ? { height: '20vh' } : {height: '2rem'}

  // TODO: Can probably ditch Carbon's Toolbar
  return (
    <div className="chat-panel" style={display}>
      <TableToolbar size="sm">
        <TableToolbarContent className="chat-toolbar">
          <Button
            kind="secondary"
            size="sm"
            onClick={() => setOpen((prev) => !prev)}
          >
            Chat
          </Button>
        </TableToolbarContent>
      </TableToolbar>
      {isOpen && <div>Chat Window</div>}
    </div>
  )
}

export default AssistantChatPanel
