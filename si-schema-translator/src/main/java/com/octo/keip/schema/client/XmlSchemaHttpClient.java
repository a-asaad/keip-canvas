package com.octo.keip.schema.client;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fetches remote XML schemas using HTTP and read them into an {@link XmlSchemaCollection}. */
public class XmlSchemaHttpClient implements XmlSchemaClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlSchemaHttpClient.class);

  private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(20);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

  private final Map<String, URI> additionalSchemaLocations;

  private final Map<URI, XmlSchema> fetchedSchemas = new HashMap<>();

  private final HttpClient httpClient;

  public XmlSchemaHttpClient(Map<String, URI> additionalSchemaLocations) {
    this.additionalSchemaLocations = additionalSchemaLocations;
    this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECTION_TIMEOUT).build();
  }

  XmlSchemaHttpClient(HttpClient httpClient, Map<String, URI> additionalSchemaLocations) {
    this.additionalSchemaLocations = additionalSchemaLocations;
    this.httpClient = httpClient;
  }

  @Override
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
    List<URI> fetchableImports =
        namespaceToURI(getImportsWithNoLocation(schemaCollection, targetNamespace)).toList();

    fetchableImports.stream()
        .filter(fetchedSchemas::containsKey)
        .forEach(uri -> collectCachedImports(schemaCollection, uri));

    Stream<CompletableFuture<HttpResponse<InputStream>>> responseFutures =
        fetchableImports.stream()
            .filter(Predicate.not(fetchedSchemas::containsKey))
            .map(uri -> fetchImports(schemaCollection, uri));

    CompletableFuture.allOf(responseFutures.toArray(CompletableFuture[]::new)).join();
  }

  private void collectCachedImports(XmlSchemaCollection schemaCollection, URI importLocation) {
    XmlSchema cached = fetchedSchemas.get(importLocation);
    if (cached != null) {
      try {
        schemaCollection.read(cached.getSchemaDocument());
      } catch (XmlSchemaSerializerException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private CompletableFuture<HttpResponse<InputStream>> fetchImports(
      XmlSchemaCollection schemaCollection, URI importLocation) {
    LOGGER.info("Fetching: {}", importLocation);
    return httpClient
        .sendAsync(
            HttpRequest.newBuilder(importLocation).timeout(REQUEST_TIMEOUT).GET().build(),
            BodyHandlers.ofInputStream())
        .handle(
            (response, ex) -> {
              if (ex != null) {
                LOGGER.warn("Failed to collect schema at: {}", importLocation, ex);
                return null;
              }
              if (response != null) {
                XmlSchema schema = readResponseIntoCollection(schemaCollection, response);
                if (schema != null) {
                  fetchedSchemas.put(importLocation, schema);
                }
              }
              return response;
            });
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

  private XmlSchema readResponseIntoCollection(
      XmlSchemaCollection schemaCollection, HttpResponse<InputStream> response) {
    if (response.statusCode() != 200) {
      LOGGER.warn(
          "Failed to retrieve xml schema at: '{}'. response code: '{}'",
          response.uri(),
          response.statusCode());
      return null;
    }

    try (var reader = toReader(response.body())) {
      return schemaCollection.read(reader);
    } catch (Exception e) {
      LOGGER.error("Unable to parse xml schema obtained from: {}", response.uri(), e);
    }
    return null;
  }

  private BufferedReader toReader(InputStream is) {
    return new BufferedReader(new InputStreamReader(is));
  }

  private boolean isNullOrBlank(String str) {
    return str == null || str.isBlank();
  }
}
