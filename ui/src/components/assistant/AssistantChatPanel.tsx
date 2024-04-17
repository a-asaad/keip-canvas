import { magenta50 } from "@carbon/colors"
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
import { CloseOutline, Send } from "@carbon/react/icons"
import { interactive } from "@carbon/themes"
import { useState } from "react"
import { useAppActions } from "../../singletons/store"
import { promptModel } from "./llmClient"

type ChatEntrySource = "user" | "AI"

interface ChatEntry {
  message: string
  source: ChatEntrySource
}

interface ChatHistoryProps {
  entries: ChatEntry[]
  streamingResponse?: string
}

interface ChatInputProps {
  handleInput: (input: string) => Promise<void>
}

const ChatInput = ({ handleInput }: ChatInputProps) => {
  const [content, setContent] = useState("")
  const [isWaiting, setWaiting] = useState(false)

  const submit = () => {
    if (content.length > 0) {
      setWaiting(true)
      handleInput(content)
        .then(() => setContent(""))
        .catch((err) => console.error(err))
        .finally(() => setWaiting(false))
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      submit()
    }
  }

  return (
    <div className="chat-input-container">
      <TextArea
        id="chat-input"
        labelText="chat-input"
        hideLabel
        placeholder="Enter Prompt..."
        rows={1}
        value={content}
        onChange={(e) => setContent(e.target.value)}
        onKeyDown={handleKeyDown}
        disabled={isWaiting}
      />

      {isWaiting ? (
        <>
          <InlineLoading className="chat-input-waiting" status="active" />
          <Button
            style={{ paddingBlockStart: "11px" }}
            size="lg"
            kind="danger--tertiary"
            hasIconOnly
            iconDescription="cancel"
            renderIcon={() => <CloseOutline size={24} />}
          />
        </>
      ) : (
        <IconButton
          label="submit"
          size="lg"
          disabled={content.length === 0}
          onClick={submit}
        >
          <Send />
        </IconButton>
      )}
    </div>
  )
}

const getEntryColor = (source: ChatEntrySource) => {
  return { borderColor: (source === "AI" ? magenta50 : interactive) as string }
}

const ChatHistory = ({ entries, streamingResponse }: ChatHistoryProps) => {
  const allEntries = streamingResponse
    ? [...entries, { message: streamingResponse, source: "AI" } as ChatEntry]
    : entries

  return (
    <Tile className="chat-history">
      <Stack gap={5}>
        {allEntries.map((entry, idx) => (
          <span
            key={idx}
            className="chat-history-entry"
            style={getEntryColor(entry.source)}
          >
            <p>{entry.message}</p>
          </span>
        ))}
      </Stack>
    </Tile>
  )
}

const AssistantChatPanel = () => {
  // TODO: Create a new action to set nodes/edges directly from object instead of JSON string.
  const { importFlowFromJson } = useAppActions()
  const [isOpen, setOpen] = useState(false)
  const [chatEntries, setChatEntries] = useState<ChatEntry[]>([])
  const [activeResponse, setActiveResponse] = useState("")

  const updateResponseChunk = (chunk: string) =>
    setActiveResponse((prev) => prev + chunk)

  const sendPrompt = async (input: string) => {
    try {
      const response = await promptModel(input, updateResponseChunk)
      importFlowFromJson(response)
      setChatEntries((prev) => [
        ...prev,
        { message: input, source: "user" },
        { message: response, source: "AI" },
      ])
    } finally {
      setActiveResponse("")
    }
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

      {/* TODO: Ensure request is still processing if panel is collapsed */}
      {/* TODO: Display an error pop-up if LLM prompt fails  */}
      {isOpen && (
        <>
          <ChatHistory
            entries={chatEntries}
            streamingResponse={activeResponse}
          />
          <ChatInput handleInput={sendPrompt} />
        </>
      )}
    </div>
  )
}

export default AssistantChatPanel
