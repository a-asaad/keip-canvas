package com.octo.keip.schema.model.eip;

import java.util.Objects;

public final class EipChildElement extends EipElement {

  private final Occurrence occurrence;

  private EipChildElement(Builder builder) {
    super(builder);
    this.occurrence = builder.occurrence;
  }

  // TODO: Handle case when max is unbounded. (e.g. use "unbounded" or -1)
  public record Occurrence(int min, int max) {}

  public Occurrence getOccurrence() {
    return occurrence;
  }

  public static class Builder extends EipElement.Builder<Builder> {

    private Occurrence occurrence;

    public Builder(String name) {
      this.name = Objects.requireNonNull(name);
    }

    public Builder occurrence(Occurrence occurrence) {
      this.occurrence = occurrence;
      return self();
    }

    @Override
    public EipChildElement build() {
      return new EipChildElement(this);
    }

    @Override
    protected Builder self() {
      return this;
    }
  }
}
