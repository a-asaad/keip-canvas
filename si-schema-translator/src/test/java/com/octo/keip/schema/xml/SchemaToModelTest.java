package com.octo.keip.schema.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.octo.keip.schema.model.eip.Attribute;
import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.EipElement;
import com.octo.keip.schema.model.eip.EipSchema;
import com.octo.keip.schema.model.eip.Restriction;
import com.octo.keip.schema.serdes.RestrictionDeserializer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SchemaToModelTest {

  private static final TypeToken<Map<String, List<EipComponent>>> eipSchemaMapType =
      new TypeToken<>() {};

  private static Reader sampleXml;
  private static EipSchema sampleEipSchema;

  @BeforeAll
  static void beforeAll() throws URISyntaxException, IOException {
    sampleXml = getSchemaFileReader("sample.xml");
    sampleEipSchema = importEipSchema("eipSample.json");
  }

  @Test
  void fullyTranslateSchemaSuccess() {
    Reader schemaReader = getSchemaFileReader("sample.xml");
    var translator = new SiSchemaTranslator();
    EipSchema resultSchema = translator.apply("test-namespace", schemaReader);
    assertSchemasEqual(sampleEipSchema, resultSchema);
  }

  private void assertSchemasEqual(EipSchema expected, EipSchema actual) {
    Map<String, List<EipComponent>> actualMap = actual.toMap();
    expected
        .toMap()
        .forEach(
            (namespace, expectedComponents) -> {
              List<EipComponent> actualComponents = actualMap.get(namespace);
              assertCompareCollections(
                  expectedComponents,
                  actualComponents,
                  Comparator.comparing(EipElement::getName),
                  this::assertComponentsEqual);
            });
  }

  // TODO: Simplify
  private <T> void assertCompareCollections(
      Collection<T> expected,
      Collection<T> actual,
      Comparator<T> comparator,
      BiConsumer<T, T> assertion) {
    assertEquals(expected.size(), actual.size());

    var expectedSort = expected.stream().sorted(comparator).toList();
    var actualSort = actual.stream().sorted(comparator).toList();

    for (int i = 0; i < expectedSort.size(); i++) {
      assertion.accept(expectedSort.get(i), actualSort.get(i));
    }
  }

  private void assertComponentsEqual(EipComponent expected, EipComponent actual) {
    Assertions.assertAll(
        () -> assertEquals(expected.getName(), actual.getName()),
        () -> assertEquals(expected.getRole(), actual.getRole()),
        () -> assertEquals(expected.getFlowType(), actual.getFlowType()),
        () -> assertEquals(expected.getDescription(), actual.getDescription()),
        () ->
            assertCompareCollections(
                expected.getAttributes(),
                actual.getAttributes(),
                Comparator.comparing(Attribute::name),
                Assertions::assertEquals),
        () -> assertEquals(expected.getChildGroup(), actual.getChildGroup()));
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
    String schemaJson =
        Files.readString(
            Paths.get(
                SchemaToModelTest.class
                    .getClassLoader()
                    .getResource("schemas/" + jsonFilename)
                    .toURI()));

    Gson gson = configureGson();
    Map<String, List<EipComponent>> eipSchemaMap = gson.fromJson(schemaJson, eipSchemaMapType);
    return EipSchema.from(eipSchemaMap);
  }

  private static Gson configureGson() {
    var gson = new GsonBuilder();
    gson.registerTypeAdapter(Restriction.class, new RestrictionDeserializer());
    return gson.create();
  }
}
