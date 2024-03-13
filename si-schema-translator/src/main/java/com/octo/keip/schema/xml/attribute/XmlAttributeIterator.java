package com.octo.keip.schema.xml.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroupMember;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroupRef;
import org.apache.ws.commons.schema.XmlSchemaAttributeOrGroupRef;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContentModel;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;

public class XmlAttributeIterator {

  private final XmlSchema xmlSchema;

  public XmlAttributeIterator(XmlSchema xmlSchema) {
    this.xmlSchema = xmlSchema;
  }

  // TODO: Naming
  public Stream<XmlSchemaAttribute> streamAttributes(XmlSchemaComplexType schemaComplexType) {
    var attributes = new HashSet<XmlSchemaAttribute>();

    List<XmlSchemaAttributeOrGroupRef> attrComposites =
        getAllAttributeComposites(schemaComplexType);

    attrComposites.forEach(
        (xmlAttr) -> {
          switch (xmlAttr) {
            case XmlSchemaAttribute attr -> attributes.add(attr);
            case XmlSchemaAttributeGroupRef groupRef -> {
              List<XmlSchemaAttributeGroupMember> groupMembers =
                  this.xmlSchema.getAttributeGroupByName(groupRef.getTargetQName()).getAttributes();
              attributes.addAll(getGroupMembers(groupMembers));
            }
            default -> throw new IllegalStateException("Unexpected value: " + xmlAttr);
          }
        });

    return attributes.stream();
  }

  private Set<XmlSchemaAttribute> getGroupMembers(
      List<XmlSchemaAttributeGroupMember> attributeGroupMembers) {
    var attributes = new HashSet<XmlSchemaAttribute>();

    attributeGroupMembers.forEach(
        (member) -> {
          switch (member) {
            case XmlSchemaAttribute attr -> attributes.add(attr);
            case XmlSchemaAttributeGroup group ->
                attributes.addAll(getGroupMembers(group.getAttributes()));
            case XmlSchemaAttributeGroupRef groupRef -> {
              List<XmlSchemaAttributeGroupMember> groupMembers =
                  this.xmlSchema.getAttributeGroupByName(groupRef.getTargetQName()).getAttributes();
              attributes.addAll(getGroupMembers(groupMembers));
            }
            default -> throw new IllegalStateException("Unexpected value: " + member);
          }
        });

    return attributes;
  }

  private List<XmlSchemaAttributeOrGroupRef> getAllAttributeComposites(
      XmlSchemaComplexType schemaComplexType) {
    XmlSchemaContentModel contentModel = schemaComplexType.getContentModel();
    if (contentModel == null || contentModel.getContent() == null) {
      return schemaComplexType.getAttributes();
    }

    var attrOrRefs = new ArrayList<XmlSchemaAttributeOrGroupRef>();

    switch (contentModel.getContent()) {
      case XmlSchemaSimpleContentExtension simpleExtension -> {
        attrOrRefs.addAll(simpleExtension.getAttributes());
        attrOrRefs.addAll(extractBaseTypes(simpleExtension.getBaseTypeName()));
      }
      case XmlSchemaComplexContentExtension complexExtension -> {
        attrOrRefs.addAll(complexExtension.getAttributes());
        attrOrRefs.addAll(extractBaseTypes(complexExtension.getBaseTypeName()));
      }
      case XmlSchemaSimpleContentRestriction simpleRestriction ->
          throw new IllegalStateException("TODO: Handle");
      case XmlSchemaComplexContentRestriction complexRestriction ->
          throw new IllegalStateException("TODO: Handle");

      default -> throw new IllegalStateException("Unexpected value: " + contentModel.getContent());
    }

    return attrOrRefs;
  }

  private List<XmlSchemaAttributeOrGroupRef> extractBaseTypes(QName baseType) {
    if (baseType == null) {
      return Collections.emptyList();
    }
    XmlSchemaType schemaType = this.xmlSchema.getTypeByName(baseType);
    if (!(schemaType instanceof XmlSchemaComplexType schemaComplexType)) {
      throw new IllegalStateException("SimpleTypes not supported yet");
    }
    return getAllAttributeComposites(schemaComplexType);
  }
}
