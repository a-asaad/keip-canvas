package com.octo.keip.schema.model.eip;

// TODO: Eventually might need to move this composite outside the model.
// If ChildGroups can only contain Child Element types.
public sealed interface ChildComposite permits ChildGroup, EipChildElement {

  void addChild(ChildComposite child);

  Occurrence occurrence();
}
