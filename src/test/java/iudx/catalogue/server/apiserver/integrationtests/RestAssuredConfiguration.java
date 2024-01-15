package iudx.catalogue.server.apiserver.integrationtests;

import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static io.restassured.RestAssured.port;
import static io.restassured.RestAssured.proxy;
import static iudx.catalogue.server.authenticator.TokensForITs.*;

import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.authenticator.TokenSetup;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit5 extension to allow {@link RestAssured} configuration to be injected into all integration
 * tests using {@link ExtendWith}. Java properties can be passed in arguments when running the
 * integration tests to configure a host (<code>intTestHost</code>), port (<code>intTestPort</code>
 * ), proxy host (<code>intTestProxyHost</code>) and proxy port (<code>intTestProxyPort</code>).
 */
public class RestAssuredConfiguration implements BeforeAllCallback {
    private static final Logger LOGGER = LogManager.getLogger(RestAssuredConfiguration.class);

  @Override
  public void beforeAll(ExtensionContext context) {
   JsonObject config = Configuration.getConfiguration("./configs/config-test.json", 1);
   //String testHost = config.getString("ip");
    String authServerHost = config.getString("authServerHost");
    //String authUrl=config.getString("authUrl");
    String testHost = System.getProperty("intTestHost");

    if (testHost != null) {
      baseURI = "http://" + testHost;
    } else {
      baseURI = "http://localhost";
    }

    String testPort = System.getProperty("intTestPort");
  // String testPort = config.getString("httpPort");


    if (testPort != null) {
      port = Integer.parseInt(testPort);
    } else {
      port = 8080;
    }

    basePath = "/iudx/cat/v1";
    String dxAuthBasePath = "auth/v1";
    String authEndpoint = "https://"+ authServerHost + "/" + dxAuthBasePath +  "/token";
    String proxyHost = System.getProperty("intTestProxyHost");
    String proxyPort = System.getProperty("intTestProxyPort");

    JsonObject providerCredentials = config.getJsonObject("clientCredentials").getJsonObject("provider");
    String providerClientId = providerCredentials.getString("clientID");
    String providerClientSecret = providerCredentials.getString("clientSecret");

    JsonObject consumerCredentials = config.getJsonObject("clientCredentials").getJsonObject("consumer");
    String consumerClientId = consumerCredentials.getString("clientID");
    String consumerClientSecret = consumerCredentials.getString("clientSecret");

      if (proxyHost != null && proxyPort != null) {
          proxy(proxyHost, Integer.parseInt(proxyPort));
      }

    LOGGER.debug("baseURI="+baseURI);
    LOGGER.debug("setting up the tokens");
    TokenSetup.setupTokens(authEndpoint, providerClientId, providerClientSecret, consumerClientId, consumerClientSecret);

    // Wait for tokens to be available before proceeding
      waitForTokens();

         // LOGGER.debug();("done with setting up the tokens");

    enableLoggingOfRequestAndResponseIfValidationFails();
 }
    private void waitForTokens() {
        int maxAttempts = 5;
        int attempt = 0;

        // Keep trying to get tokens until they are available or max attempts are reached
        while ((cosAdminToken == null || adminToken == null || token == null) && attempt < maxAttempts) {
            LOGGER.debug("Waiting for tokens to be available. Attempt: " + (attempt + 1));
            // Introduce a delay between attempts
            try {
                Thread.sleep(1000); // Adjust the delay as needed
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            attempt++;
        }

        if (cosAdminToken == null || adminToken == null || token == null) {
            // Log an error or throw an exception if tokens are still not available
            throw new RuntimeException("Failed to retrieve tokens after multiple attempts.");
        } else {
            LOGGER.debug("Tokens are now available. Proceeding with RestAssured configuration.");
        }
    }
}
