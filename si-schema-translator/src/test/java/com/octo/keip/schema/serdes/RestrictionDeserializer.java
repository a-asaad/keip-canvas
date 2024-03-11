package com.octo.keip.schema.serdes;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.octo.keip.schema.model.eip.Restriction;
import java.lang.reflect.Type;
import java.util.List;

public class RestrictionDeserializer implements JsonDeserializer<Restriction> {
  @Override
  public Restriction deserialize(JsonElement json, Type type, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();
    if (jsonObject.get("type").getAsString().equals(Restriction.RestrictionType.ENUM.toString())) {
      List<String> values =
          jsonObject.get("values").getAsJsonArray().asList().stream()
              .map(JsonElement::getAsString)
              .toList();
      return new Restriction.MultiValuedRestriction(Restriction.RestrictionType.ENUM, values);
    }

    throw new JsonParseException("Only enum restrictions are supported");
  }
}
