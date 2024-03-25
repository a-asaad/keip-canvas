package com.octo.keip.schema.xml.visitor;

import com.octo.keip.schema.model.eip.Attribute;
import com.octo.keip.schema.model.eip.ChildComposite;
import com.octo.keip.schema.model.eip.ChildGroup;
import com.octo.keip.schema.model.eip.EipChildElement;
import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.FlowType;
import com.octo.keip.schema.model.eip.Indicator;
import com.octo.keip.schema.model.eip.Occurrence;
import com.octo.keip.schema.model.eip.Role;
import com.octo.keip.schema.xml.attribute.AnnotationTranslator;
import com.octo.keip.schema.xml.attribute.AttributeTranslator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroupParticle;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.walker.XmlSchemaAttrInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaVisitor;

// TODO: Refactor
public class EipTranslationVisitor implements XmlSchemaVisitor {

  private final AttributeTranslator attributeTranslator;

  private final AnnotationTranslator annotationTranslator;

  private final Map<QName, ChildComposite> discoveredElements;

  private EipComponent.Builder eipComponentBuilder;

  private ChildCompositeWrapper currElement;

  public EipTranslationVisitor() {
    this.attributeTranslator = new AttributeTranslator();
    this.annotationTranslator = new AnnotationTranslator();
    discoveredElements = new HashMap<>();
  }

  // TODO: Return EipComponent instead of builder
  public EipComponent.Builder getEipComponent() {
    return eipComponentBuilder;
  }

  public void reset() {
    eipComponentBuilder = null;
    currElement = null;
  }

  @Override
  public void onEnterElement(
      XmlSchemaElement xmlSchemaElement, XmlSchemaTypeInfo xmlSchemaTypeInfo, boolean visited) {
    if (xmlSchemaElement.isTopLevel()) {
      assert eipComponentBuilder == null
          : "The top level element should only be entered once. Was the visitor reset?";
      // TODO: Figure out how to get flowtype and role.
      eipComponentBuilder =
          new EipComponent.Builder(xmlSchemaElement.getName(), Role.ENDPOINT, FlowType.SOURCE)
              .description(annotationTranslator.getDescription(xmlSchemaElement));
      return;
    }

    ChildComposite element;
    if (visited) {
      element = discoveredElements.get(getKey(xmlSchemaElement));
      element = element.withOccurrence(getOccurrence(xmlSchemaElement));
    } else {
      element =
          new EipChildElement.Builder(xmlSchemaElement.getName())
              .occurrence(getOccurrence(xmlSchemaElement))
              .description(annotationTranslator.getDescription(xmlSchemaElement))
              .build();
      discoveredElements.putIfAbsent(getKey(xmlSchemaElement), element);
    }

    var wrapper = new ChildCompositeWrapper(element, currElement);
    currElement.wrappedChild.addChild(element);
    currElement = wrapper;
  }

  @Override
  public void onExitElement(
      XmlSchemaElement xmlSchemaElement, XmlSchemaTypeInfo xmlSchemaTypeInfo, boolean visited) {
    if (xmlSchemaElement.isTopLevel()) {
      return;
    }
    exitNode();
  }

  @Override
  public void onVisitAttribute(
      XmlSchemaElement xmlSchemaElement, XmlSchemaAttrInfo xmlSchemaAttrInfo) {
    Attribute attribute = attributeTranslator.translate(xmlSchemaAttrInfo);

    if (xmlSchemaElement.isTopLevel()) {
      eipComponentBuilder.addAttribute(attribute);
      return;
    }

    if (currElement.wrappedChild instanceof EipChildElement child) {
      child.addAttribute(attribute);
    }
  }

  @Override
  public void onEndAttributes(
      XmlSchemaElement xmlSchemaElement, XmlSchemaTypeInfo xmlSchemaTypeInfo) {}

  @Override
  public void onEnterSubstitutionGroup(XmlSchemaElement xmlSchemaElement) {}

  @Override
  public void onExitSubstitutionGroup(XmlSchemaElement xmlSchemaElement) {}

  @Override
  public void onEnterAllGroup(XmlSchemaAll xmlSchemaAll) {
    enterGroup(Indicator.ALL, xmlSchemaAll);
  }

  @Override
  public void onExitAllGroup(XmlSchemaAll xmlSchemaAll) {
    exitNode();
  }

  @Override
  public void onEnterChoiceGroup(XmlSchemaChoice xmlSchemaChoice) {
    enterGroup(Indicator.CHOICE, xmlSchemaChoice);
  }

  @Override
  public void onExitChoiceGroup(XmlSchemaChoice xmlSchemaChoice) {
    exitNode();
  }

  @Override
  public void onEnterSequenceGroup(XmlSchemaSequence xmlSchemaSequence) {
    enterGroup(Indicator.SEQUENCE, xmlSchemaSequence);
  }

  @Override
  public void onExitSequenceGroup(XmlSchemaSequence xmlSchemaSequence) {
    exitNode();
  }

  @Override
  public void onVisitAny(XmlSchemaAny xmlSchemaAny) {
    // TODO: Might need to parse this for arbitrary beans.
  }

  @Override
  public void onVisitAnyAttribute(
      XmlSchemaElement xmlSchemaElement, XmlSchemaAnyAttribute xmlSchemaAnyAttribute) {}

  private void enterGroup(Indicator indicator, XmlSchemaGroupParticle particle) {
    var group = new ChildGroup(indicator, getOccurrence(particle));
    var wrapper = new ChildCompositeWrapper(group, currElement);

    // TODO: Prefer a cleaner way to do this one time check
    if (currElement == null) {
      // top level child group
      eipComponentBuilder.childGroup(group);
    } else {
      currElement.wrappedChild.addChild(group);
    }

    currElement = wrapper;
  }

  private void exitNode() {
    currElement = currElement.parent();
  }

  private Occurrence getOccurrence(XmlSchemaParticle particle) {
    return new Occurrence(particle.getMinOccurs(), particle.getMaxOccurs());
  }

  private record ChildCompositeWrapper(ChildComposite wrappedChild, ChildCompositeWrapper parent) {}

  private QName getKey(XmlSchemaElement element) {
    QName qName = element.getSchemaTypeName();
    return qName == null ? element.getQName() : qName;
  }

  // TODO: REMOVE
  public static void printTree(ChildComposite child, String indentation) {
    System.out.print(indentation);

    List<ChildComposite> children = new ArrayList<>();

    var minOccur = child.occurrence().min();
    var maxOccur = child.occurrence().max() == Occurrence.UNBOUNDED ? -1 : child.occurrence().max();

    if (child instanceof EipChildElement element) {
      System.out.printf("%s (%d, %d)%n", element.getName(), minOccur, maxOccur);
      if (element.getChildGroup() != null) {
        children.add(element.getChildGroup());
      }
    } else if (child instanceof ChildGroup group) {
      System.out.printf("%s (%d, %d)%n", group.indicator(), minOccur, maxOccur);
      children.addAll(group.children());
    }

    for (var c : children) {
      printTree(c, indentation + "  ");
    }
  }
}
