package com.octo.keip.schema.model.eip;

import java.util.List;

// TODO: Explain how to this does not map one-to-one with XML Schema child group indicators.
public record ChildGroup(
    Indicator indicator, List<EipChildElement> elements, Occurrence occurrence) {
  public ChildGroup(Indicator indicator, List<EipChildElement> elements) {
    this(indicator, elements, null);
  }
}
