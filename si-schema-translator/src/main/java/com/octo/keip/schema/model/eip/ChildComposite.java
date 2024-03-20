package com.octo.keip.schema.model.eip;

public sealed interface ChildComposite permits ChildGroup, EipChildElement {

  void addChild(ChildComposite child);

  Occurrence occurrence();

  ChildComposite withOccurrence(Occurrence occurrence);
}
