package com.octo.keip.schema.xml.attribute;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroupMember;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroupRef;
import org.apache.ws.commons.schema.XmlSchemaComplexType;

public class XmlAttributeIterator {

  private final XmlSchema xmlSchema;

  public XmlAttributeIterator(XmlSchema xmlSchema) {
    this.xmlSchema = xmlSchema;
  }

  public Stream<XmlSchemaAttribute> visitXmlAttributes(XmlSchemaComplexType xmlComplexType) {
    var attributes = new HashSet<XmlSchemaAttribute>();

    xmlComplexType
        .getAttributes()
        .forEach(
            (xmlAttr) -> {
              switch (xmlAttr) {
                case XmlSchemaAttribute attr -> attributes.add(attr);
                case XmlSchemaAttributeGroupRef groupRef -> {
                  List<XmlSchemaAttributeGroupMember> groupMembers =
                      this.xmlSchema
                          .getAttributeGroupByName(groupRef.getTargetQName())
                          .getAttributes();
                  attributes.addAll(visitAttributeGroupMembers(groupMembers));
                }
                default -> throw new IllegalStateException("Unexpected value: " + xmlAttr);
              }
            });

    return attributes.stream();
  }

  private Set<XmlSchemaAttribute> visitAttributeGroupMembers(
      List<XmlSchemaAttributeGroupMember> attributeGroupMembers) {
    var attributes = new HashSet<XmlSchemaAttribute>();

    attributeGroupMembers.forEach(
        (member) -> {
          switch (member) {
            case XmlSchemaAttribute attr -> attributes.add(attr);
            case XmlSchemaAttributeGroup group ->
                attributes.addAll(visitAttributeGroupMembers(group.getAttributes()));
            case XmlSchemaAttributeGroupRef groupRef -> {
              List<XmlSchemaAttributeGroupMember> groupMembers =
                  this.xmlSchema.getAttributeGroupByName(groupRef.getTargetQName()).getAttributes();
              attributes.addAll(visitAttributeGroupMembers(groupMembers));
            }
            default -> throw new IllegalStateException("Unexpected value: " + member);
          }
        });

    return attributes;
  }
}
