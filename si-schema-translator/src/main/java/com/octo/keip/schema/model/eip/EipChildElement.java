package com.octo.keip.schema.model.eip;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EipChildElement extends EipElement implements ChildComposite {

  private Occurrence occurrence;

  private EipChildElement(Builder builder) {
    super(builder);
    this.occurrence = builder.occurrence;
  }

  @Override
  public void addChild(ChildComposite child) {
    this.setChildGroup(child);
  }

  @Override
  public List<ChildComposite> children() {
    return new ArrayList<>(List.of(this.getChildGroup()));
  }

  @Override
  public Occurrence occurrence() {
    return this.occurrence == null ? Occurrence.DEFAULT_OCCURRENCE : this.occurrence;
  }

  @Override
  public ChildComposite withOccurrence(Occurrence occurrence) {
    this.occurrence = occurrence;
    return this;
  }

  // TODO: Create a builder constructor that takes an element?
  public EipChildElement withChildGroup(ChildComposite child) {
    var element =
        new Builder(this.name)
            .description(this.description)
            .attributes(this.attributes)
            .occurrence(this.occurrence)
            .build();
    element.addChild(child);
    return element;
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
