package com.octo.keip.schema.model.eip;

import com.google.gson.annotations.SerializedName;

public enum Role {
  @SerializedName("endpoint")
  ENDPOINT,
  @SerializedName("channel")
  CHANNEL;
}
