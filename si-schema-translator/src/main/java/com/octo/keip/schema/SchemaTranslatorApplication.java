package com.octo.keip.schema;

import com.octo.keip.schema.client.XmlSchemaHttpClient;
import com.octo.keip.schema.config.XmlSchemaSourceConfiguration;
import java.io.InputStream;

// TODO: Rename packages
public class SchemaTranslatorApplication {
  public static void main(String[] args) {
    XmlSchemaSourceConfiguration config = XmlSchemaSourceConfiguration.readYaml(getYaml());
    var schemaClient = new XmlSchemaHttpClient(config.getImportedSchemaLocationsMap());
    var translation = new EipSchemaTranslation(config, schemaClient);
  }

  private static InputStream getYaml() {
    return SchemaTranslatorApplication.class
        .getClassLoader()
        .getResourceAsStream("schemaConfig.yaml");
  }
}
