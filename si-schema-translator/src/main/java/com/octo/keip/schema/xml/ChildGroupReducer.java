package com.octo.keip.schema.xml;

import com.octo.keip.schema.model.eip.ChildComposite;
import com.octo.keip.schema.model.eip.ChildGroup;
import com.octo.keip.schema.model.eip.EipChildElement;
import com.octo.keip.schema.model.eip.Indicator;
import com.octo.keip.schema.model.eip.Occurrence;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChildGroupReducer {

  // TODO: Might potentially want to limit recursion depth to prevent StackOverflow errors.
  public ChildGroup reduceGroup(ChildComposite group) {
    ChildComposite reduced = reduce(group);
    return (ChildGroup) reduced;
  }

  private ChildComposite reduce(ChildComposite composite) {
    return switch (composite) {
      case null -> null;
      case EipChildElement element -> {
        ChildComposite group = reduce(element.getChildGroup());
        yield reduce(element.withChildGroup(group));
      }
      case ChildGroup group -> {
        List<ChildComposite> children = group.children().stream().map(this::reduce).toList();
        yield reduce(group.withChildren(children));
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
            this::collapseSingleChildGroup,
            this::collapseSameIndicatorGroups,
            this::deDuplicateElements,
            this::collapseChoiceGroups);

    ChildGroup reduced = group;
    for (var r : reducers) {
      reduced = r.apply(reduced);
    }

    return reduced;
  }

  /** Remove any duplicate (by name) element siblings in a child group. */
  ChildGroup deDuplicateElements(ChildGroup group) {
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

    return group.withChildren(deDuplicated);
  }

  /**
   * If multiple sibling groups have the same indicator and set of *element* children (in any
   * order), keep just one group and remove the rest.
   *
   * <p>Warning: If the redundant groups have different occurrence values, the occurrence value from
   * the first group is used.
   *
   * <p>Example: Sequence(Sequence(child1, child2), Sequence(child2, child1)) ->
   * Sequence(Sequence(child1, child2))
   */
  ChildGroup removeRedundantGroups(ChildGroup group) {
    Set<String> elementNames = new HashSet<>();

    List<ChildComposite> reducedChildren =
        group.children().stream()
            .filter(
                child ->
                    switch (child) {
                      case EipChildElement ignored -> true;
                      case ChildGroup cg -> {
                        if (allChildrenAreElements(cg)) {
                          String combinedChildNames = concatChildNames(cg);
                          yield elementNames.add(combinedChildNames);
                        }
                        yield true;
                      }
                    })
            .toList();

    return group.withChildren(reducedChildren);
  }

  /**
   * If a group has a single child, move the child up to the group's parent.
   *
   * <p>Example: Sequence(All(child1)) -> Sequence(child1)
   */
  ChildGroup collapseSingleChildGroup(ChildGroup group) {
    List<ChildComposite> reducedChildren =
        group.children().stream()
            .map(
                child ->
                    switch (child) {
                      case EipChildElement element -> element;
                      case ChildGroup cg -> {
                        if (cg.children().size() == 1) {
                          yield cg.children().getFirst().withOccurrence(cg.occurrence());
                        } else {
                          yield cg;
                        }
                      }
                    })
            .toList();

    return group.withChildren(reducedChildren);
  }

  /**
   * If parent and child groups have the same indicator, the child is removed from the tree and the
   * parent inherits its ancestors. The moved child will inherit the loosest occurrence values from
   * the old and new parents (i.e. occurrence=[min=min(old,new), max=max(old,new)])
   *
   * <p>Example: Sequence(Sequence(child1, child2)) -> Sequence(child1, child2)
   */
  ChildGroup collapseSameIndicatorGroups(ChildGroup group) {
    List<ChildComposite> reducedChildren =
        group.children().stream()
            .flatMap(
                child ->
                    switch (child) {
                      case EipChildElement element -> Stream.of(element);
                      case ChildGroup cg -> {
                        if (isReducibleIndicator(cg.indicator())
                            && cg.indicator().equals(group.indicator())) {
                          yield cg.children().stream().map(c -> this.relaxOccurrence(c, cg));
                        } else {
                          yield Stream.of(cg);
                        }
                      }
                    })
            .toList();

    return group.withChildren(reducedChildren);
  }

  /**
   * Remove any choice groups and move the children up to the choice groups' parents. The min
   * occurrence for the moved children is relaxed to zero, making each child optional.
   *
   * <p>Warning: This simplification may be too relaxed, will likely need to revisit at some point.
   *
   * <p>Example: Sequence(Choice(child1, child2)) -> Sequence(child1[min=0], child2[min=0])
   */
  ChildGroup collapseChoiceGroups(ChildGroup group) {
    List<ChildComposite> reducedChildren =
        group.children().stream()
            .flatMap(
                child ->
                    switch (child) {
                      case EipChildElement element -> Stream.of(element);
                      case ChildGroup cg -> {
                        if (Indicator.CHOICE.equals(cg.indicator())) {
                          yield cg.children().stream()
                              .map(c -> this.inheritOccurrenceFromChoice(c, cg));
                        } else {
                          yield Stream.of(cg);
                        }
                      }
                    })
            .toList();

    return group.withChildren(reducedChildren);
  }

  // TODO: Is the CHOICE exception still necessary?
  private boolean isReducibleIndicator(Indicator indicator) {
    return !Indicator.CHOICE.equals(indicator);
  }

  private ChildComposite relaxOccurrence(ChildComposite composite, ChildComposite parent) {
    long min = Math.min(composite.occurrence().min(), parent.occurrence().min());
    long max = Math.max(composite.occurrence().max(), parent.occurrence().max());
    return composite.withOccurrence(new Occurrence(min, max));
  }

  private ChildComposite inheritOccurrenceFromChoice(
      ChildComposite composite, ChildComposite group) {
    long max = Math.max(composite.occurrence().max(), group.occurrence().max());
    return composite.withOccurrence(new Occurrence(0, max));
  }

  private String concatChildNames(ChildGroup group) {
    String childNames =
        group.children().stream()
            .map(
                child ->
                    switch (child) {
                      case EipChildElement element -> element.getName();
                      case ChildGroup ignored -> null;
                    })
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.joining(""));
    return group.indicator().name() + ":" + childNames;
  }

  private boolean allChildrenAreElements(ChildGroup cg) {
    return cg.children().stream().allMatch(c -> c instanceof EipChildElement);
  }
}
