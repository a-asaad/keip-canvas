package com.octo.keip.schema.xml;

import com.octo.keip.schema.model.eip.ChildComposite;
import com.octo.keip.schema.model.eip.ChildGroup;
import com.octo.keip.schema.model.eip.EipChildElement;
import com.octo.keip.schema.model.eip.Indicator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: Make methods static?
public class ChildGroupReducer {

  public ChildGroup reduceGroup(ChildComposite group) {
    ChildComposite reduced = reduce(group);
    return (ChildGroup) reduced;
  }

  private ChildComposite reduce(ChildComposite composite) {
    return switch (composite) {
      case null -> null;
      case EipChildElement element -> {
        ChildComposite group = reduce(element.getChildGroup());
        yield reduce(element.copyWith(group));
      }
      case ChildGroup group -> {
        List<ChildComposite> children = group.children().stream().map(this::reduce).toList();
        yield reduce(group.copyWith(children));
      }
    };
  }

  private ChildComposite reduce(EipChildElement element) {
    return element;
  }

  private ChildComposite reduce(ChildGroup group) {
    List<UnaryOperator<ChildGroup>> reducers =
        List.of(
            this::removeRedundantGroups,
            this::reduceSingleChildGroup,
            this::combineSameIndicatorGroups,
            this::deDuplicateElements);

    ChildGroup reduced = group;
    for (var r : reducers) {
      reduced = r.apply(reduced);
    }

    return reduced;
  }

  // Remove duplicated elements in a child group
  private ChildGroup deDuplicateElements(ChildGroup group) {
    Set<String> names = new HashSet<>();
    List<ChildComposite> deDuplicated =
        group.children().stream()
            .map(
                child ->
                    switch (child) {
                      case EipChildElement element -> {
                        if (names.add(element.getName())) {
                          yield element;
                        }
                        yield null;
                      }
                      case ChildGroup cg -> cg;
                    })
            .filter(Objects::nonNull)
            .toList();

    return group.copyWith(deDuplicated);
  }

  //   Remove adjacent groups with the same (possibly in a different order) child elements
  private ChildGroup removeRedundantGroups(ChildGroup group) {
    Set<String> elementNames = new HashSet<>();

    List<ChildComposite> reducedChildren =
        group.children().stream()
            .filter(
                child ->
                    switch (child) {
                      case EipChildElement element -> true;
                      case ChildGroup cg -> {
                        if (allChildrenAreElements(cg)) {
                          String combinedChildNames = concatChildNames(cg);
                          yield elementNames.add(combinedChildNames);
                        }
                        yield true;
                      }
                    })
            .toList();

    return group.copyWith(reducedChildren);
  }

  // TODO: Account for occurrence
  private ChildGroup reduceSingleChildGroup(ChildGroup group) {
    List<ChildComposite> reducedChildren =
        group.children().stream()
            .map(
                child ->
                    switch (child) {
                      case EipChildElement element -> element;
                      case ChildGroup cg -> {
                        if (cg.children().size() == 1) {
                          yield cg.children().getFirst();
                        } else {
                          yield cg;
                        }
                      }
                    })
            .toList();

    return group.copyWith(reducedChildren);
  }

  // TODO: Account for occurrence
  private ChildGroup combineSameIndicatorGroups(ChildGroup group) {
    List<ChildComposite> reducedChildren =
        group.children().stream()
            .flatMap(
                child ->
                    switch (child) {
                      case EipChildElement element -> Stream.of(element);
                      case ChildGroup cg -> {
                        if (isReducibleIndicator(cg.indicator())
                            && cg.indicator().equals(group.indicator())) {
                          yield cg.children().stream();
                        } else {
                          yield Stream.of(cg);
                        }
                      }
                    })
            .toList();

    return group.copyWith(reducedChildren);
  }

  private boolean isReducibleIndicator(Indicator indicator) {
    return !Indicator.CHOICE.equals(indicator);
  }

  private String concatChildNames(ChildGroup group) {
    return group.children().stream()
        .map(
            child ->
                switch (child) {
                  case EipChildElement element -> element.getName();
                  case ChildGroup ignored -> null;
                })
        .filter(Objects::nonNull)
        .sorted()
        .collect(Collectors.joining(""));
  }

  private boolean allChildrenAreElements(ChildGroup cg) {
    return cg.children().stream().allMatch(c -> c instanceof EipChildElement);
  }
}
