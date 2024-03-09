package com.octo.keip.schema.xml;

import com.google.gson.GsonBuilder;
import com.octo.keip.schema.model.eip.Attribute;
import com.octo.keip.schema.model.eip.EipSchema;
import com.octo.keip.schema.xml.attribute.XmlAttributeIterator;
import com.octo.keip.schema.xml.attribute.XmlAttributeTranslator;
import java.io.Reader;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeOrGroupRef;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;

public class SiSchemaTranslator {

  private static final String DEFAULT_NAMESPACE_URI = "http://www.example.com/schema/default";

  public EipSchema apply(String namespace, Reader xmlReader) {
    var schemaCol = new XmlSchemaCollection();
    XmlSchema xmlSchema = schemaCol.read(xmlReader);
    return translate(xmlSchema);
  }

  private EipSchema translate(XmlSchema xmlSchema) {
    XmlSchemaElement element =
        xmlSchema.getElementByName(new QName(DEFAULT_NAMESPACE_URI, "inbound-channel-adapter"));
    XmlSchemaComplexType adapterType = (XmlSchemaComplexType) element.getSchemaType();
    // schemaType -> simple or complex
    assert !adapterType.isMixed();

    List<XmlSchemaAttributeOrGroupRef> attributes = adapterType.getAttributes();

    // attribute -> Attribute: get info
    // attribute -> GroupRef: get info for each attribute in group from ref
    var iterator = new XmlAttributeIterator(xmlSchema);
    Stream<XmlSchemaAttribute> attributeStream = iterator.visitXmlAttributes(adapterType);

    var translator = new XmlAttributeTranslator(xmlSchema);
    List<Attribute> eipAttributes = attributeStream.map(translator::translate).toList();

    // getParticle -> XmlSchemaParticle
    // XmlSchemaParticle -> element, group, or groupRef (ignore any)
    // element -> get info
    // group -> all, choice, or sequence (handle appropriately)
    var gson = new GsonBuilder().setPrettyPrinting().create();
    System.out.println(gson.toJson(eipAttributes));
    return new EipSchema();
  }
}
