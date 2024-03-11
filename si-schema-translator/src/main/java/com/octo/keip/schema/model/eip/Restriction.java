package com.octo.keip.schema.model.eip;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// TODO: This is not exhaustive. XML schemas support many more restriction types.
// Decide which ones we should support.
public sealed interface Restriction {

  enum RestrictionType {
    @SerializedName("enum")
    ENUM;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  RestrictionType type();

  record MultiValuedRestriction(RestrictionType type, List<String> values) implements Restriction {}
  ;
}
