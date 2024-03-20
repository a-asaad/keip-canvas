package com.octo.keip.schema.model.eip;

// TODO: Handle case when max is unbounded. (e.g. use "unbounded" or -1)
public record Occurrence(long min, long max) {
  public static final Occurrence DEFAULT_OCCURRENCE = new Occurrence(1, 1);

  public static final long UNBOUNDED = Long.MAX_VALUE;

  public boolean isDefault() {
    return DEFAULT_OCCURRENCE.equals(this);
  }
}
