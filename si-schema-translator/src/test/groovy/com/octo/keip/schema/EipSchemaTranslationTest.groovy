package com.octo.keip.schema

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.octo.keip.schema.client.XmlSchemaClient
import com.octo.keip.schema.config.XmlSchemaSourceConfiguration
import com.octo.keip.schema.model.eip.EipComponent
import com.octo.keip.schema.model.eip.EipElement
import com.octo.keip.schema.model.eip.EipSchema
import com.octo.keip.schema.test.EipComparisonUtils
import com.octo.keip.schema.test.TestIOUtils
import org.apache.ws.commons.schema.XmlSchemaCollection
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path

class EipSchemaTranslationTest extends Specification {

    @Shared
            expectedEipMap = importEipSchema(Path.of("end-to-end-eip-schema.json")).toMap()

    @Shared
            schemaSourceConfig = XmlSchemaSourceConfiguration.readYaml(TestIOUtils.getYamlConfig(Path.of("end-to-end-config.yaml")))

    def "Use XmlSchemaSourceConfiguration to translate XML to EIP schema"() {
        given:
        def schemaCollectionFirst = new XmlSchemaCollection()
        schemaCollectionFirst.read(TestIOUtils.getXmlSchemaFileReader(Path.of("end-to-end", "ns1.xml")))

        def schemaCollectionSecond = new XmlSchemaCollection()
        schemaCollectionSecond.read(TestIOUtils.getXmlSchemaFileReader(Path.of("end-to-end", "ns2.xml")))

        def schemaClient = Mock(XmlSchemaClient)
        schemaClient.collect(_, _) >> { args -> args[0].equals("http://www.example.com/schema/ns1") ? schemaCollectionFirst : schemaCollectionSecond }

        when:
        def eipTranslation = new EipSchemaTranslation(schemaSourceConfig, schemaClient)
        def resultMap = eipTranslation.getEipSchema().toMap();

        then:
        resultMap.size() == 2
        EipComparisonUtils.assertCollectionsEqualNoOrder(resultMap["ns1"], expectedEipMap["ns1"], Comparator.comparing(EipElement::getName), EipComparisonUtils::assertEipComponentsEqual, "Comparing ns1 components")
        EipComparisonUtils.assertCollectionsEqualNoOrder(resultMap["ns2"], expectedEipMap["ns2"], Comparator.comparing(EipElement::getName), EipComparisonUtils::assertEipComponentsEqual, "Comparing ns2 components")
    }

    def "If translating a schema throws an exception, move on to next one"() {
        given:
        def schemaCollectionSecond = new XmlSchemaCollection()
        schemaCollectionSecond.read(TestIOUtils.getXmlSchemaFileReader(Path.of("end-to-end", "ns2.xml")))

        def schemaClient = Mock(XmlSchemaClient)
        schemaClient.collect(_, _) >> { args ->
            {
                if (args[0].equals("http://www.example.com/schema/ns1")) {
                    throw new RuntimeException("schema fail")
                }
                return schemaCollectionSecond
            }
        }

        when:
        def eipTranslation = new EipSchemaTranslation(schemaSourceConfig, schemaClient)
        def resultMap = eipTranslation.getEipSchema().toMap();

        then:
        resultMap.size() == 1
        EipComparisonUtils.assertCollectionsEqualNoOrder(resultMap["ns2"], expectedEipMap["ns2"], Comparator.comparing(EipElement::getName), EipComparisonUtils::assertEipComponentsEqual, "Comparing ns2 components")
    }

    private static EipSchema importEipSchema(Path jsonFilePath) throws URISyntaxException, IOException {
        Path path = Path.of("schemas", "json").resolve(jsonFilePath)
        String schemaJson = EipSchemaTranslationTest.getClassLoader().getResource(path.toString()).text

        Gson gson = TestIOUtils.configureGson()
        def eipComponentListType = new TypeToken<Map<String, List<EipComponent>>>() {}
        return EipSchema.from(gson.fromJson(schemaJson, eipComponentListType))
    }
}
