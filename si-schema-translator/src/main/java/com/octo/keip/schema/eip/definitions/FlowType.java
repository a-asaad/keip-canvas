package com.octo.keip.schema.eip.definitions;

import com.google.gson.annotations.SerializedName;

public enum FlowType {
  @SerializedName("source")
  SOURCE,
  @SerializedName("sink")
  SINK,
  @SerializedName("passthru")
  PASSTHRU;
}
