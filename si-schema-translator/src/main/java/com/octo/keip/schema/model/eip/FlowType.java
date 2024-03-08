package com.octo.keip.schema.model.eip;

import com.google.gson.annotations.SerializedName;

public enum FlowType {
  @SerializedName("source")
  SOURCE,
  @SerializedName("sink")
  SINK,
  @SerializedName("passthru")
  PASSTHRU;
}
