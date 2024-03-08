package com.octo.keip.schema.xml;

import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.EipSchema;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SchemaToModelTest {

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
}
