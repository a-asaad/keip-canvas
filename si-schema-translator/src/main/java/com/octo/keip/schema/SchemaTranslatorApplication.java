package com.octo.keip.schema;

import com.octo.keip.schema.config.SchemaRetrievalConfiguration;
import com.octo.keip.schema.config.SchemaRetrievalConfiguration.SchemaIdentifier;
import com.octo.keip.schema.http.XmlSchemaHttpClient;
import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.EipSchema;
import com.octo.keip.schema.xml.SchemaTranslator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.ws.commons.schema.XmlSchemaCollection;

// TODO: Rename packages
public class SchemaTranslatorApplication {
  public static void main(String[] args) {
    SchemaRetrievalConfiguration config = SchemaRetrievalConfiguration.readYaml(getYaml());

    EipSchema eipSchema = new EipSchema();

    for (SchemaIdentifier targetSchema : config.getSchemas()) {
      Map<String, URI> schemaLocations =
          targetSchema.getImportedSchemas().stream()
              .collect(
                  Collectors.toMap(SchemaIdentifier::getNamespace, SchemaIdentifier::getLocation));
      schemaLocations.put(targetSchema.getNamespace(), targetSchema.getLocation());
      var httpClient = new XmlSchemaHttpClient(schemaLocations);
      try {
        XmlSchemaCollection schemaCollection = httpClient.collect(targetSchema.getNamespace());
        var translator = new SchemaTranslator(targetSchema.getExcludedElements());
        List<EipComponent> components =
            translator.translate(
                schemaCollection, schemaCollection.schemaForNamespace(targetSchema.getNamespace()));
        eipSchema.addComponents(targetSchema.getAlias(), components);
      } catch (IOException | InterruptedException e) {
        // TODO: Log instead
        throw new RuntimeException(e);
      }
    }

    //    try (Writer writer = new FileWriter("/tmp/eipSchema.json")) {
    //      new Gson().toJson(eipSchema, writer);
    //    } catch (IOException e) {
    //      throw new RuntimeException(e);
    //    }

    var i = 4;
  }

  private static InputStream getYaml() {
    return SchemaTranslatorApplication.class
        .getClassLoader()
        .getResourceAsStream("schemaConfig.yaml");
  }
}
