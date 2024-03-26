package com.octo.keip.schema.xml

import com.octo.keip.schema.model.eip.Attribute
import com.octo.keip.schema.model.eip.AttributeType
import com.octo.keip.schema.model.eip.ChildGroup
import com.octo.keip.schema.model.eip.EipChildElement
import com.octo.keip.schema.model.eip.FlowType
import com.octo.keip.schema.model.eip.Indicator
import com.octo.keip.schema.model.eip.Occurrence
import com.octo.keip.schema.model.eip.Role
import org.apache.ws.commons.schema.XmlSchemaCollection
import org.apache.ws.commons.schema.XmlSchemaElement
import org.apache.ws.commons.schema.walker.XmlSchemaWalker
import spock.lang.Specification

import java.nio.file.Path

class EipTranslationVisitorTest extends Specification {

    static final TEST_XML_NAMESPACE = "test-ns"

    def xmlSchemaCollection = new XmlSchemaCollection()

    def visitor = new EipTranslationVisitor()

    XmlSchemaWalker walker = setupWalker()

    def "Top level element is set as the main EipComponent"() {
        when:
        walker.walk(getTopLevelComponent("top-level-component"))
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getName() == "top-level-component"
        eipComponent.getDescription() == "Top Level EIP Component"
        eipComponent.getRole() == Role.ENDPOINT
        eipComponent.getFlowType() == FlowType.SOURCE

        def expectedAttrs = [new Attribute.Builder("top-attr-1", AttributeType.NUMBER).defaultValue("1").build(), new Attribute.Builder("top-attr-2", AttributeType.STRING).required(true).build()] as HashSet
        eipComponent.attributes == expectedAttrs
    }

    def "Visit multiple top level elements without resetting visitor throws exception"() {
        when:
        walker.walk(getTopLevelComponent("top-level-component"))
        walker.walk(getTopLevelComponent("top-level-component"))

        then:
        thrown(IllegalStateException)
    }

    def "Top level element test single ChildGroup"() {
        when:
        walker.walk(getTopLevelComponent("top-level-component"))
        def eipComponent = visitor.getEipComponent()

        then:
        def group = eipComponent.getChildGroup() as ChildGroup
        group.indicator() == Indicator.SEQUENCE
        group.occurrence() == new Occurrence(0, Occurrence.UNBOUNDED)
        group.children().size() == 1
    }

    def "Visit with previously unseen child element"() {
        when:
        walker.walk(getTopLevelComponent("top-level-component"))
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getName() == "top-level-component"
        def group = eipComponent.getChildGroup() as ChildGroup
        group.children().size() == 1

        def childElement = group.children().getFirst() as EipChildElement
        childElement.getName() == "childElement1"
        childElement.occurrence() == new Occurrence(2, 4)
        childElement.getDescription() == "baseType example docs"

        def expectedAttrs = [new Attribute.Builder("child-attr-1", AttributeType.BOOLEAN).description("Enable thing").build()] as HashSet
        childElement.attributes == expectedAttrs
    }

    def "Visit with previously visited child element"() {
        when:
        walker.walk(getTopLevelComponent("top-level-component"))
        visitor.reset()
        walker.walk(getTopLevelComponent("alt-top-level-component"))
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getName() == "alt-top-level-component"
        def group = eipComponent.getChildGroup() as ChildGroup
        group.children().size() == 1

        def childElement = group.children().getFirst() as EipChildElement
        childElement.getName() == "altChildElement1"
        childElement.occurrence() == Occurrence.DEFAULT
        childElement.getDescription() == "baseType example docs"

        def expectedAttrs = [new Attribute.Builder("child-attr-1", AttributeType.BOOLEAN).description("Enable thing").build()] as HashSet
        childElement.attributes == expectedAttrs
    }

    def "Visit handle nested children"() {
        when:
        walker.walk(getTopLevelComponent("top-level-component"))
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getName() == "top-level-component"
        def group = eipComponent.getChildGroup() as ChildGroup
        group.children().size() == 1

        def childElement = group.children().getFirst() as EipChildElement
        childElement.getName() == "childElement1"

        def nestedGroup = childElement.getChildGroup() as ChildGroup
        nestedGroup.indicator() == Indicator.SEQUENCE
        nestedGroup.occurrence() == Occurrence.DEFAULT

        nestedGroup.children().size() == 2

        def firstGroup = nestedGroup.children().getFirst() as ChildGroup
        firstGroup.indicator() == Indicator.CHOICE
        firstGroup.occurrence() == new Occurrence(1, 3)
        firstGroup.children().size() == 1
        (firstGroup.children().getFirst() as EipChildElement).getName() == "nestedChild1"

        def secondGroup = nestedGroup.children().getLast() as ChildGroup
        secondGroup.indicator() == Indicator.SEQUENCE
        secondGroup.occurrence() == new Occurrence(0, 1)
        secondGroup.children().size() == 1
        (secondGroup.children().getFirst() as EipChildElement).getName() == "nestedChild2"
    }

    def "Visit component with an ALL group"() {
        when:
        walker.walk(getTopLevelComponent("component-with-all-group"))
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getName() == "component-with-all-group"
        def group = eipComponent.getChildGroup() as ChildGroup
        group.indicator() == Indicator.ALL
        group.children().size() == 2
    }

    private XmlSchemaWalker setupWalker() {
        xmlSchemaCollection.read(getSchemaFileReader("sample.xml"))
        def walker = new XmlSchemaWalker(xmlSchemaCollection)
        walker.addVisitor(visitor)
        return walker
    }

    private XmlSchemaElement getTopLevelComponent(String name) {
        return xmlSchemaCollection.schemaForNamespace(TEST_XML_NAMESPACE).getElementByName(name)
    }

    private static BufferedReader getSchemaFileReader(String filename) {
        String path = Path.of("schemas", "eip-visitor-samples", filename).toString()
        return EipTranslationVisitorTest.class.getClassLoader().getResource(path).newReader()
    }
}