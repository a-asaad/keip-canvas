package com.octo.keip.schema.xml.attribute;

import static org.apache.ws.commons.schema.XmlSchemaSerializer.XSD_NAMESPACE;

import com.octo.keip.schema.model.eip.Attribute;
import com.octo.keip.schema.model.eip.AttributeType;
import com.octo.keip.schema.model.eip.Restriction;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaUse;

public class XmlAttributeTranslator {

  private final XmlSchema xmlSchema;
  private final AttributeTypeTranslator typeTranslator;
  private final AttributeRestrictionTranslator restrictionTranslator;

  public XmlAttributeTranslator(XmlSchema xmlSchema) {
    this.xmlSchema = xmlSchema;
    this.typeTranslator = new AttributeTypeTranslator(xmlSchema);
    this.restrictionTranslator = new AttributeRestrictionTranslator(xmlSchema);
  }

  public Attribute translate(XmlSchemaAttribute attribute) {
    // TODO: review asserts
    assert !XmlSchemaUse.PROHIBITED.equals(attribute.getUse());

    Attribute.Builder builder =
        new Attribute.Builder(attribute.getName(), getType(attribute))
            .defaultValue(attribute.getDefaultValue());

    if (XmlSchemaUse.REQUIRED.equals(attribute.getUse())) {
      builder.required(true);
    }

    String description = AnnotationTranslator.getDescription(attribute.getAnnotation());
    if (!description.isBlank()) {
      builder.description(description);
    }

    Restriction restriction = getRestriction(attribute);
    if (restriction != null) {
      builder.restriction(restriction);
    }

    return builder.build();
  }

  private AttributeType getType(XmlSchemaAttribute attribute) {
    if (attribute.getSchemaType() != null) {
      return this.typeTranslator.apply(attribute.getSchemaType().getContent());
    }
    return AttributeType.of(attribute.getSchemaTypeName().getLocalPart().toUpperCase());
  }

  private Restriction getRestriction(XmlSchemaAttribute attribute) {
    if (attribute.getSchemaType() != null) {
      return this.restrictionTranslator.apply(attribute.getSchemaType().getContent());
    } else if (attribute.getSchemaTypeName() != null
        && !XSD_NAMESPACE.equals(attribute.getSchemaTypeName().getNamespaceURI())) {
      // TODO: Is the case safe?
      // TODO: Can this type lookup logic be extracted?
      XmlSchemaSimpleType schemaType =
          (XmlSchemaSimpleType) this.xmlSchema.getTypeByName(attribute.getSchemaTypeName());
      return this.restrictionTranslator.apply(schemaType.getContent());
    }
    return null;
  }
}
