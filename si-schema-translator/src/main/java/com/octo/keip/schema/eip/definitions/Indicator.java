package com.octo.keip.schema.eip.definitions;

import com.google.gson.annotations.SerializedName;

public enum Indicator {
  @SerializedName("all")
  ALL,
  @SerializedName("choice")
  CHOICE,
  @SerializedName("sequence")
  SEQUENCE;
}
