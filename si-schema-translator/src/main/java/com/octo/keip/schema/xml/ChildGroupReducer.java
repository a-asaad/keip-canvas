package com.octo.keip.schema.xml;

import com.octo.keip.schema.model.eip.ChildComposite;
import com.octo.keip.schema.model.eip.ChildGroup;
import com.octo.keip.schema.model.eip.EipChildElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

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
        ChildGroup deDuplicated = deDuplicated(groupCopy);
        if (updated == null) {
          updated = deDuplicated;
        } else {
          updated.addChild(deDuplicated);
        }
      }
    }

    return updated;
  }

  public ChildGroup deDuplicated(ChildGroup group) {
    List<UnaryOperator<ChildGroup>> reducers =
        List.of(
            this::deDuplicateElements,
            this::removeRedundantGroups,
            this::reduceSingleChildGroup,
            this::combineSameIndicatorGroups);

    ChildGroup deDuplicated = group;
    for (var r : reducers) {
      deDuplicated = r.apply(deDuplicated);
    }

    return deDuplicated;
  }

  // Remove duplicated elements in a child group
  private ChildGroup deDuplicateElements(ChildGroup group) {
    Set<String> names = new HashSet<>();
    List<ChildComposite> noDups = new ArrayList<>(group.children().size());
    for (var c : group.children()) {
      if (c instanceof EipChildElement element) {
        if (names.add(element.getName())) {
          noDups.add(c);
        }
      } else {
        noDups.add(c);
      }
    }
    return group.replaceChildren(noDups);
  }

  //   Remove adjacent groups with the same (possibly in a different order) child elements
  private ChildGroup removeRedundantGroups(ChildGroup group) {
    Set<String> elementNames = new HashSet<>();
    Set<ChildGroup> toRemove = new HashSet<>();
    for (var child : group.children()) {
      if (child instanceof ChildGroup cg) {
        if (cg.children().stream().allMatch(c -> c instanceof EipChildElement)) {
          String combinedName =
              cg.children().stream()
                  .map(EipChildElement.class::cast)
                  .map(EipChildElement::getName)
                  .sorted()
                  .collect(Collectors.joining(""));
          if (!elementNames.add(combinedName)) {
            toRemove.add(cg);
          }
        }
      }
    }
    List<ChildComposite> updated =
        group.children().stream()
            .filter(c -> c instanceof EipChildElement || !toRemove.contains(c))
            .toList();
    return group.replaceChildren(updated);
  }

  private ChildGroup reduceSingleChildGroup(ChildGroup group) {
    List<ChildComposite> updated = new ArrayList<>(group.children().size());
    for (var child : group.children()) {
      if (child instanceof ChildGroup cg && cg.children().size() == 1) {
        updated.add(cg.children().getFirst());
      } else {
        updated.add(child);
      }
    }
    return group.replaceChildren(updated);
  }

  private ChildGroup combineSameIndicatorGroups(ChildGroup group) {
    List<ChildComposite> updated = new ArrayList<>(group.children().size());
    for (var child : group.children()) {
      if (child instanceof ChildGroup cg && cg.indicator().equals(group.indicator())) {
        updated.addAll(cg.children());
      } else {
        updated.add(child);
      }
    }
    return group.replaceChildren(updated);
  }
}
