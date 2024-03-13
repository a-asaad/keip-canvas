package com.octo.keip.schema.xml;

import com.octo.keip.schema.model.eip.Attribute;
import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.EipSchema;
import com.octo.keip.schema.model.eip.FlowType;
import com.octo.keip.schema.model.eip.Role;
import com.octo.keip.schema.xml.attribute.AnnotationTranslator;
import com.octo.keip.schema.xml.attribute.XmlAttributeIterator;
import com.octo.keip.schema.xml.attribute.XmlAttributeTranslator;
import java.io.Reader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaType;

public class SiSchemaTranslator {

  private static final String DEFAULT_NAMESPACE_URI = "http://www.example.com/schema/default";

  public EipSchema apply(String namespace, Reader xmlReader) {
    var schemaCol = new XmlSchemaCollection();
    XmlSchema xmlSchema = schemaCol.read(xmlReader);
    return translate(namespace, xmlSchema);
  }

  private EipSchema translate(String namespace, XmlSchema xmlSchema) {
    // TODO: Should iterator and translator be combined? Or have the translator depend on the
    // iterator?
    var iterator = new XmlAttributeIterator(xmlSchema);
    var translator = new XmlAttributeTranslator(xmlSchema);

    var eipSchema = new EipSchema();

    for (XmlSchemaElement element : xmlSchema.getElements().values()) {
      XmlSchemaType schemaType = element.getSchemaType();
      // TODO: Branch for simple vs. complex
      if (!(schemaType instanceof XmlSchemaComplexType schemaComplexType)) {
        throw new IllegalStateException("SimpleTypes not supported yet");
      }

      assert !schemaType.isMixed();

      Stream<XmlSchemaAttribute> attributeStream = iterator.streamAttributes(schemaComplexType);
      Set<Attribute> eipAttributes =
          attributeStream.map(translator::translate).collect(Collectors.toSet());

      // TODO: Figure out how to get flowtype and role.
      EipComponent.Builder componentBuilder =
          new EipComponent.Builder(element.getName(), Role.ENDPOINT, FlowType.SOURCE)
              .attributes(eipAttributes);

      String description = AnnotationTranslator.getDescription(element.getAnnotation());
      if (!description.isBlank()) {
        componentBuilder.description(description);
      }
      eipSchema.addComponent(namespace, componentBuilder.build());
    }

    // getParticle -> XmlSchemaParticle
    // getParticle or getContentTypeParticle?
    // XmlSchemaParticle -> element, group, or groupRef (ignore any?)
    // getContentModel -> 4 types (simple/complex x extension/restriction)
    // element -> get info
    // group -> all, choice, or sequence (handle appropriately)
    // xsd:any (namespace="##other"), most likely refer to beans.
    return eipSchema;
  }
}
