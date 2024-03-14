package com.octo.keip.schema.xml;

import com.octo.keip.schema.model.eip.Attribute;
import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.EipSchema;
import com.octo.keip.schema.model.eip.FlowType;
import com.octo.keip.schema.model.eip.Role;
import com.octo.keip.schema.xml.attribute.AnnotationTranslator;
import java.io.Reader;
import java.util.Set;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.walker.XmlSchemaWalker;

public class SiSchemaTranslator {

  private static final String DEFAULT_NAMESPACE_URI = "http://www.example.com/schema/default";

  public EipSchema apply(String namespace, Reader xmlReader) {
    var schemaCol = new XmlSchemaCollection();
    XmlSchema xmlSchema = schemaCol.read(xmlReader);
    var schemaWalker = new XmlSchemaWalker(schemaCol);
    return translate(namespace, xmlSchema, schemaWalker);
  }

  private EipSchema translate(String namespace, XmlSchema xmlSchema, XmlSchemaWalker schemaWalker) {
    var eipSchema = new EipSchema();

    for (XmlSchemaElement element : xmlSchema.getElements().values()) {

      // TODO: Should visitor be reset instead of created everytime?
      var eipVisitor = new EipTranslationVisitor();
      schemaWalker.addVisitor(eipVisitor);
      schemaWalker.walk(element);
      schemaWalker.removeVisitor(eipVisitor);

      Set<Attribute> eipAttributes = eipVisitor.getAttributes();

      // TODO: Figure out how to get flowtype and role.
      // TODO: Extract
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
