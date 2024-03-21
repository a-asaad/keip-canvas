package com.octo.keip.schema.xml;

import com.octo.keip.schema.model.eip.ChildGroup;
import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.EipSchema;
import com.octo.keip.schema.xml.visitor.EipTranslationVisitor;
import java.util.Set;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.walker.XmlSchemaWalker;

// TODO: Add some comments
public class SiSchemaTranslator {

  // TODO: Pass in as a ctor arg
  private final Set<String> EXCLUDED_COMPONENTS = Set.of("selector-chain");

  private final ChildGroupReducer groupReducer = new ChildGroupReducer();

  public EipSchema apply(String namespace, XmlSchemaCollection schemaCollection, XmlSchema target) {
    return translate(namespace, schemaCollection, target);
  }

  // TODO: Refactor
  private EipSchema translate(
      String namespace, XmlSchemaCollection schemaCol, XmlSchema xmlSchema) {
    var eipSchema = new EipSchema();

    var eipVisitor = new EipTranslationVisitor();
    var schemaWalker = new XmlSchemaWalker(schemaCol);
    schemaWalker.addVisitor(eipVisitor);

    for (XmlSchemaElement element : xmlSchema.getElements().values()) {

      if (EXCLUDED_COMPONENTS.contains(element.getName())) {
        continue;
      }

      // TODO: Should visitor be reset instead of created everytime?
      // TODO: Should walker be cleared?
      eipVisitor.reset();
      schemaWalker.walk(element);

      EipComponent.Builder eipComponentBuilder = eipVisitor.getEipComponent();

      ChildGroup reduced = groupReducer.reduceGroup(eipComponentBuilder.build().getChildGroup());
      eipSchema.addComponent(namespace, eipComponentBuilder.childGroup(reduced).build());

      // TODO: Remove
      //      System.out.println("Component: " + element.getName());
      //      EipTranslationVisitor.printTree(reduced, "");
      //      System.out.println();

      //      eipSchema.addComponent(namespace, eipComponentBuilder.build());
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
