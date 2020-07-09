package iudx.catalogue.server.authenticator;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@ExtendWith(VertxExtension.class)
public class AuthenticationServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceTest.class);
    private static Vertx vertxObj;
    private static AuthenticationService authenticationService;
    private static final Properties properties = new Properties();

    /**
     * Initialize and start the auth service for testing.
     * @param vertx the vertx instance object. Injected by VertxExtension and not started in clustered mode.
     * @param testContext the context object injected by VertxExtension
     */
    @BeforeAll
    @DisplayName("Initialize Vertx and deploy AuthenticationVerticle")
    static void initialize(Vertx vertx, VertxTestContext testContext) {
        vertxObj = vertx;
        WebClient client = AuthenticationVerticle.createWebClient(vertxObj, properties, true);
        authenticationService = new AuthenticationServiceImpl(client);
        logger.info("Auth tests setup complete");
        testContext.completeNow();
    }

    @Test
    @DisplayName("Testing setup")
    public void shouldSucceed(VertxTestContext testContext) {
        logger.info("Default test is passing");
        testContext.completeNow();
    }

    @Test
    @DisplayName("Test if WebClient has been initialized correctly")
    public void testWebClientSetup(VertxTestContext testContext) {
//        WebClient client = AuthenticationVerticle.createWebClient(vertxObj, properties, true);

        try {
            FileInputStream configFile = new FileInputStream(Constants.CONFIG_FILE);
            if (properties.isEmpty()) properties.load(configFile);
        } catch (IOException e) {
            logger.error("Could not load properties from config file", e);
        }

        WebClientOptions webClientOptions = new WebClientOptions();
        webClientOptions
                .setTrustAll(true)
                .setSsl(true)
                .setKeyStoreOptions(new JksOptions()
                        .setPath(properties.getProperty(Constants.KEYSTORE_PATH))
                        .setPassword(properties.getProperty(Constants.KEYSTORE_PASSWORD)));
        WebClient client = WebClient.create(vertxObj, webClientOptions);

        client.post(Constants.AUTH_SERVER_HOST, Constants.AUTH_CERTINFO_PATH).send(httpResponseAsyncResult -> {
            if (httpResponseAsyncResult.failed()) {
                logger.error("Cert info call to auth server failed");
                testContext.failNow(httpResponseAsyncResult.cause());
                return;
            }
            logger.info("Cert info call to auth server succeeded");
            logger.info("Response status code: " + httpResponseAsyncResult.result().statusCode());
            JsonObject responseBody = httpResponseAsyncResult.result().bodyAsJsonObject();
            logger.info("Response body: " + responseBody.toString());
            testContext.completeNow();
        });
    }
}
