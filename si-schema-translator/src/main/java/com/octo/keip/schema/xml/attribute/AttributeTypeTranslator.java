package com.octo.keip.schema.xml.attribute;

import static org.apache.ws.commons.schema.XmlSchemaSerializer.XSD_NAMESPACE;

import com.octo.keip.schema.model.eip.AttributeType;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeUnion;

public class AttributeTypeTranslator implements AttributeTranslator<AttributeType> {

  private final XmlSchema xmlSchema;

  public AttributeTypeTranslator(XmlSchema xmlSchema) {
    this.xmlSchema = xmlSchema;
  }

  @Override
  public AttributeType apply(XmlSchemaSimpleTypeList list) {
    throw new RuntimeException("TODO: Not sure how to handle yet");
  }

  @Override
  public AttributeType apply(XmlSchemaSimpleTypeRestriction restriction) {
    return AttributeType.STRING;
  }

  @Override
  public AttributeType apply(XmlSchemaSimpleTypeUnion union) {
    QName[] qNames = union.getMemberTypesQNames();
    for (var qName : qNames) {
      if (XSD_NAMESPACE.equals(qName.getNamespaceURI())) {
        return AttributeType.of(qName.getLocalPart());
      }
      return apply(((XmlSchemaSimpleType) this.xmlSchema.getTypeByName(qName)).getContent());
    }
    // TODO: remove
    throw new RuntimeException("Could not resolve Attribute Type for " + union.getId());
  }
}
