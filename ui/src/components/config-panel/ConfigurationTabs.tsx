import { Stack, Tab, TabList, TabPanel, TabPanels, Tabs } from "@carbon/react"

interface ConfigurationTabProps {
  hasAttributes: boolean
  hasChildren: boolean
  attributesForm: React.ReactNode
  childrenForm: React.ReactNode
}

const ConfigurationInputTabs = ({
  hasAttributes,
  hasChildren,
  attributesForm,
  childrenForm,
}: ConfigurationTabProps) => {
  return (
    <Stack gap={4}>
      <Tabs>
        <TabList aria-label="Attributes/children toggle" contained fullWidth>
          {hasAttributes && <Tab>Attributes</Tab>}
          {hasChildren && <Tab>Children</Tab>}
        </TabList>
        <TabPanels>
          {hasAttributes && (
            <TabPanel className="side-panel-unpadded-container">
              {attributesForm}
            </TabPanel>
          )}
          {hasChildren && <TabPanel>{childrenForm}</TabPanel>}
        </TabPanels>
      </Tabs>
    </Stack>
  )
}

export default ConfigurationInputTabs
