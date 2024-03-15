package com.octo.keip.schema.xml;

import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.EipSchema;
import com.octo.keip.schema.xml.attribute.AnnotationTranslator;
import java.io.Reader;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.walker.XmlSchemaWalker;

public class SiSchemaTranslator {

  private static final String DEFAULT_NAMESPACE_URI = "http://www.example.com/schema/default";

  public EipSchema apply(String namespace, Reader xmlReader) {
    var schemaCol = new XmlSchemaCollection();
    XmlSchema xmlSchema = schemaCol.read(xmlReader);
    return translate(namespace, schemaCol, xmlSchema);
  }

  private EipSchema translate(
      String namespace, XmlSchemaCollection schemaCol, XmlSchema xmlSchema) {
    var eipSchema = new EipSchema();

    var eipVisitor = new EipTranslationVisitor();
    var schemaWalker = new XmlSchemaWalker(schemaCol);
    schemaWalker.addVisitor(eipVisitor);

    for (XmlSchemaElement element : xmlSchema.getElements().values()) {

      // TODO: Should visitor be reset instead of created everytime?
      // TODO: Should walker be cleared?
      eipVisitor.reset();
      schemaWalker.walk(element);

      EipComponent.Builder eipComponentBuilder = eipVisitor.getEipComponent();

//      System.out.println(element.getName());
//      EipTranslationVisitor.printTree(eipComponentBuilder.build().getChildGroup(), "");

      // TODO: Figure out how to get flowtype and role.
      // TODO: Extract
      //      EipComponent.Builder componentBuilder =
      //          new EipComponent.Builder(element.getName(), Role.ENDPOINT, FlowType.SOURCE)
      //              .attributes(eipAttributes);

      String description = AnnotationTranslator.getDescription(element.getAnnotation());
      if (!description.isBlank()) {
        eipComponentBuilder.description(description);
      }
      eipSchema.addComponent(namespace, eipComponentBuilder.build());
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
