package com.octo.keip.schema.xml;

import com.octo.keip.schema.model.eip.ChildComposite;
import com.octo.keip.schema.model.eip.ChildGroup;
import com.octo.keip.schema.model.eip.EipChildElement;

// TODO: Eliminate casts and use polymorphism instead.
public class ChildGroupReducer {

  public ChildGroup reduce(ChildComposite composite) {
    ChildComposite result = reduce(composite, null);
    return (ChildGroup) result;
  }

  private ChildComposite reduce(ChildComposite composite, ChildComposite updated) {
    if (composite == null) {
      return null;
    }

    switch (composite) {
      case EipChildElement element -> {
        EipChildElement elementCopy = element.childlessCopy();
        reduce(element.getChildGroup(), elementCopy);
        updated.addChild(elementCopy);
      }
      case ChildGroup group -> {
        // TODO: Refactor
        ChildGroup groupCopy = group.childlessCopy();
        group.children().forEach(c -> reduce(c, groupCopy));
        ChildGroup deDuplicated = groupCopy.deDuplicated();
        if (updated == null) {
          updated = deDuplicated;
        } else if (updated instanceof ChildGroup && deDuplicated.children().size() == 1) {
          updated.addChild(deDuplicated.children().getFirst());
        } else {
          updated.addChild(deDuplicated);
        }
      }
    }

    return updated;
  }
}
