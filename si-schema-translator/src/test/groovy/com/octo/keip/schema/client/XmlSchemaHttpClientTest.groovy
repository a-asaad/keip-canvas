package com.octo.keip.schema.client

import org.apache.ws.commons.schema.XmlSchemaCollection
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

import static org.apache.ws.commons.schema.XmlSchemaSerializer.XSD_NAMESPACE

class XmlSchemaHttpClientTest extends Specification {

    static final TARGET_NAMESPACE = "http://www.example.com/test-target"
    static final FIRST_NAMESPACE = "http://www.example.com/first"
    static final SECOND_NAMESPACE = "http://www.example.com/second"
    static final THIRD_NAMESPACE = "http://www.example.com/third"

    static final TARGET_URI = URI.create("http://localhost/test-target")

    Map<String, URI> userProvidedSchemaLocations = [
            (FIRST_NAMESPACE) : URI.create("http://localhost/first"),
            (SECOND_NAMESPACE): URI.create("http://localhost/second")
    ]

    HttpClient httpClient = defaultHttpClientMock()

    def xmlSchemaClient = new XmlSchemaHttpClient(httpClient, userProvidedSchemaLocations)

    def "Collect target and imported schemas success"() {
        when:
        def schemaCollection = xmlSchemaClient.collect(TARGET_NAMESPACE, TARGET_URI)

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, FIRST_NAMESPACE, SECOND_NAMESPACE, THIRD_NAMESPACE, XSD_NAMESPACE]
    }

    def "Collect target and caching imported schemas success"() {
        when:
        xmlSchemaClient.collect(TARGET_NAMESPACE, TARGET_URI)
        def schemaCollection = xmlSchemaClient.collect(TARGET_NAMESPACE, TARGET_URI)

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, FIRST_NAMESPACE, SECOND_NAMESPACE, THIRD_NAMESPACE, XSD_NAMESPACE]
    }

    def "Response from target-schema URI with error status code throws exception"() {
        given:
        def errorResponse = Mock(HttpResponse)
        errorResponse.statusCode() >> 404

        when:
        xmlSchemaClient.collect(TARGET_NAMESPACE, TARGET_URI)

        then:
        httpClient.send(_, _) >> errorResponse
        thrown(RuntimeException)
    }

    def "Sending target-schema request throws an exception, exception is uncaught"() {
        when:
        xmlSchemaClient.collect(TARGET_NAMESPACE, TARGET_URI)

        then:
        httpClient.send(_, _) >> { throw new ConnectException("server down") }
        thrown(ConnectException)
    }

    def "Sending imported schema http request throws an exception, exception is uncaught"() {
        when:
        xmlSchemaClient.collect(TARGET_NAMESPACE, TARGET_URI)

        then:
        httpClient.sendAsync(_, _) >> { throw new ConnectException("server down") }
        thrown(ConnectException)
    }

    def "When an import location is not provided in the user-supplied locations map, ignore the missing URI and collect the rest"() {
        given:
        userProvidedSchemaLocations.remove(FIRST_NAMESPACE)

        when:
        def schemaCollection = xmlSchemaClient.collect(TARGET_NAMESPACE, TARGET_URI)

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, SECOND_NAMESPACE, THIRD_NAMESPACE, XSD_NAMESPACE]
    }

    def "'Future' errors generated while fetching imported schemas are skipped. The remaining URIs are collected"() {
        given:
        def mockClient = minimalHttpClientMock()
        stubSendAsyncInteraction(mockClient,
                CompletableFuture.failedFuture(new ConnectException("imported schema fail")),
                CompletableFuture.completedFuture(mockHttpResponse("second.xml")))

        xmlSchemaClient = new XmlSchemaHttpClient(mockClient, userProvidedSchemaLocations)

        when:
        def schemaCollection = xmlSchemaClient.collect(TARGET_NAMESPACE, TARGET_URI)

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, SECOND_NAMESPACE, THIRD_NAMESPACE, XSD_NAMESPACE]
    }

    def "Imported schema http requests that return error response codes are skipped"() {
        given:
        def errorResponse = Mock(HttpResponse)
        errorResponse.statusCode() >> 404

        def mockClient = minimalHttpClientMock()
        stubSendAsyncInteraction(mockClient,
                CompletableFuture.completedFuture(errorResponse),
                CompletableFuture.completedFuture(mockHttpResponse("second.xml")))

        xmlSchemaClient = new XmlSchemaHttpClient(mockClient, userProvidedSchemaLocations)

        when:
        def schemaCollection = xmlSchemaClient.collect(TARGET_NAMESPACE, TARGET_URI)

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, SECOND_NAMESPACE, THIRD_NAMESPACE, XSD_NAMESPACE]
    }

    def "Error reading a successfully fetched schema response is skipped"() {
        given:
        def unreadableResponse = mockHttpResponse("first.xml")
        unreadableResponse.body().close()

        def mockClient = minimalHttpClientMock()
        stubSendAsyncInteraction(mockClient,
                CompletableFuture.completedFuture(unreadableResponse),
                CompletableFuture.completedFuture(mockHttpResponse("second.xml")))

        xmlSchemaClient = new XmlSchemaHttpClient(mockClient, userProvidedSchemaLocations)

        when:
        def schemaCollection = xmlSchemaClient.collect(TARGET_NAMESPACE, TARGET_URI)

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, SECOND_NAMESPACE, THIRD_NAMESPACE, XSD_NAMESPACE]
    }

    private HttpClient defaultHttpClientMock() {
        HttpClient httpClient = minimalHttpClientMock()
        def respFirst = CompletableFuture.completedFuture(mockHttpResponse("first.xml"))
        def respSecond = CompletableFuture.completedFuture(mockHttpResponse("second.xml"))
        stubSendAsyncInteraction(httpClient, respFirst, respSecond)
        return httpClient
    }

    private HttpClient minimalHttpClientMock() {
        HttpClient httpClient = Mock(HttpClient)
        httpClient.send(_, _) >>> [mockHttpResponse("test-target.xml"), mockHttpResponse("test-target.xml")]
        return httpClient
    }

    private void stubSendAsyncInteraction(HttpClient client, CompletableFuture<HttpResponse> responseForFirst, CompletableFuture<HttpResponse> responseForSecond) {
        client.sendAsync(_, _) >> { args -> return (args[0] as HttpRequest).uri().toString().endsWith("/first") ? responseForFirst : responseForSecond }
    }

    private HttpResponse<InputStream> mockHttpResponse(String schemaFilename) {
        HttpResponse<InputStream> schemaResponse = Mock(HttpResponse)
        schemaResponse.body() >> getXmlInputStream(schemaFilename)
        schemaResponse.statusCode() >> 200
        return schemaResponse
    }

    private static InputStream getXmlInputStream(String filename) {
        String path = Path.of("schemas", "xml", "http-client", filename).toString()
        return XmlSchemaHttpClientTest.class.getClassLoader().getResource(path).newInputStream()
    }

    private static getNamespaces(XmlSchemaCollection schemaCollection) {
        return schemaCollection.getXmlSchemas().collect { it::getTargetNamespace() }
    }
}
