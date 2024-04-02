package com.octo.keip.schema.client;

import java.net.URI;
import org.apache.ws.commons.schema.XmlSchemaCollection;

public interface XmlSchemaClient {

  XmlSchemaCollection collect(String targetNamespace, URI schemaLocation) throws Exception;
}
