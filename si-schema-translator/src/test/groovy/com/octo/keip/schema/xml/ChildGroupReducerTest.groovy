package com.octo.keip.schema.xml

import com.octo.keip.schema.model.eip.ChildGroup
import com.octo.keip.schema.model.eip.EipChildElement
import com.octo.keip.schema.model.eip.Indicator
import com.octo.keip.schema.model.eip.Occurrence
import spock.lang.Specification

class ChildGroupReducerTest extends Specification {

    private final reducer = new ChildGroupReducer()

    def "Sibling groups with the same child set and indicators are deduplicated"() {
        given:
        def child1 = new EipChildElement.Builder("c1").build()
        def child2 = new EipChildElement.Builder("c2").build()
        def middle1 = new ChildGroup(Indicator.SEQUENCE, List.of(child1, child2))
        def middle2 = new ChildGroup(Indicator.SEQUENCE, List.of(child2, child1))
        def parent = new ChildGroup(Indicator.ALL, List.of(middle1, middle2))

        when:
        def result = reducer.removeRedundantGroups(parent)

        then:
        result.children().size() == 1
        result.children()[0] == middle1
        result.occurrence() == parent.occurrence()
        result.indicator() == parent.indicator()
    }

    def "Sibling groups with the same child set but different indicators are unchanged"() {
        given:
        def child1 = new EipChildElement.Builder("c1").build()
        def child2 = new EipChildElement.Builder("c2").build()
        def middle1 = new ChildGroup(Indicator.SEQUENCE, List.of(child1, child2))
        def middle2 = new ChildGroup(Indicator.CHOICE, List.of(child2, child1))
        def parent = new ChildGroup(Indicator.ALL, List.of(middle1, middle2))

        when:
        def result = reducer.removeRedundantGroups(parent)

        then:
        result == parent
    }

    def "Sibling groups with the same indicators but a mixed (group + element) child set are unchanged"() {
        given:
        def child1 = new EipChildElement.Builder("c1").build()
        def child2 = new EipChildElement.Builder("c2").build()
        def childGroup = new ChildGroup(Indicator.SEQUENCE, List.of());
        def middle1 = new ChildGroup(Indicator.SEQUENCE, List.of(child1, child2, childGroup))
        def middle2 = new ChildGroup(Indicator.SEQUENCE, List.of(child2, child1, childGroup))
        def parent = new ChildGroup(Indicator.ALL, List.of(middle1, middle2))

        when:
        def result = reducer.removeRedundantGroups(parent)

        then:
        result == parent
    }

    def "Group with a single child is collapsed"() {
        given:
        def child = new EipChildElement.Builder("c1").build()
        def middleOccurrence = new Occurrence(0, 3)
        def middle = new ChildGroup(Indicator.ALL, middleOccurrence, List.of(child))
        def parent = new ChildGroup(Indicator.SEQUENCE, List.of(middle))

        when:
        def result = reducer.collapseSingleChildGroup(parent)

        then:
        parent.children().size() == 1
        result.children()[0] == child
        child.occurrence() == middleOccurrence
        result.occurrence() == parent.occurrence()
        result.indicator() == parent.indicator()
    }

    def "Group with multiple children unchanged"() {
        given:
        def child1 = new EipChildElement.Builder("c1").build()
        def child2 = new EipChildElement.Builder("c2").build()
        def middle = new ChildGroup(Indicator.ALL, List.of(child1, child2))
        def parent = new ChildGroup(Indicator.SEQUENCE, List.of(middle))

        when:
        def result = reducer.collapseSingleChildGroup(parent)

        then:
        result == parent
    }

    def "Group with same indicator child group is collapsed"(Occurrence middleOccur, Occurrence childOccur, Occurrence resultOccur) {
        given:
        def child1 = new EipChildElement.Builder("c1").occurrence(childOccur).build()
        def child2 = new EipChildElement.Builder("c2").occurrence(childOccur).build()
        def middle = new ChildGroup(Indicator.SEQUENCE, middleOccur, List.of(child1, child2))
        def parent = new ChildGroup(Indicator.SEQUENCE, List.of(middle))

        when:
        def result = reducer.collapseSameIndicatorGroups(parent)

        then:
        result.children() == [child1, child2]
        child1.occurrence() == resultOccur
        child2.occurrence() == resultOccur
        result.indicator() == parent.indicator()
        result.occurrence() == result.occurrence()

        where:
        middleOccur                             | childOccur           | resultOccur
        new Occurrence(0, Occurrence.UNBOUNDED) | new Occurrence(1, 1) | new Occurrence(0, Occurrence.UNBOUNDED)
        new Occurrence(0, 1)                    | new Occurrence(1, 5) | new Occurrence(0, 5)
        new Occurrence(1, 3)                    | new Occurrence(0, 1) | new Occurrence(0, 3)
        new Occurrence(1, 1)                    | new Occurrence(0, 2) | new Occurrence(0, 2)
    }

    def "CHOICE group with a CHOICE child group is a special case and is not collapsed by the same indicator reducer"() {
        given:
        def child1 = new EipChildElement.Builder("c1").build()
        def child2 = new EipChildElement.Builder("c2").build()
        def middle = new ChildGroup(Indicator.CHOICE, List.of(child1, child2))
        def parent = new ChildGroup(Indicator.CHOICE, List.of(middle))

        when:
        def result = reducer.collapseSameIndicatorGroups(parent)

        then:
        result == parent
    }

    def "Sibling elements with the same name are deduplicated"() {
        given:
        def child1 = new EipChildElement.Builder("c1").build()
        def child2 = new EipChildElement.Builder("c2").build()
        def child3 = new EipChildElement.Builder("c1").build()
        def parent = new ChildGroup(Indicator.ALL, List.of(child1, child2, child3))

        when:
        def result = reducer.deDuplicateElements(parent)

        then:
        result.children() == [child1, child2]
    }

    def "Groups with CHOICE indicator are collapsed"() {
        given:
        def child1 = new EipChildElement.Builder("c1").occurrence(new Occurrence(1, 2)).build()
        def child2 = new EipChildElement.Builder("c2").occurrence(new Occurrence(1, 2)).build()
        def middle = new ChildGroup(Indicator.CHOICE, List.of(child1, child2))
        def parent = new ChildGroup(Indicator.SEQUENCE, List.of(middle))

        def expectedOccurrence = new Occurrence(0, 2)

        when:
        def result = reducer.collapseChoiceGroups(parent)

        then:
        result.children() == [child1, child2]
        child1.occurrence() == expectedOccurrence
        child1.occurrence() == expectedOccurrence
    }
}
