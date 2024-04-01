package com.octo.keip.schema.http;

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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fetches remote XML schemas using HTTP and read them into an {@link XmlSchemaCollection}. */
public class XmlSchemaHttpClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlSchemaHttpClient.class);

  private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(20);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

  private final Map<String, URI> additionalSchemaLocations;

  private final HttpClient httpClient;

  public XmlSchemaHttpClient(Map<String, URI> additionalSchemaLocations) {
    this.additionalSchemaLocations = additionalSchemaLocations;
    this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECTION_TIMEOUT).build();
  }

  public XmlSchemaHttpClient(HttpClient httpClient, Map<String, URI> additionalSchemaLocations) {
    this.additionalSchemaLocations = additionalSchemaLocations;
    this.httpClient = httpClient;
  }

  public XmlSchemaCollection collect(String targetNamespace, URI schemaLocation)
      throws IOException, InterruptedException {
    LOGGER.info("Fetching target schema xml at: {}", schemaLocation);

    HttpRequest request =
        HttpRequest.newBuilder().uri(schemaLocation).timeout(REQUEST_TIMEOUT).GET().build();

    HttpResponse<InputStream> response = httpClient.send(request, BodyHandlers.ofInputStream());
    if (response.statusCode() == 200) {
      var schemaCollection = new XmlSchemaCollection();
      try (var reader = toReader(response.body())) {
        schemaCollection.read(reader);
        collectUnderDefinedImports(schemaCollection, targetNamespace);
        return schemaCollection;
      }
    }

    LOGGER.error("Response code: {}", response.statusCode());
    throw new RuntimeException("Failed to retrieve the target schema");
  }

  /**
   * Collect imported schemas that have no "schemaLocation" attribute defined. Instead, the
   * locations are provided by a user-supplied configuration.
   */
  private void collectUnderDefinedImports(
      XmlSchemaCollection schemaCollection, String targetNamespace) {
    Stream<URI> linkedUris =
        namespaceToURI(getImportsWithNoLocation(schemaCollection, targetNamespace));

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

  private Stream<String> getImportsWithNoLocation(
      XmlSchemaCollection schemaCollection, String targetNamespace) {
    XmlSchema target = schemaCollection.schemaForNamespace(targetNamespace);
    return target.getExternals().stream()
        .filter(ext -> isNullOrBlank(ext.getSchemaLocation()) && ext instanceof XmlSchemaImport)
        .map(ext -> ((XmlSchemaImport) ext).getNamespace());
  }

  private Stream<URI> namespaceToURI(Stream<String> namespaces) {
    return namespaces
        .filter(
            ns -> {
              if (!additionalSchemaLocations.containsKey(ns)) {
                LOGGER.warn("No matching URI was provided for linked namespace: {}", ns);
                return false;
              }
              return true;
            })
        .map(additionalSchemaLocations::get);
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

  private boolean isNullOrBlank(String str) {
    return str == null || str.isBlank();
  }
}
