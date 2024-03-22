package com.octo.keip.schema.xml;

import com.octo.keip.schema.model.eip.ChildGroup;
import com.octo.keip.schema.model.eip.EipChildElement;
import com.octo.keip.schema.model.eip.EipComponent;
import com.octo.keip.schema.model.eip.EipElement;
import com.octo.keip.schema.model.eip.EipSchema;
import com.octo.keip.schema.xml.visitor.EipTranslationVisitor;
import java.util.Objects;
import java.util.Set;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.walker.XmlSchemaWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Add some comments
// TODO: Add some error handling
// TODO: Add some logging
public class SiSchemaTranslator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SiSchemaTranslator.class);

  // TODO: Pass in as a ctor arg. Make externally configurable.
  // TODO: Mostly excluded because of 'bean' element child reduction issue. Investigate solutions.
  private final Set<String> EXCLUDED_COMPONENTS =
      Set.of("selector-chain", "spel-property-accessors", "converter", "chain");

  // TODO: Pass in as a ctor arg. Make externally configurable.
  private final Set<QName> SKIP_CHILD_REDUCE =
      Set.of(new QName("http://www.springframework.org/schema/beans", "bean"));

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
        LOGGER.debug("Skipping component: {}", element.getName());
        continue;
      }

      LOGGER.debug("Translating component: {}", element.getName());

      try {
        // TODO: Should visitor be reset instead of created everytime?
        // TODO: Should walker be cleared?
        // TODO: Make more robust against StackOverflow errors
        eipVisitor.reset();
        schemaWalker.walk(element);

        EipComponent.Builder eipComponentBuilder = eipVisitor.getEipComponent();

        ChildGroup reduced = groupReducer.reduceGroup(eipComponentBuilder.build().getChildGroup());
        eipSchema.addComponent(namespace, eipComponentBuilder.childGroup(reduced).build());
      } catch (Exception e) {
        throw new RuntimeException(
            String.format("Error translating component: %s", element.getName()), e);
      }

      // TODO: Remove
      //      System.out.println("Component: " + element.getName());
      //      EipTranslationVisitor.printTree(reduced, "");
      //      System.out.println();

      //      eipSchema.addComponent(namespace, eipComponentBuilder.build());
    }

    // xsd:any (namespace="##other"), most likely refer to beans.
    assert isTopLevelFreeOfNestedGroups(eipSchema, namespace);
    return eipSchema;
  }

  private boolean isTopLevelFreeOfNestedGroups(EipSchema eipSchema, String namespace) {
    return eipSchema.toMap().get(namespace).stream()
        .map(EipElement::getChildGroup)
        .filter(Objects::nonNull)
        .flatMap(c -> c.children().stream())
        .allMatch(c -> c instanceof EipChildElement);
  }
}
