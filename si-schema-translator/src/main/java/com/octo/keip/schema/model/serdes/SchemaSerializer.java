package com.octo.keip.schema.model.serdes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.octo.keip.schema.model.eip.EipSchema;
import com.octo.keip.schema.model.eip.Occurrence;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class SchemaSerializer {
  static final Gson GSON =
      new GsonBuilder().registerTypeAdapter(Occurrence.class, new OccurrenceSerializer()).create();

  public static void writeSchemaToJsonFile(EipSchema eipSchema, File file) throws IOException {
    try (Writer writer = new FileWriter(file)) {
      GSON.toJson(eipSchema.toMap(), writer);
    }
  }
}
