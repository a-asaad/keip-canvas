package com.octo.keip.schema.xml.attribute;

import com.octo.keip.schema.model.eip.Attribute;
import com.octo.keip.schema.model.eip.AttributeType;
import com.octo.keip.schema.model.eip.Restriction;
import java.util.Collections;
import java.util.List;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.apache.ws.commons.schema.walker.XmlSchemaAttrInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaBaseSimpleType;
import org.apache.ws.commons.schema.walker.XmlSchemaRestriction;
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo;

public class XmlAttributeTranslator {

  public Attribute translate(XmlSchemaAttrInfo attrInfo) {
    XmlSchemaAttribute attribute = attrInfo.getAttribute();
    XmlSchemaTypeInfo typeInfo = attrInfo.getType();

    // TODO: review asserts
    assert !XmlSchemaUse.PROHIBITED.equals(attribute.getUse());
    assert !typeInfo.isMixed();

    Attribute.Builder builder =
        new Attribute.Builder(attribute.getName(), getType(typeInfo))
            .defaultValue(attribute.getDefaultValue());

    if (XmlSchemaUse.REQUIRED.equals(attribute.getUse())) {
      builder.required(true);
    }

    String description = AnnotationTranslator.getDescription(attribute.getAnnotation());
    if (!description.isBlank()) {
      builder.description(description);
    }

    Restriction restriction = getRestriction(typeInfo);
    if (restriction != null) {
      builder.restriction(restriction);
    }

    return builder.build();
  }

  private AttributeType getType(XmlSchemaTypeInfo typeInfo) {
    return switch (typeInfo.getType()) {
      case LIST -> throw new IllegalStateException("TODO: Figure out how to handle list types");
      case UNION -> resolveUnionType(typeInfo);
      case ATOMIC -> toAttributeType(typeInfo.getBaseType());
      case COMPLEX ->
          throw new IllegalStateException("TODO: Is it even possible to have complex attr types?");
    };
  }

  private AttributeType toAttributeType(XmlSchemaBaseSimpleType simpleType) {
    return switch (simpleType) {
      case BOOLEAN -> AttributeType.BOOLEAN;
      case DECIMAL, DOUBLE, FLOAT -> AttributeType.NUMBER;
      default -> AttributeType.STRING;
    };
  }

  private AttributeType resolveUnionType(XmlSchemaTypeInfo typeInfo) {
    assert typeInfo.getType().equals(XmlSchemaTypeInfo.Type.UNION);
    // Take the first type
    for (XmlSchemaTypeInfo childType : typeInfo.getChildTypes()) {
      if (childType.getBaseType() != null) {
        return toAttributeType(childType.getBaseType());
      }
    }
    return AttributeType.STRING;
  }

  private Restriction getRestriction(XmlSchemaTypeInfo typeInfo) {
    List<String> enumRestrictions = getEnumRestrictions(typeInfo);
    if (!enumRestrictions.isEmpty()) {
      return new Restriction.MultiValuedRestriction(
          Restriction.RestrictionType.ENUM, enumRestrictions);
    }

    if (typeInfo.getChildTypes() != null) {
      // Take the first restriction
      for (XmlSchemaTypeInfo childType : typeInfo.getChildTypes()) {
        Restriction restriction = getRestriction(childType);
        if (restriction != null) {
          return restriction;
        }
      }
    }

    return null;
  }

  private List<String> getEnumRestrictions(XmlSchemaTypeInfo typeInfo) {
    if (typeInfo.getFacets() == null) {
      return Collections.emptyList();
    }

    List<XmlSchemaRestriction> restrictions =
        typeInfo.getFacets().get(XmlSchemaRestriction.Type.ENUMERATION);

    if (restrictions == null) {
      return Collections.emptyList();
    }

    return restrictions.stream().map(restriction -> restriction.getValue().toString()).toList();
  }
}
