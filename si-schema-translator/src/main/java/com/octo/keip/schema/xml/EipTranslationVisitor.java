package com.octo.keip.schema.xml;

import com.octo.keip.schema.model.eip.ChildComposite;
import com.octo.keip.schema.model.eip.ChildGroup;
import com.octo.keip.schema.model.eip.EipChildElement;
import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.FlowType;
import com.octo.keip.schema.model.eip.Indicator;
import com.octo.keip.schema.model.eip.Occurrence;
import com.octo.keip.schema.model.eip.Role;
import com.octo.keip.schema.xml.attribute.XmlAttributeTranslator;
import java.util.Collections;
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
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.walker.XmlSchemaAttrInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaVisitor;

public class EipTranslationVisitor implements XmlSchemaVisitor {

  private final XmlAttributeTranslator attributeTranslator;

  private final Map<QName, ChildComposite> childElements;

  private EipComponent.Builder eipComponentBuilder;

  private ChildCompositeWrapper currElement;

  public EipTranslationVisitor() {
    this.attributeTranslator = new XmlAttributeTranslator();
    childElements = new HashMap<>();
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
      assert eipComponentBuilder == null: "The top level element should only be entered once. Was the visitor reset?";
      // TODO: Figure out how to get flowtype and role.
      eipComponentBuilder =
          new EipComponent.Builder(xmlSchemaElement.getName(), Role.ENDPOINT, FlowType.SOURCE);
      return;
    }

    ChildComposite element;
    if (visited) {
      element = childElements.get(xmlSchemaElement.getQName());
    } else {
      var occurrence =
          new Occurrence(xmlSchemaElement.getMinOccurs(), xmlSchemaElement.getMaxOccurs());
      element =
          new EipChildElement.Builder(xmlSchemaElement.getName()).occurrence(occurrence).build();
    }

    var wrapper = new ChildCompositeWrapper(element, currElement);
    currElement.wrappedChild.addChild(element);
    currElement = wrapper;
  }

  @Override
  public void onExitElement(
      XmlSchemaElement xmlSchemaElement, XmlSchemaTypeInfo xmlSchemaTypeInfo, boolean b) {
    if (xmlSchemaElement.isTopLevel()) {
      return;
    }
    childElements.put(xmlSchemaElement.getQName(), currElement.wrappedChild);
    exitNode();
  }

  @Override
  public void onVisitAttribute(
      XmlSchemaElement xmlSchemaElement, XmlSchemaAttrInfo xmlSchemaAttrInfo) {
    if (!xmlSchemaElement.isTopLevel()) {
      return;
    }
    eipComponentBuilder.addAttribute(attributeTranslator.translate(xmlSchemaAttrInfo));
  }

  @Override
  public void onEndAttributes(
      XmlSchemaElement xmlSchemaElement, XmlSchemaTypeInfo xmlSchemaTypeInfo) {
    int i = 5;
  }

  @Override
  public void onEnterSubstitutionGroup(XmlSchemaElement xmlSchemaElement) {
    int i = 5;
  }

  @Override
  public void onExitSubstitutionGroup(XmlSchemaElement xmlSchemaElement) {
    int i = 5;
  }

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
    int i = 5;
  }

  @Override
  public void onVisitAnyAttribute(
      XmlSchemaElement xmlSchemaElement, XmlSchemaAnyAttribute xmlSchemaAnyAttribute) {
    int i = 5;
  }

  private void enterGroup(Indicator indicator, XmlSchemaGroupParticle particle) {
    var occurrence = new Occurrence(particle.getMinOccurs(), particle.getMaxOccurs());
    var group = new ChildGroup(indicator, occurrence);
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

  private void compressGroup() {

  }

  private record ChildCompositeWrapper(ChildComposite wrappedChild, ChildCompositeWrapper parent) {}

  // TODO: REMOVE
  public static void printTree(ChildComposite child, String indentation) {
    System.out.print(indentation);

    List<ChildComposite> children = Collections.emptyList();
    if (child instanceof EipChildElement element) {
      System.out.println(element.getName());
      if (element.getChildGroup() != null) {
        children = element.getChildGroup().children();
      }
    } else if (child instanceof ChildGroup group) {
      System.out.println(group.indicator());
      children = group.children();
    }

    for (var c : children) {
      printTree(c, indentation + "  ");
    }
  }
}
