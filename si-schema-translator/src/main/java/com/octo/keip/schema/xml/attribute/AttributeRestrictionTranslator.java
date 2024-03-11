package com.octo.keip.schema.xml.attribute;

import static org.apache.ws.commons.schema.XmlSchemaSerializer.XSD_NAMESPACE;

import com.octo.keip.schema.model.eip.Restriction;
import com.octo.keip.schema.model.eip.Restriction.MultiValuedRestriction;
import com.octo.keip.schema.model.eip.Restriction.RestrictionType;

import java.util.List;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeUnion;

public class AttributeRestrictionTranslator implements AttributeTypeContentTranslator<Restriction> {

  private final XmlSchema xmlSchema;

  public AttributeRestrictionTranslator(XmlSchema xmlSchema) {
    this.xmlSchema = xmlSchema;
  }

  @Override
  public Restriction apply(XmlSchemaSimpleTypeList list) {
    throw new RuntimeException("TODO: Not sure how to handle yet");
  }

  @Override
  public Restriction apply(XmlSchemaSimpleTypeRestriction restriction) {
    // TODO: Should this logic be moved to the Restriction classes?
    // TODO: Also consider handling other xsd restriction types.
    List<String> enums =
        restriction.getFacets().stream()
            .filter(facet -> facet instanceof XmlSchemaEnumerationFacet)
            .map(facet -> facet.getValue().toString())
            .toList();

    return enums.isEmpty()
        ? null
        : new MultiValuedRestriction(RestrictionType.ENUM, enums);
  }

  @Override
  public Restriction apply(XmlSchemaSimpleTypeUnion union) {
    QName[] qNames = union.getMemberTypesQNames();
    for (var qName : qNames) {
      if (XSD_NAMESPACE.equals(qName.getNamespaceURI())) {
        continue;
      }
      return apply(((XmlSchemaSimpleType) this.xmlSchema.getTypeByName(qName)).getContent());
    }
    return null;
  }
}
