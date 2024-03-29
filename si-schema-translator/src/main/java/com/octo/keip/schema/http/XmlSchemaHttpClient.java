package com.octo.keip.schema.http;

import static org.apache.ws.commons.schema.XmlSchemaSerializer.XSD_NAMESPACE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlSchemaHttpClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlSchemaHttpClient.class);

  private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(20);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

  private final Map<String, URI> schemaLocations;

  private final HttpClient httpClient;

  public XmlSchemaHttpClient(Map<String, URI> schemaLocations) {
    this.schemaLocations = schemaLocations;
    this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECTION_TIMEOUT).build();
  }

  public XmlSchemaHttpClient(HttpClient httpClient, Map<String, URI> schemaLocations) {
    this.schemaLocations = schemaLocations;
    this.httpClient = httpClient;
  }

  public XmlSchemaCollection collect(String targetNamespace)
      throws IOException, InterruptedException {
    URI xmlSchemaUri = schemaLocations.get(targetNamespace);
    if (xmlSchemaUri == null) {
      throw new IllegalArgumentException(
          "No matching URI was provided for target namespace: " + targetNamespace);
    }

    LOGGER.info("Fetching target schema xml at: {}", xmlSchemaUri);

    HttpRequest request =
        HttpRequest.newBuilder().uri(xmlSchemaUri).timeout(REQUEST_TIMEOUT).GET().build();

    HttpResponse<InputStream> response = httpClient.send(request, BodyHandlers.ofInputStream());
    if (response.statusCode() == 200) {
      var schemaCollection = new XmlSchemaCollection();
      try (var reader = toReader(response.body())) {
        schemaCollection.read(reader);
        collectLinkedSchemas(schemaCollection, targetNamespace);
        return schemaCollection;
      }
    }

    LOGGER.error("Response code: {}", response.statusCode());
    throw new RuntimeException("Failed to retrieve the target schema");
  }

  private Stream<String> filterLinkedNamespaces(
      XmlSchemaCollection schemaCollection, String targetNamespace) {
    // TODO: Add error check for targetNamespace
    XmlSchema target = schemaCollection.schemaForNamespace(targetNamespace);
    NamespacePrefixList namespaceContext = target.getNamespaceContext();
    return Arrays.stream(namespaceContext.getDeclaredPrefixes())
        .map(namespaceContext::getNamespaceURI)
        .filter(uriStr -> !uriStr.equals(XSD_NAMESPACE) && !uriStr.equals(targetNamespace));
  }

  private Stream<URI> namespaceToURI(Stream<String> namespaces) {
    // TODO: Check xsd:imports if no matching URI is explicitly provided
    return namespaces
        .filter(
            ns -> {
              if (!schemaLocations.containsKey(ns)) {
                LOGGER.warn("No matching URI was provided for linked namespace: {}", ns);
                return false;
              }
              return true;
            })
        .map(schemaLocations::get);
  }

  private void collectLinkedSchemas(XmlSchemaCollection schemaCollection, String targetNamespace) {
    Stream<URI> linkedUris =
        namespaceToURI(filterLinkedNamespaces(schemaCollection, targetNamespace));

    Stream<CompletableFuture<HttpResponse<InputStream>>> responseFutures =
        linkedUris.map(
            uri -> {
              LOGGER.info("Fetching: {}", uri);
              return httpClient
                  .sendAsync(
                      HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).GET().build(),
                      BodyHandlers.ofInputStream())
                  .handle(
                      (response, ex) -> {
                        if (ex != null) {
                          LOGGER.warn("Failed to collect schema at: {}", uri, ex);
                          return null;
                        }
                        if (response != null) {
                          readResponseIntoCollection(schemaCollection, response);
                        }
                        return response;
                      });
            });

    CompletableFuture.allOf(responseFutures.toArray(CompletableFuture[]::new)).join();
  }

  private void readResponseIntoCollection(
      XmlSchemaCollection schemaCollection, HttpResponse<InputStream> response) {
    if (response.statusCode() != 200) {
      LOGGER.warn(
          "Failed to retrieve xml schema at: '{}'. response code: '{}'",
          response.uri(),
          response.statusCode());
      return;
    }

    try (var reader = toReader(response.body())) {
      schemaCollection.read(reader);
    } catch (Exception e) {
      LOGGER.error("Unable to parse xml schema obtained from: {}", response.uri(), e);
    }
  }

  private BufferedReader toReader(InputStream is) {
    return new BufferedReader(new InputStreamReader(is));
  }
}
