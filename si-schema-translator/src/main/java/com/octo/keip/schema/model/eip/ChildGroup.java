package com.octo.keip.schema.model.eip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  public ChildGroup childlessCopy() {
    return new ChildGroup(this.indicator, this.occurrence, new ArrayList<>());
  }

  public ChildGroup deDuplicated() {
    List<ChildComposite> deDuplicated = deDuplicateChildren();
    return new ChildGroup(this.indicator, this.occurrence, deDuplicated);
  }

  private List<ChildComposite> deDuplicateChildren() {
    Set<String> names = new HashSet<>();
    List<ChildComposite> noDups = new ArrayList<>(this.children.size());
    for (var c : this.children) {
      if (c instanceof EipChildElement element) {
        if (names.add(element.getName())) {
          noDups.add(c);
        }
      } else {
        noDups.add(c);
      }
    }
    return noDups;
  }
}
