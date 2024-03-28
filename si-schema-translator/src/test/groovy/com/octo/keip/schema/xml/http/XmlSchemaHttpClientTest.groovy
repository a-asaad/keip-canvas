package com.octo.keip.schema.xml.http

import com.octo.keip.schema.http.XmlSchemaHttpClient
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
    static final FIRST_LINKED_NAMESPACE = "http://www.example.com/first"
    static final SECOND_LINKED_NAMESPACE = "http://www.example.com/second"

    Map<String, URI> schemaLocations = [(TARGET_NAMESPACE)       : URI.create("http://localhost/test-target"),
                                        (FIRST_LINKED_NAMESPACE) : URI.create("http://localhost/first"),
                                        (SECOND_LINKED_NAMESPACE): URI.create("http://localhost/second"),]

    HttpClient httpClient = defaultHttpClientMock()

    def xmlSchemaClient = new XmlSchemaHttpClient(httpClient, schemaLocations)

    def "Collect target and linked schemas success"() {
        when:
        def schemaCollection = xmlSchemaClient.collect(TARGET_NAMESPACE)

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, FIRST_LINKED_NAMESPACE, SECOND_LINKED_NAMESPACE, XSD_NAMESPACE]
    }

    def "Collect with missing target schema location throws exception"() {
        given:
        schemaLocations.remove(TARGET_NAMESPACE)

        when:
        xmlSchemaClient.collect(TARGET_NAMESPACE)

        then:
        thrown(IllegalArgumentException)
    }

    def "Response from target-schema URI with error status code throws exception"() {
        given:
        def errorResponse = Mock(HttpResponse)
        errorResponse.statusCode() >> 404

        when:
        xmlSchemaClient.collect(TARGET_NAMESPACE)

        then:
        httpClient.send(_, _) >> errorResponse
        thrown(RuntimeException)
    }

    def "Sending target-schema request throws an exception, exception is uncaught"() {
        when:
        xmlSchemaClient.collect(TARGET_NAMESPACE)

        then:
        httpClient.send(_, _) >> { throw new ConnectException("server down") }
        thrown(ConnectException)
    }

    def "Sending linked schema request throws an exception, exception is uncaught"() {
        when:
        xmlSchemaClient.collect(TARGET_NAMESPACE)

        then:
        httpClient.sendAsync(_, _) >> { throw new ConnectException("server down") }
        thrown(ConnectException)
    }

    def "Collect with missing linked schema location ignores missing URI and collects the remaining URIs"() {
        given:
        schemaLocations.remove(FIRST_LINKED_NAMESPACE)

        when:
        def schemaCollection = xmlSchemaClient.collect(TARGET_NAMESPACE)

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, SECOND_LINKED_NAMESPACE, XSD_NAMESPACE]
    }

    def "'Future' errors generated while fetching linked schemas are skipped. The remaining URIs are collected"() {
        given:
        def mockClient = minimalHttpClientMock()
        stubSendAsyncInteraction(mockClient,
                CompletableFuture.failedFuture(new ConnectException("linked schema fail")),
                CompletableFuture.completedFuture(mockHttpResponse("second.xml")))

        xmlSchemaClient = new XmlSchemaHttpClient(mockClient, schemaLocations)

        when:
        def schemaCollection = xmlSchemaClient.collect(TARGET_NAMESPACE)

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, SECOND_LINKED_NAMESPACE, XSD_NAMESPACE]
    }

    def "Linked schema requests that return error response codes are skipped"() {
        given:
        def errorResponse = Mock(HttpResponse)
        errorResponse.statusCode() >> 404

        def mockClient = minimalHttpClientMock()
        stubSendAsyncInteraction(mockClient,
                CompletableFuture.completedFuture(errorResponse),
                CompletableFuture.completedFuture(mockHttpResponse("second.xml")))

        xmlSchemaClient = new XmlSchemaHttpClient(mockClient, schemaLocations)

        when:
        def schemaCollection = xmlSchemaClient.collect(TARGET_NAMESPACE)

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, SECOND_LINKED_NAMESPACE, XSD_NAMESPACE]
    }

    def "Error reading a successful linked schema response is skipped"() {
        given:
        def unreadableResponse = mockHttpResponse("first.xml")
        unreadableResponse.body().close()

        def mockClient = minimalHttpClientMock()
        stubSendAsyncInteraction(mockClient,
                CompletableFuture.completedFuture(unreadableResponse),
                CompletableFuture.completedFuture(mockHttpResponse("second.xml")))

        xmlSchemaClient = new XmlSchemaHttpClient(mockClient, schemaLocations)

        when:
        def schemaCollection = xmlSchemaClient.collect(TARGET_NAMESPACE)

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, SECOND_LINKED_NAMESPACE, XSD_NAMESPACE]
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
        httpClient.send(_, _) >> mockHttpResponse("test-target.xml")
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
