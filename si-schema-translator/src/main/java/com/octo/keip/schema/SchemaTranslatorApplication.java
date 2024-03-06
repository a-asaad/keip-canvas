package com.octo.keip.schema;

import com.octo.keip.schema.xml.XmlSchemaParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

// TODO: Rename packages
public class SchemaTranslatorApplication {
  public static void main(String[] args) {
    try (var inStream =
            SchemaTranslatorApplication.class
                .getClassLoader()
                .getResourceAsStream("tmp/spring-integration-5.2.xsd");
        var reader = new BufferedReader(new InputStreamReader(inStream))) {
      var p = new XmlSchemaParser();
      p.parse(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
