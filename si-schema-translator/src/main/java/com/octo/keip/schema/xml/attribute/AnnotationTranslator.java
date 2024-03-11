package com.octo.keip.schema.xml.attribute;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAnnotationItem;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AnnotationTranslator {

  private static final Pattern whitespacePattern = Pattern.compile("\\s+");

  // TODO: Handle the appInfo/tool annotations
  public static String getDescription(XmlSchemaAnnotation annotation) {
    if (annotation != null && annotation.getItems() != null) {
      return annotation.getItems().stream()
          .map(AnnotationTranslator::getMarkup)
          .map(AnnotationTranslator::getTextContent)
          .collect(Collectors.joining(""));
    }
    return "";
  }

  private static NodeList getMarkup(XmlSchemaAnnotationItem item) {
    return switch (item) {
      case XmlSchemaAppInfo appInfo -> appInfo.getMarkup();
      case XmlSchemaDocumentation doc -> doc.getMarkup();
      default -> throw new IllegalStateException("Unexpected value: " + item);
    };
  }

  private static String getTextContent(NodeList nodeList) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      String content = node.getTextContent().trim();
      sb.append(whitespacePattern.matcher(content).replaceAll(" "));
    }
    return sb.toString();
  }
}
