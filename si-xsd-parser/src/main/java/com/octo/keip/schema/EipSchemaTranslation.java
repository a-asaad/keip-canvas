package com.octo.keip.schema;

import com.octo.keip.schema.client.XmlSchemaClient;
import com.octo.keip.schema.config.XsdSourceConfiguration;
import com.octo.keip.schema.config.XsdSourceConfiguration.SchemaIdentifier;
import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.EipSchema;
import com.octo.keip.schema.xml.SchemaTranslator;
import java.util.List;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EipSchemaTranslation {

  private static final Logger LOGGER = LoggerFactory.getLogger(EipSchemaTranslation.class);

  private final XmlSchemaClient xmlSchemaClient;

  private final EipSchema eipSchema;

  public EipSchemaTranslation(
          XsdSourceConfiguration sourceConfiguration, XmlSchemaClient xmlSchemaClient) {
    this.xmlSchemaClient = xmlSchemaClient;
    this.eipSchema = translate(sourceConfiguration);
  }

  public EipSchema getEipSchema() {
    return eipSchema;
  }

  private EipSchema translate(XsdSourceConfiguration sourceConfiguration) {
    EipSchema translatedSchema = new EipSchema();

    for (SchemaIdentifier targetSchema : sourceConfiguration.getSchemas()) {
      try {
        XmlSchemaCollection schemaCollection = xmlSchemaClient.collect(targetSchema.getLocation());

        var translator = new SchemaTranslator(targetSchema.getExcludedElements());

        List<EipComponent> components =
            translator.translate(
                schemaCollection, schemaCollection.schemaForNamespace(targetSchema.getNamespace()));

        translatedSchema.addComponents(targetSchema.getAlias(), components);
      } catch (Exception e) {
        LOGGER.error("Failed to translate schema: {}", targetSchema.getNamespace(), e);
      }
    }

    return translatedSchema;
  }
}
