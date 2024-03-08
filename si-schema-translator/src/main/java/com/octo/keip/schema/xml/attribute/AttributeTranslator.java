package com.octo.keip.schema.xml.attribute;

import org.apache.ws.commons.schema.XmlSchemaSimpleTypeContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeUnion;

// TODO: Revisit. Kind of taking over from the JVM here, still preferred to XmlSchema wrappers at
// the moment.
public interface AttributeTranslator<T> {

  default T apply(XmlSchemaSimpleTypeContent content) {
    return switch (content) {
      case XmlSchemaSimpleTypeList list -> apply(list);
      case XmlSchemaSimpleTypeRestriction restriction -> apply(restriction);
      case XmlSchemaSimpleTypeUnion union -> apply(union);
        // TODO: Not ideal to throw exception here.
      default -> throw new IllegalStateException("Unexpected value: " + content);
    };
  }

  T apply(XmlSchemaSimpleTypeList list);

  T apply(XmlSchemaSimpleTypeRestriction restriction);

  T apply(XmlSchemaSimpleTypeUnion union);
}
