package com.octo.keip.schema.serdes;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.octo.keip.schema.model.eip.ChildComposite;
import com.octo.keip.schema.model.eip.ChildGroup;
import com.octo.keip.schema.model.eip.EipChildElement;
import java.lang.reflect.Type;

public class ChildCompositeDeserializer implements JsonDeserializer<ChildComposite> {
  @Override
  public ChildComposite deserialize(JsonElement json, Type type, JsonDeserializationContext context)
      throws JsonParseException {
    if (json.getAsJsonObject().has("indicator")) {
      return context.deserialize(json, ChildGroup.class);
    }
    return context.deserialize(json, EipChildElement.class);
  }
}
