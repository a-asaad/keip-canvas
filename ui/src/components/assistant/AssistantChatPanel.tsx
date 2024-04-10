import {
  Button,
  IconButton,
  Stack,
  TableToolbar,
  TableToolbarContent,
  TextArea,
  Tile,
} from "@carbon/react"
import { Send } from "@carbon/react/icons"
import { useState } from "react"

interface ChatHistoryProps {
  entries: string[]
}

interface ChatInputProps {
  handleInput: (input: string) => void
}

const ChatInput = ({ handleInput }: ChatInputProps) => {
  const [content, setContent] = useState("")
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleInput(content)
      setContent("")
    }
  }
  return (
    <div className="chat-input-container">
      <TextArea
        id="chat-input"
        labelText="chat-input"
        hideLabel
        rows={1}
        value={content}
        onChange={(e) => setContent(e.target.value)}
        onKeyDown={handleKeyDown}
      />
      <IconButton label="send" size="md">
        <Send />
      </IconButton>
    </div>
  )
}

const ChatHistory = ({ entries }: ChatHistoryProps) => {
  return (
    <Tile className="chat-history">
      <Stack gap={5}>
        {entries.map((entry, idx) => (
          <span key={idx} className="chat-history-entry">
            {entry}
          </span>
        ))}
      </Stack>
    </Tile>
  )
}

const AssistantChatPanel = () => {
  const [isOpen, setOpen] = useState(false)
  const [chatEntries, setChatEntries] = useState<string[]>([])

  const addEntry = (input: string) => {
    setChatEntries((prev) => [...prev, input])
  }

  const display = isOpen ? { height: "30vh" } : { height: "2rem" }

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

      {isOpen && (
        <>
          <ChatHistory entries={chatEntries} />
          <ChatInput handleInput={addEntry} />
        </>
      )}
    </div>
  )
}

export default AssistantChatPanel
