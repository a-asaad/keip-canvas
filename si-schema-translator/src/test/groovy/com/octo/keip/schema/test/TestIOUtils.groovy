package com.octo.keip.schema.test

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.octo.keip.schema.model.eip.ChildComposite
import com.octo.keip.schema.model.eip.Occurrence
import com.octo.keip.schema.model.eip.Restriction
import com.octo.keip.schema.serdes.ChildCompositeDeserializer
import com.octo.keip.schema.serdes.OccurrenceDeserializer
import com.octo.keip.schema.serdes.RestrictionDeserializer

import java.nio.file.Path

class TestIOUtils {

    static BufferedReader getXmlSchemaFileReader(Path filepath) {
        Path path = Path.of("schemas", "xml").resolve(filepath)
        return TestIOUtils.class.getClassLoader().getResource(path.toString()).newReader()
    }

    static BufferedInputStream getYamlConfig(Path filePath) {
        String path = Path.of("source-configs").resolve(filePath)
        return TestIOUtils.class.getClassLoader().getResource(path.toString()).newInputStream()
    }

    static Gson configureGson() {
        var gson = new GsonBuilder()
        gson.registerTypeAdapter(Restriction.class, new RestrictionDeserializer())
        gson.registerTypeAdapter(ChildComposite.class, new ChildCompositeDeserializer())
        gson.registerTypeAdapter(Occurrence.class, new OccurrenceDeserializer())
        return gson.create()
    }
}
