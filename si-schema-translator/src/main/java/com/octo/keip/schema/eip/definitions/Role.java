package com.octo.keip.schema.eip.definitions;

import com.google.gson.annotations.SerializedName;

public enum Role {
  @SerializedName("endpoint")
  ENDPOINT,
  @SerializedName("channel")
  CHANNEL;
}
