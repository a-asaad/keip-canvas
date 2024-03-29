package com.octo.keip.schema.config;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class SchemaRetrievalConfiguration {

  private List<SchemaIdentifier> schemas;

  public List<SchemaIdentifier> getSchemas() {
    return schemas;
  }

  public void setSchemas(List<SchemaIdentifier> schemas) {
    this.schemas = schemas;
  }

  public static SchemaRetrievalConfiguration readYaml(InputStream is) {
    Yaml yaml = new Yaml(new Constructor(SchemaRetrievalConfiguration.class, new LoaderOptions()));
    return yaml.loadAs(is, SchemaRetrievalConfiguration.class);
  }

  public static class SchemaIdentifier {
    private String alias;
    private String namespace;
    private URI location;

    private List<SchemaIdentifier> importedSchemas = Collections.emptyList();

    private Set<String> excludedElements = Collections.emptySet();

    public String getAlias() {
      return alias;
    }

    public String getNamespace() {
      return namespace;
    }

    public URI getLocation() {
      return location;
    }

    public List<SchemaIdentifier> getImportedSchemas() {
      return importedSchemas;
    }

    public Set<String> getExcludedElements() {
      return excludedElements;
    }

    public void setAlias(String alias) {
      this.alias = alias;
    }

    public void setNamespace(String namespace) {
      this.namespace = namespace;
    }

    public void setLocation(URI location) {
      this.location = location;
    }

    public void setImportedSchemas(List<SchemaIdentifier> importedSchemas) {
      this.importedSchemas = importedSchemas;
    }

    public void setExcludedElements(Set<String> excludedElements) {
      this.excludedElements = excludedElements;
    }
  }
}
