package com.octo.keip.schema.xml

import com.octo.keip.schema.model.eip.FlowType
import com.octo.keip.schema.model.eip.Role
import com.octo.keip.schema.xml.attribute.AnnotationTranslator
import com.octo.keip.schema.xml.attribute.AttributeTranslator
import org.apache.ws.commons.schema.XmlSchema
import org.apache.ws.commons.schema.XmlSchemaElement
import org.apache.ws.commons.schema.XmlSchemaSequence
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo
import spock.lang.Specification

class EipTranslationVisitorOldTest extends Specification {

    static final DESCRIPTION = "test-description"

    static final TOP_LEVEL_ELEMENT_NAME = "test-top-level-name"

    def visitor = new EipTranslationVisitor(new AttributeTranslator(), mockAnnotationTranslator(DESCRIPTION))

    def topLevelElement = new XmlSchemaElement(new XmlSchema("test-ns", null), true)

    def typeInfo = new XmlSchemaTypeInfo(false)

    void setup() {
        topLevelElement.setName(TOP_LEVEL_ELEMENT_NAME)
    }

    def "OnEnterElement with top level element sets the main EipComponent"() {
        when:
        visitor.onEnterElement(topLevelElement, typeInfo, false)
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getName() == TOP_LEVEL_ELEMENT_NAME
        eipComponent.getDescription() == DESCRIPTION
        eipComponent.getRole() == Role.ENDPOINT
        eipComponent.getFlowType() == FlowType.SOURCE
    }

    def "OnEnterElement with multiple top level element visits throws an error"() {
        given:
        visitor.onEnterElement(topLevelElement, typeInfo, false)

        when:
        visitor.onEnterElement(topLevelElement, typeInfo, false)

        then:
        thrown(IllegalStateException)
    }

    def "OnEnterElement with previously unseen child element"() {
        given:
        def xmlElement = new XmlSchemaElement(topLevelElement.getParent(), false)
        xmlElement.setName("test-child")

        def xmlGroup = new XmlSchemaSequence()
        xmlGroup.getItems().add(xmlElement)
        visitor.onEnterElement(topLevelElement, typeInfo, false)
        visitor.onEnterSequenceGroup(xmlGroup)
        when:
        visitor.onEnterElement(xmlElement, typeInfo, false)
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getChildGroup().children().size() == 1
    }

    private AnnotationTranslator mockAnnotationTranslator(String description) {
        def mockTranslator = Mock(AnnotationTranslator)
        mockTranslator.getDescription(_) >> description
        return mockTranslator
    }
}
