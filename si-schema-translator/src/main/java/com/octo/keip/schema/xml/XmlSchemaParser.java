package com.octo.keip.schema.xml;

import java.io.Reader;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchemaCollection;

public class XmlSchemaParser {

  public void parse(Reader r) {
    var schemaCol = new XmlSchemaCollection();
    var schema = schemaCol.read(r);
    var adapterElement =
        schema.getElementByName(
            new QName(
                "http://www.springframework.org/schema/integration", "inbound-channel-adapter"));
    int i = 5;
  }
}
