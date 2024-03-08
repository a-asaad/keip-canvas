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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EipChildElement that)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(occurrence, that.occurrence);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), occurrence);
  }
}
