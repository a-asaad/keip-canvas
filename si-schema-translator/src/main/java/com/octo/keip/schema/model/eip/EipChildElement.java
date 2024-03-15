package com.octo.keip.schema.model.eip;

import java.util.Objects;

public final class EipChildElement extends EipElement implements ChildComposite {

  private final Occurrence occurrence;

  private EipChildElement(Builder builder) {
    super(builder);
    this.occurrence = builder.occurrence;
  }

  @Override
  public void addChild(ChildComposite child) {
    // TODO: Not great to have to cast here. Rethink.
    this.setChildGroup((ChildGroup) child);
  }

  public Occurrence occurrence() {
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
