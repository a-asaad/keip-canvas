package com.octo.keip.schema.model.serdes

import com.google.gson.JsonParser
import com.octo.keip.schema.model.eip.Attribute
import com.octo.keip.schema.model.eip.AttributeType
import com.octo.keip.schema.model.eip.ChildGroup
import com.octo.keip.schema.model.eip.EipChildElement
import com.octo.keip.schema.model.eip.EipComponent
import com.octo.keip.schema.model.eip.EipSchema
import com.octo.keip.schema.model.eip.FlowType
import com.octo.keip.schema.model.eip.Indicator
import com.octo.keip.schema.model.eip.Occurrence
import com.octo.keip.schema.model.eip.Restriction
import com.octo.keip.schema.model.eip.Role
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class SchemaSerializerTest extends Specification {

    static final EXPECTED_SCHEMA_JSON = importEipSchemaJson()

    @TempDir
    File testDir

    def "Serialize EipSchema to JSON String"() {
        given:
        def attr = new Attribute.Builder("test-attr", AttributeType.STRING)
                .description("test attr description")
                .defaultValue("default val")
                .required(false)
                .restriction(new Restriction.MultiValuedRestriction(Restriction.RestrictionType.ENUM, List.of("first", "second")))
                .build()
        def child = new EipChildElement.Builder("test-child").description("test child description").build()
        def eipComponent = new EipComponent.Builder("test-top", Role.ENDPOINT, FlowType.SOURCE)
                .description("test top level description")
                .addAttribute(attr)
                .childGroup(new ChildGroup(Indicator.SEQUENCE, List.of(child)))
                .build()
        def eipSchema = EipSchema.from(Map.of("test-ns", List.of(eipComponent)))
        def outputFile = new File(testDir, "schema.json")

        when:
        SchemaSerializer.writeSchemaToJsonFile(eipSchema, outputFile)

        then:
        JsonParser.parseString(outputFile.text) == JsonParser.parseString(EXPECTED_SCHEMA_JSON)
    }

    def "Test custom occurrence deserializer"(Occurrence input, String expectedJson) {
        when:
        def result = SchemaSerializer.GSON.toJson(input)

        then:
        JsonParser.parseString(result) == JsonParser.parseString(expectedJson)

        where:
        input                                   | expectedJson
        Occurrence.DEFAULT                      | ""
        new Occurrence(0, 1)                    | "{min: 0}"
        new Occurrence(1, 3)                    | "{max: 3}"
        new Occurrence(5, 10)                   | "{min: 5, max: 10}"
        new Occurrence(0, Occurrence.UNBOUNDED) | "{min: 0, max: -1}"
    }

    private static String importEipSchemaJson() throws URISyntaxException, IOException {
        Path path = Path.of("schemas", "json", "serialization-sanity-check.json")
        return SchemaSerializerTest.getClassLoader().getResource(path.toString()).text
    }

}
