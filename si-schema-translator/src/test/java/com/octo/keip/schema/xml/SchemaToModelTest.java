package com.octo.keip.schema.xml;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.EipSchema;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
//    sampleEipSchema = importEipSchema("eipSample.json");
  }

  @Test
  void fullyTranslateSchemaSuccess() throws IOException {
    Reader schemaReader = getSchemaFileReader("sample.xml");
    var translator = new SiSchemaTranslator();
    EipSchema eipSchema = translator.apply("test", schemaReader);
    Assertions.assertTrue(true);
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

    var gson = new Gson();
    Map<String, List<EipComponent>> eipSchemaMap = gson.fromJson(schemaJson, eipSchemaMapType);
    return EipSchema.from(eipSchemaMap);
  }
}
