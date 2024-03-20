package com.octo.keip.schema.model.eip;

import java.util.ArrayList;
import java.util.List;

// TODO: Explain how this does not map one-to-one with XML Schema child group indicators.
public record ChildGroup(Indicator indicator, Occurrence occurrence, List<ChildComposite> children)
    implements ChildComposite {
  public ChildGroup(Indicator indicator, Occurrence occurrence) {
    this(indicator, occurrence, new ArrayList<>());
  }

  @Override
  public void addChild(ChildComposite child) {
    children.add(child);
  }

  @Override
  public ChildComposite withOccurrence(Occurrence occurrence) {
    return new ChildGroup(this.indicator, occurrence, this.children);
  }

  public ChildGroup withChildren(List<ChildComposite> children) {
    return new ChildGroup(this.indicator, this.occurrence, children);
  }
}
