package com.octo.keip.schema.xml.attribute;

import static org.apache.ws.commons.schema.XmlSchemaSerializer.XSD_NAMESPACE;

import com.octo.keip.schema.model.eip.Restriction;
import com.octo.keip.schema.model.eip.Restriction.MultiValuedRestriction;
import com.octo.keip.schema.model.eip.Restriction.RestrictionType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
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
    assert restriction.getBaseType() == null;

    List<String> enums =
        Stream.concat(getBaseEnums(restriction).stream(), getEnums(restriction).stream()).toList();
    return enums.isEmpty() ? null : new MultiValuedRestriction(RestrictionType.ENUM, enums);
  }

  // TODO: Similar logic to getRestriction and extractBaseTypes. See if you can extract.
  // Recursion might not be necessary here if we can guarantee that the base type is always a
  // restriction.
  private List<String> getBaseEnums(XmlSchemaSimpleTypeRestriction restriction) {
    if (restriction.getBaseTypeName() != null
        && !restriction.getBaseTypeName().getNamespaceURI().equals(XSD_NAMESPACE)) {
      XmlSchemaSimpleType schemaType =
          (XmlSchemaSimpleType) this.xmlSchema.getTypeByName(restriction.getBaseTypeName());
      XmlSchemaSimpleTypeRestriction baseRestriction =
          (XmlSchemaSimpleTypeRestriction) schemaType.getContent();
      return getEnums(baseRestriction);
    }
    return Collections.emptyList();
  }

  private List<String> getEnums(XmlSchemaSimpleTypeRestriction restriction) {
    return restriction.getFacets().stream()
        .filter(facet -> facet instanceof XmlSchemaEnumerationFacet)
        .map(facet -> facet.getValue().toString())
        .toList();
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
