package iudx.catalogue.server.apiserver.integrationtests;

import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.Configuration;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static io.restassured.RestAssured.port;
import static io.restassured.RestAssured.proxy;

/**
 * JUnit5 extension to allow {@link RestAssured} configuration to be injected into all integration
 * tests using {@link ExtendWith}. Java properties can be passed in arguments when running the
 * integration tests to configure a host (<code>intTestHost</code>), port (<code>intTestPort</code>
 * ), proxy host (<code>intTestProxyHost</code>) and proxy port (<code>intTestProxyPort</code>).
 */
public class RestAssuredConfiguration implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    JsonObject config = Configuration.getConfiguration("./configs/config-test.json", 3);
    String testHost = config.getString("ip");

    if (testHost != null) {
      baseURI = "http://" + testHost;
    } else {
      baseURI = "http://localhost";
    }

    String testPort = config.getString("httpPort");

    if (testPort != null) {
      port = Integer.parseInt(testPort);
    } else {
      port = 8443;
    }

    basePath = "/iudx/cat/v1";

//    String proxyHost = System.getProperty("intTestProxyHost");
//    String proxyPort = System.getProperty("intTestProxyPort");
//
//    if (proxyHost != null && proxyPort != null) {
//      proxy(proxyHost, Integer.parseInt(proxyPort));
//    }

    enableLoggingOfRequestAndResponseIfValidationFails();
  }
}