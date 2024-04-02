package com.octo.keip.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.octo.keip.schema.client.XmlSchemaClient;
import com.octo.keip.schema.config.XmlSchemaSourceConfiguration;
import com.octo.keip.schema.config.XmlSchemaSourceConfiguration.SchemaIdentifier;
import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.EipSchema;
import com.octo.keip.schema.model.eip.Occurrence;
import com.octo.keip.schema.model.serdes.OccurrenceSerializer;
import com.octo.keip.schema.xml.SchemaTranslator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EipSchemaTranslation {

  private static final Logger LOGGER = LoggerFactory.getLogger(EipSchemaTranslation.class);

  private static final Gson GSON = gsonBuilder().create();

  private static final Gson GSON_PRETTY = gsonBuilder().setPrettyPrinting().create();

  private final XmlSchemaClient xmlSchemaClient;

  private final EipSchema eipSchema;

  public EipSchemaTranslation(
      XmlSchemaSourceConfiguration sourceConfiguration, XmlSchemaClient xmlSchemaClient) {
    this.xmlSchemaClient = xmlSchemaClient;
    this.eipSchema = translate(sourceConfiguration);
  }

  public EipSchema getEipSchema() {
    return eipSchema;
  }

  public String schemaAsJson(boolean pretty) {
    if (pretty) {
      return GSON_PRETTY.toJson(eipSchema.toMap());
    }
    return GSON.toJson(eipSchema.toMap());
  }

  public void writeSchemaToJsonFile(File file) throws IOException {
    try (Writer writer = new FileWriter(file)) {
      GSON.toJson(eipSchema.toMap(), writer);
    }
  }

  private EipSchema translate(XmlSchemaSourceConfiguration sourceConfiguration) {
    EipSchema eipSchema = new EipSchema();

    for (SchemaIdentifier targetSchema : sourceConfiguration.getSchemas()) {
      try {
        XmlSchemaCollection schemaCollection =
            xmlSchemaClient.collect(targetSchema.getNamespace(), targetSchema.getLocation());

        var translator = new SchemaTranslator(targetSchema.getExcludedElements());

        List<EipComponent> components =
            translator.translate(
                schemaCollection, schemaCollection.schemaForNamespace(targetSchema.getNamespace()));

        eipSchema.addComponents(targetSchema.getAlias(), components);
      } catch (Exception e) {
        LOGGER.error("Failed to translate schema: {}", targetSchema.getNamespace(), e);
      }
    }

    return eipSchema;
  }

  private static GsonBuilder gsonBuilder() {
    return new GsonBuilder().registerTypeAdapter(Occurrence.class, new OccurrenceSerializer());
  }
}
