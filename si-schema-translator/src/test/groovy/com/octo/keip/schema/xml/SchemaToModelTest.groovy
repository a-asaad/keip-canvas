package com.octo.keip.schema.xml

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.octo.keip.schema.model.eip.*
import com.octo.keip.schema.serdes.ChildCompositeDeserializer
import com.octo.keip.schema.serdes.RestrictionDeserializer
import spock.lang.Specification

import java.util.function.BiConsumer

class SchemaToModelTest extends Specification {

    private static eipSchemaMapType = new TypeToken<Map<String, List<EipComponent>>>() {}

    private static Reader testXmlReader
    private static EipSchema sampleEipSchema

    void setupSpec() {
        testXmlReader = getSchemaFileReader("sample.xml")
//        sampleEipSchema = importEipSchema("eipSample.json")
        sampleEipSchema = importEipSchema("/tmp/minimal-schema.json")
    }

    def "Check fully translated EIP JSON Schema"() {
        given:
        def translator = new SiSchemaTranslator();
        when:
        EipSchema resultSchema = translator.apply("test-namespace", testXmlReader);
        then:
        assertSchemasEqual(sampleEipSchema, resultSchema);
    }

    private void assertSchemasEqual(EipSchema expected, EipSchema actual) {
        def actualMap = actual.toMap();
        expected
                .toMap()
                .each { namespace, expectedComponents ->
                    def actualComponents = actualMap[namespace]
                    assertCollectionsEqualNoOrder(
                            expectedComponents,
                            actualComponents,
                            Comparator.comparing(EipElement::getName),
                            this::assertEipComponentsEqual,
                            "Comparing top level components")
                }
    }

    // TODO: Simplify
    private static void assertCollectionsEqualNoOrder(
            Collection expected,
            Collection actual,
            Comparator comparator,
            BiConsumer assertion,
            String message) {
        if (expected == null && actual == null) {
            return;
        }

        assert expected.size() == actual.size(): message

        def expectedSort = expected.sort(false, comparator)
        def actualSort = actual.sort(false, comparator)

        for (i in 0..<expected.size()) {
            assertion.accept(expectedSort[i], actualSort[i]);
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
                    { a, b -> assert a == b },
                    String.format(
                            "Comparing EIP Component Attributes (component: %s)", expected.getName()))
            assertEipChildGroupsEqual(expected.getChildGroup(), actual.getChildGroup())
        }
    }

    private void assertEipChildGroupsEqual(ChildComposite expected, ChildComposite actual) {
        // TODO: Alternatives to null guard clause
        if (expected == null && actual == null) {
            return;
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
                    { a, b -> assert a == b },
                    String.format("Comparing child elements (child-name: %s)", expected.getName()))
            assertEipChildGroupsEqual(expected.getChildGroup(), actual.getChildGroup())
        }
    }

    private static BufferedReader getSchemaFileReader(String filename) {
        return new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(
                                SchemaToModelTest.class
                                        .getClassLoader()
                                        .getResourceAsStream("schemas/" + filename))));
    }

    private static EipSchema importEipSchema(String jsonFilename)
            throws URISyntaxException, IOException {
        String schemaJson = SchemaToModelTest.getClassLoader().getResource("schemas/${jsonFilename}").text

        Gson gson = configureGson();
        Map<String, List<EipComponent>> eipSchemaMap = gson.fromJson(schemaJson, eipSchemaMapType);
        return EipSchema.from(eipSchemaMap);
    }

    private static Gson configureGson() {
        var gson = new GsonBuilder();
        gson.registerTypeAdapter(Restriction.class, new RestrictionDeserializer());
        gson.registerTypeAdapter(ChildComposite.class, new ChildCompositeDeserializer());
        return gson.create();
    }
}
