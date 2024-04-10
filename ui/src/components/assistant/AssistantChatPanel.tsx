import {
  Button,
  IconButton,
  InlineLoading,
  Stack,
  TableToolbar,
  TableToolbarContent,
  TextArea,
  Tile,
} from "@carbon/react"
import { Send } from "@carbon/react/icons"
import { useState } from "react"
import { promptModel } from "./llmClient"

interface ChatHistoryProps {
  entries: string[]
}

interface ChatInputProps {
  handleInput: (input: string) => Promise<void>
}

const ChatInput = ({ handleInput }: ChatInputProps) => {
  const [content, setContent] = useState("")
  const [isWaiting, setWaiting] = useState(false)

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      if (content.length > 0) {
        setWaiting(true)
        handleInput(content)
          .then(() => setContent(""))
          .catch((err) => console.error(err))
          .finally(() => setWaiting(false))
      }
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
        disabled={isWaiting}
      />

      {isWaiting ? (
        <InlineLoading className="chat-input-waiting" status="active" />
      ) : (
        <IconButton label="send" size="md" disabled={content.length === 0}>
          <Send />
        </IconButton>
      )}
    </div>
  )
}

const ChatHistory = ({ entries }: ChatHistoryProps) => {
  return (
    <Tile className="chat-history">
      <Stack gap={5}>
        {entries.map((entry, idx) => (
          <span key={idx} className="chat-history-entry">
            <p>{entry}</p>
          </span>
        ))}
      </Stack>
    </Tile>
  )
}

const AssistantChatPanel = () => {
  const [isOpen, setOpen] = useState(false)
  const [chatEntries, setChatEntries] = useState<string[]>([])

  const sendPrompt = async (input: string) => {
    const response = await promptModel(input)
    console.log(response)
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
          <ChatInput handleInput={sendPrompt} />
        </>
      )}
    </div>
  )
}

export default AssistantChatPanel
