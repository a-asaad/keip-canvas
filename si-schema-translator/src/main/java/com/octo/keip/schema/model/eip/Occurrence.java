package com.octo.keip.schema.model.eip;

public record Occurrence(long min, long max) {
  public static final Occurrence DEFAULT = new Occurrence(1, 1);

  public static final long UNBOUNDED = Long.MAX_VALUE;

  public boolean isDefault() {
    return DEFAULT.equals(this);
  }
}
