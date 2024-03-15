package com.octo.keip.schema.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.octo.keip.schema.model.eip.Attribute;
import com.octo.keip.schema.model.eip.ChildComposite;
import com.octo.keip.schema.model.eip.ChildGroup;
import com.octo.keip.schema.model.eip.EipChildElement;
import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.EipElement;
import com.octo.keip.schema.model.eip.EipSchema;
import com.octo.keip.schema.model.eip.Restriction;
import com.octo.keip.schema.serdes.ChildCompositeDeserializer;
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

  private static Reader testXmlReader;
  private static EipSchema sampleEipSchema;

  @BeforeAll
  static void beforeAll() throws URISyntaxException, IOException {
    testXmlReader = getSchemaFileReader("sample.xml");
    //    sampleEipSchema = importEipSchema("eipSample.json");
    sampleEipSchema = importEipSchema("minimal-schema.json");
  }

  @Test
  void fullyTranslateSchemaSuccess() {
    var translator = new SiSchemaTranslator();
    EipSchema resultSchema = translator.apply("test-namespace", testXmlReader);
    assertSchemasEqual(sampleEipSchema, resultSchema);
  }

  private void assertSchemasEqual(EipSchema expected, EipSchema actual) {
    Map<String, List<EipComponent>> actualMap = actual.toMap();
    expected
        .toMap()
        .forEach(
            (namespace, expectedComponents) -> {
              List<EipComponent> actualComponents = actualMap.get(namespace);
              assertCollectionsEqualNoOrder(
                  expectedComponents,
                  actualComponents,
                  Comparator.comparing(EipElement::getName),
                  this::assertEipComponentsEqual);
            });
  }

  // TODO: Simplify
  private <T> void assertCollectionsEqualNoOrder(
      Collection<T> expected,
      Collection<T> actual,
      Comparator<T> comparator,
      BiConsumer<T, T> assertion) {
    if (expected == null && actual == null) {
      return;
    }

    assertEquals(expected.size(), actual.size());

    var expectedSort = expected.stream().sorted(comparator).toList();
    var actualSort = actual.stream().sorted(comparator).toList();

    for (int i = 0; i < expectedSort.size(); i++) {
      assertion.accept(expectedSort.get(i), actualSort.get(i));
    }
  }

  private void assertEipComponentsEqual(EipComponent expected, EipComponent actual) {
    Assertions.assertAll(
        () -> assertEquals(expected.getName(), actual.getName()),
        () -> assertEquals(expected.getRole(), actual.getRole()),
        () -> assertEquals(expected.getFlowType(), actual.getFlowType()),
        () -> assertEquals(expected.getDescription(), actual.getDescription()),
        () ->
            assertCollectionsEqualNoOrder(
                expected.getAttributes(),
                actual.getAttributes(),
                Comparator.comparing(Attribute::name),
                Assertions::assertEquals),
        () -> assertEipChildGroupsEqual(expected.getChildGroup(), actual.getChildGroup()));
  }

  // TODO: Refactor once ChildComposite is updated or removed from model.
  private void assertEipChildGroupsEqual(ChildGroup expected, ChildGroup actual) {
    // TODO: Alternatives to null guard clause
    if (expected == null && actual == null) {
      return;
    }

    List<EipChildElement> expectedChildren =
        expected.children().stream().map(EipChildElement.class::cast).toList();

    List<EipChildElement> actualChildren =
        actual.children().stream().map(EipChildElement.class::cast).toList();

    Assertions.assertAll(
        () -> assertEquals(expected.indicator(), actual.indicator()),
        () -> assertEquals(expected.occurrence(), actual.occurrence()),
        () ->
            assertCollectionsEqualNoOrder(
                expectedChildren,
                actualChildren,
                Comparator.comparing(EipChildElement::getName),
                this::assertEipChildElementEqual));
  }

  private void assertEipChildElementEqual(EipChildElement expected, EipChildElement actual) {
    Assertions.assertAll(
        () -> assertEquals(expected.getName(), actual.getName()),
        () -> assertEquals(expected.getDescription(), actual.getDescription()),
        () -> assertEquals(expected.occurrence(), actual.occurrence()),
        () ->
            assertCollectionsEqualNoOrder(
                expected.getAttributes(),
                actual.getAttributes(),
                Comparator.comparing(Attribute::name),
                Assertions::assertEquals),
        () -> assertEipChildGroupsEqual(expected.getChildGroup(), actual.getChildGroup()));
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
    gson.registerTypeAdapter(ChildComposite.class, new ChildCompositeDeserializer());
    return gson.create();
  }
}
