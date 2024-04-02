package com.octo.keip.schema.cmd;

import com.octo.keip.schema.EipSchemaTranslation;
import com.octo.keip.schema.client.XmlSchemaHttpClient;
import com.octo.keip.schema.config.XmlSchemaSourceConfiguration;
import com.octo.keip.schema.model.serdes.SchemaSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "translate",
    mixinStandardHelpOptions = true,
    version = "0.1.0",
    description =
        "Fetches and parses the Spring Integration XML schema files listed in the source configuration file, then translates them into an EIP Schema.")
public class TranslateCommand implements Callable<Integer> {

  @Option(
      names = {"-s", "--source"},
      required = true,
      description = "Path to XML schema source file")
  private File source;

  @Option(
      names = {"-o", "--output"},
      required = true,
      description =
          "Specify the path to write the translated EIP Schema. If a file does not exist at the path, it will be created. If a file already exists, it will be overwritten.")
  private File output;

  @Override
  public Integer call() throws Exception {
    XmlSchemaSourceConfiguration sourceConfig = parseSourceFile();

    EipSchemaTranslation translation = translateSchemas(sourceConfig);

    SchemaSerializer.writeSchemaToJsonFile(translation.getEipSchema(), output);

    return 0;
  }

  private XmlSchemaSourceConfiguration parseSourceFile() throws IOException {
    try (var fis = new FileInputStream(source)) {
      return XmlSchemaSourceConfiguration.readYaml(fis);
    }
  }

  private EipSchemaTranslation translateSchemas(XmlSchemaSourceConfiguration sourceConfig) {
    var schemaClient = new XmlSchemaHttpClient(sourceConfig.getImportedSchemaLocationsMap());
    return new EipSchemaTranslation(sourceConfig, schemaClient);
  }
}
