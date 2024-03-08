package com.octo.keip.schema.model.eip;

import com.google.gson.annotations.SerializedName;

// TODO: See if annotations can be removed from enum.
public enum AttributeType {
  @SerializedName("string")
  STRING,
  @SerializedName("boolean")
  BOOLEAN,
  @SerializedName("number")
  NUMBER;

  public static AttributeType of(String typeStr) {
    return switch (typeStr.toLowerCase()) {
      case "boolean" -> BOOLEAN;
      case "integer", "decimal", "float", "double" -> NUMBER;
      default -> STRING;
    };
  }
}
