package com.octo.keip.schema.xml;

import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.FlowType;
import com.octo.keip.schema.model.eip.Role;
import com.octo.keip.schema.xml.attribute.XmlAttributeTranslator;
import java.util.ArrayList;
import java.util.List;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.walker.XmlSchemaAttrInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaVisitor;

public class EipTranslationVisitor implements XmlSchemaVisitor {

  private final XmlAttributeTranslator attributeTranslator;

  private EipComponent.Builder eipComponentBuilder;

  private ChildComposite currElement;

  public EipTranslationVisitor() {
    this.attributeTranslator = new XmlAttributeTranslator();
  }

  public EipComponent getEipComponent() {
    return eipComponentBuilder.build();
  }

  @Override
  public void onEnterElement(
      XmlSchemaElement xmlSchemaElement, XmlSchemaTypeInfo xmlSchemaTypeInfo, boolean b) {
    if (xmlSchemaElement.isTopLevel()) {
      assert eipComponentBuilder == null;
      currElement = new ChildComposite.Element(xmlSchemaElement.getName(), null);
      // TODO: Figure out how to get flowtype and role.
      eipComponentBuilder =
          new EipComponent.Builder(xmlSchemaElement.getName(), Role.ENDPOINT, FlowType.SOURCE);
      return;
    }
    var element = new ChildComposite.Element(xmlSchemaElement.getName(), currElement);
    currElement.addChild(element);
    currElement = element;
  }

  @Override
  public void onExitElement(
      XmlSchemaElement xmlSchemaElement, XmlSchemaTypeInfo xmlSchemaTypeInfo, boolean b) {
    if (!xmlSchemaElement.isTopLevel()) {
      exitNode();
    }
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
    enterGroup("all");
  }

  @Override
  public void onExitAllGroup(XmlSchemaAll xmlSchemaAll) {
    exitNode();
  }

  @Override
  public void onEnterChoiceGroup(XmlSchemaChoice xmlSchemaChoice) {
    enterGroup("choice");
  }

  @Override
  public void onExitChoiceGroup(XmlSchemaChoice xmlSchemaChoice) {
    exitNode();
  }

  @Override
  public void onEnterSequenceGroup(XmlSchemaSequence xmlSchemaSequence) {
    enterGroup("sequence");
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

  private void enterGroup(String name) {
    var element = new ChildComposite.Group(name, currElement);
    currElement.addChild(element);
    currElement = element;
  }

  private void exitNode() {
    currElement = currElement.parent();
  }

  private sealed interface ChildComposite {

    List<ChildComposite> children();

    ChildComposite parent();

    default void addChild(ChildComposite child) {
      children().add(child);
    }

    record Element(String name, ChildComposite parent, List<ChildComposite> children)
        implements ChildComposite {
      public Element(String name, ChildComposite parent) {
        this(name, parent, new ArrayList<>());
      }
    }

    record Group(String name, ChildComposite parent, List<ChildComposite> children)
        implements ChildComposite {
      public Group(String name, ChildComposite parent) {
        this(name, parent, new ArrayList<>());
      }
    }
  }
}
