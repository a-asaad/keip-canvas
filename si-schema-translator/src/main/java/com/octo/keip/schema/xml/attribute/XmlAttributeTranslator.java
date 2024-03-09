package com.octo.keip.schema.xml.attribute;

import com.octo.keip.schema.model.eip.Attribute;
import com.octo.keip.schema.model.eip.AttributeType;
import com.octo.keip.schema.model.eip.Restriction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAnnotationItem;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlAttributeTranslator {

  private static final Pattern whitespacePattern = Pattern.compile("\\s+");

  private final AttributeTypeTranslator typeTranslator;
  private final AttributeRestrictionTranslator restrictionTranslator;

  public XmlAttributeTranslator(XmlSchema xmlSchema) {
    this.typeTranslator = new AttributeTypeTranslator(xmlSchema);
    this.restrictionTranslator = new AttributeRestrictionTranslator(xmlSchema);
  }

  public Attribute translate(XmlSchemaAttribute attribute) {
    // REMOVE: checks
    assert !XmlSchemaUse.PROHIBITED.equals(attribute.getUse());

    Attribute.Builder builder =
        new Attribute.Builder(attribute.getName(), getType(attribute))
            .defaultValue(attribute.getDefaultValue());

    if (XmlSchemaUse.REQUIRED.equals(attribute.getUse())) {
      builder.required(true);
    }

    String description = getDescription(attribute);
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
    }
    return null;
  }

  private String getDescription(XmlSchemaAttribute attribute) {
    XmlSchemaAnnotation annotation = attribute.getAnnotation();
    if (annotation != null && annotation.getItems() != null) {
      return annotation.getItems().stream()
          .map(this::getMarkup)
          .map(this::getTextContent)
          .collect(Collectors.joining(""));
    }
    return "";
  }

  private NodeList getMarkup(XmlSchemaAnnotationItem item) {
    return switch (item) {
      case XmlSchemaAppInfo appInfo -> appInfo.getMarkup();
      case XmlSchemaDocumentation doc -> doc.getMarkup();
      default -> throw new IllegalStateException("Unexpected value: " + item);
    };
  }

  private String getTextContent(NodeList nodeList) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      String content = node.getTextContent().trim();
      sb.append(whitespacePattern.matcher(content).replaceAll(" "));
    }
    return sb.toString();
  }
}
