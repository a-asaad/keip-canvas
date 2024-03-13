package com.octo.keip.schema.model.eip;

import java.util.List;

public record ChildGroup(
    Indicator indicator, List<EipChildElement> elements, Occurrence occurrence) {
  public ChildGroup(Indicator indicator, List<EipChildElement> elements) {
    this(indicator, elements, null);
  }
}
