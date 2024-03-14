package com.octo.keip.schema.xml;

import com.octo.keip.schema.model.eip.Attribute;
import com.octo.keip.schema.xml.attribute.XmlAttributeTranslator;
import java.util.HashSet;
import java.util.Set;
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
  private final Set<Attribute> attributes;

  public EipTranslationVisitor() {
    this.attributeTranslator = new XmlAttributeTranslator();
    this.attributes = new HashSet<>();
  }

  public Set<Attribute> getAttributes() {
    return attributes;
  }

  @Override
  public void onEnterElement(
      XmlSchemaElement xmlSchemaElement, XmlSchemaTypeInfo xmlSchemaTypeInfo, boolean b) {
    int i = 5;
  }

  @Override
  public void onExitElement(
      XmlSchemaElement xmlSchemaElement, XmlSchemaTypeInfo xmlSchemaTypeInfo, boolean b) {
    int i = 5;
  }

  @Override
  public void onVisitAttribute(
      XmlSchemaElement xmlSchemaElement, XmlSchemaAttrInfo xmlSchemaAttrInfo) {
    if (!xmlSchemaElement.isTopLevel()) {
      return;
    }
    var name = xmlSchemaAttrInfo.getAttribute().getName();
    attributes.add(this.attributeTranslator.translate(xmlSchemaAttrInfo));
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
    int i = 5;
  }

  @Override
  public void onExitAllGroup(XmlSchemaAll xmlSchemaAll) {
    int i = 5;
  }

  @Override
  public void onEnterChoiceGroup(XmlSchemaChoice xmlSchemaChoice) {
    int i = 5;
  }

  @Override
  public void onExitChoiceGroup(XmlSchemaChoice xmlSchemaChoice) {
    int i = 5;
  }

  @Override
  public void onEnterSequenceGroup(XmlSchemaSequence xmlSchemaSequence) {
    int i = 5;
  }

  @Override
  public void onExitSequenceGroup(XmlSchemaSequence xmlSchemaSequence) {
    int i = 5;
  }

  @Override
  public void onVisitAny(XmlSchemaAny xmlSchemaAny) {
    int i = 5;
  }

  @Override
  public void onVisitAnyAttribute(
      XmlSchemaElement xmlSchemaElement, XmlSchemaAnyAttribute xmlSchemaAnyAttribute) {
    int i = 5;
  }
}
