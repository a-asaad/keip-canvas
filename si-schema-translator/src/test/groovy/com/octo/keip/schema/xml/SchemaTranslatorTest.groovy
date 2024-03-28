package com.octo.keip.schema.xml

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.octo.keip.schema.model.eip.Attribute
import com.octo.keip.schema.model.eip.ChildComposite
import com.octo.keip.schema.model.eip.ChildGroup
import com.octo.keip.schema.model.eip.EipChildElement
import com.octo.keip.schema.model.eip.EipComponent
import com.octo.keip.schema.model.eip.EipElement
import com.octo.keip.schema.model.eip.Indicator
import com.octo.keip.schema.model.eip.Occurrence
import com.octo.keip.schema.model.eip.Restriction
import com.octo.keip.schema.serdes.ChildCompositeDeserializer
import com.octo.keip.schema.serdes.OccurrenceDeserializer
import com.octo.keip.schema.serdes.RestrictionDeserializer
import org.apache.ws.commons.schema.XmlSchemaCollection
import spock.lang.Specification

import java.nio.file.Path
import java.util.function.BiConsumer

class SchemaTranslatorTest extends Specification {

    static final List<EipComponent> EIP_COMPONENTS = importEipSchema("translatedEipSample.json")

    static final Set<String> EXCLUDED_COMPONENTS = ["ignored-component"]

    def schemaTranslator = new SchemaTranslator(EXCLUDED_COMPONENTS)

    def schemaCollection = new XmlSchemaCollection()

    def "Check end-to-end XML schema to EIP JSON translation success"() {
        given:
        def targetSchema = schemaCollection.read(getSchemaFileReader(Path.of("schema-translator-sample.xml")))
        schemaCollection.read(getSchemaFileReader(Path.of("dependencies", "spring-tool.xsd")))

        when:
        List<EipComponent> result = schemaTranslator.translate(schemaCollection, targetSchema)

        then:
        assertCollectionsEqualNoOrder(
                EIP_COMPONENTS,
                result,
                Comparator.comparing(EipElement::getName),
                this::assertEipComponentsEqual,
                "Comparing top level components")
    }

    def "Check EIP translation schema with nested top level child groups (sequence, choice, etc.) not allowed"() {
        given:
        def noOpReducer = Mock(ChildGroupReducer)
        noOpReducer.reduceGroup(_) >> { ChildGroup group -> group }
        schemaTranslator.setGroupReducer(noOpReducer)

        def targetSchema = schemaCollection.read(getSchemaFileReader(Path.of("schema-translator-sample.xml")))

        when:
        schemaTranslator.translate(schemaCollection, targetSchema)

        then:
        thrown(IllegalArgumentException)
    }

    def "Exception thrown during component translation skips the component"() {
        given:
        def faultyReducer = Mock(ChildGroupReducer)
        faultyReducer.reduceGroup(_) >> { throw new RuntimeException("broken reducer") } >> new ChildGroup(Indicator.SEQUENCE, Occurrence.DEFAULT)
        schemaTranslator.setGroupReducer(faultyReducer)

        def targetSchema = schemaCollection.read(getSchemaFileReader(Path.of("schema-translator-sample.xml")))

        when:
        List<EipComponent> result = schemaTranslator.translate(schemaCollection, targetSchema)

        then:
        result.size() == 1
        result.getFirst().getName() == "sample-filter"
    }

    private static void assertCollectionsEqualNoOrder(
            Collection expected,
            Collection actual,
            Comparator comparator,
            BiConsumer assertion,
            String message) {
        if (expected == null && actual == null) {
            return
        }

        assert expected.size() == actual.size(): message

        def expectedSort = expected.sort(false, comparator)
        def actualSort = actual.sort(false, comparator)

        for (i in 0..<expected.size()) {
            assertion.accept(expectedSort[i], actualSort[i])
        }
    }

    private void assertEipComponentsEqual(EipComponent expected, EipComponent actual) {
        verifyAll {
            expected.getName() == actual.getName()
            expected.getRole() == actual.getRole()
            expected.getFlowType() == actual.getFlowType()
            expected.getDescription() == actual.getDescription()
            assertCollectionsEqualNoOrder(
                    expected.getAttributes(),
                    actual.getAttributes(),
                    Comparator.comparing(Attribute::name),
                    { exp, act -> assert exp == act },
                    String.format(
                            "Comparing EIP Component Attributes (component: %s)", expected.getName()))
            assertEipChildGroupsEqual(expected.getChildGroup(), actual.getChildGroup())
        }
    }

    private void assertEipChildGroupsEqual(ChildComposite expected, ChildComposite actual) {
        // TODO: Alternatives to null guard clause
        if (expected == null && actual == null) {
            return
        }

        assert expected instanceof ChildGroup
        assert actual instanceof ChildGroup

        ChildGroup expectedGroup = expected
        ChildGroup actualGroup = actual

        List<EipChildElement> expectedChildren = expectedGroup.children() as List<EipChildElement>

        List<EipChildElement> actualChildren = actualGroup.children() as List<EipChildElement>

        verifyAll {
            expectedGroup.indicator() == actualGroup.indicator()
            expectedGroup.occurrence() == actual.occurrence()
            assertCollectionsEqualNoOrder(
                    expectedChildren,
                    actualChildren,
                    Comparator.comparing(EipChildElement::getName),
                    this::assertEipChildElementsEqual,
                    "Comparing Child Groups")
        }
    }


    private void assertEipChildElementsEqual(EipChildElement expected, EipChildElement actual) {
        verifyAll {
            expected.getName() == actual.getName()
            expected.getDescription() == actual.getDescription()
            expected.occurrence() == actual.occurrence()
            assertCollectionsEqualNoOrder(
                    expected.getAttributes(),
                    actual.getAttributes(),
                    Comparator.comparing(Attribute::name),
                    { exp, act -> assert exp == act },
                    String.format("Comparing child elements (child-name: %s)", expected.getName()))
            assertEipChildGroupsEqual(expected.getChildGroup(), actual.getChildGroup())
        }
    }

    private static BufferedReader getSchemaFileReader(Path filepath) {
        String path = Path.of("schemas", "xml", filepath.toString()).toString()
        return SchemaTranslatorTest.class.getClassLoader().getResource(path).newReader()
    }

    private static List<EipComponent> importEipSchema(String jsonFilename)
            throws URISyntaxException, IOException {
        String schemaJson = SchemaTranslatorTest.getClassLoader().getResource(Path.of("schemas", "json", jsonFilename).toString()).text

        Gson gson = configureGson()
        def eipComponentListType = new TypeToken<List<EipComponent>>() {}
        return gson.fromJson(schemaJson, eipComponentListType)
    }

    private static Gson configureGson() {
        var gson = new GsonBuilder()
        gson.registerTypeAdapter(Restriction.class, new RestrictionDeserializer())
        gson.registerTypeAdapter(ChildComposite.class, new ChildCompositeDeserializer())
        gson.registerTypeAdapter(Occurrence.class, new OccurrenceDeserializer())
        return gson.create()
    }
}
