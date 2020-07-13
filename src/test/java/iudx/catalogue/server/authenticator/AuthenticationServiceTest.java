package iudx.catalogue.server.authenticator;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Properties;
import java.util.Random;

@ExtendWith(VertxExtension.class)
public class AuthenticationServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceTest.class);
    private static final Properties properties = new Properties();
    private static Vertx vertxObj;
    private static AuthenticationService authenticationService;

    /**
     * Initialize and start the auth service for testing.
     *
     * @param vertx       the vertx instance object. Injected by VertxExtension and not started in clustered mode.
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

    /**
     * Initialize and do a dummy call with a class 1 cert to the auth server
     *
     * @param testContext the context object injected by VertxExtension
     */
    @Test
    @DisplayName("Test if WebClient has been initialized correctly")
    public void testWebClientSetup(VertxTestContext testContext) {
        WebClient client = AuthenticationVerticle.createWebClient(vertxObj, properties, true);
        client.post(443, Constants.AUTH_SERVER_HOST, Constants.AUTH_CERTINFO_PATH).send(httpResponseAsyncResult -> {
            if (httpResponseAsyncResult.failed()) {
                logger.error("Cert info call to auth server failed");
                testContext.failNow(httpResponseAsyncResult.cause());
                return;
            }
            logger.info("Cert info call to auth server succeeded");
            testContext.completeNow();
        });
    }

    /**
     * Test the authInfo argument validation. First test with dummy token to see if the validation succeeds. And then
     * clear the authInfo object, and test to see if the validation fails since the token is missing. Does not check if
     * the token format itself is invalid, since that's the job of the auth server. Also check if operation field is
     * present. That field should contain a valid HTTP method value : POST | PUT | DELETE etc.
     *
     * @param testContext the context object injected by VertxExtension
     */
    @Test
    @DisplayName("Test authInfo validation of the tokenInterospect call")
    public void testTIPAuthInfoArgValidation(VertxTestContext testContext) {
        JsonObject authInfo = new JsonObject();
        String dummyToken = properties.getProperty(Constants.DUMMY_TOKEN_KEY);
        int rnd =  new Random().nextInt(HttpMethod.values().length);
        String dummyMethod = HttpMethod.values()[rnd].toString();
        authInfo.put("token", dummyToken);
        authInfo.put("operation", dummyMethod);
        try {
            AuthenticationServiceImpl.validateAuthInfo(authInfo);
            logger.info("Dummy token value: " + dummyToken);
            logger.info("Dummy operation value: " + dummyMethod);
            logger.info("Validation of proper auth info arg succeeded");
        } catch (IllegalArgumentException e) {
            logger.error("Valid auth info arg failed validation");
            testContext.failNow(e.getCause());
            return;
        }

        authInfo.clear();
        try {
            AuthenticationServiceImpl.validateAuthInfo(authInfo);
            logger.error("Empty authInfo without token/operation passed validation");
            testContext.failNow(new IllegalArgumentException());
            return;
        } catch (IllegalArgumentException e) {
            logger.info(e.getMessage());
            logger.info("Empty authInfo failed validation");
        }

        authInfo.clear();
        authInfo.put("token", dummyToken);
        authInfo.put("operation", "NONSENSE");
        try {
            AuthenticationServiceImpl.validateAuthInfo(authInfo);
            logger.error("Invalid operation in authInfo passed validation");
            testContext.failNow(new IllegalArgumentException());
            return;
        } catch (IllegalArgumentException e) {
            logger.info(e.getMessage());
            logger.info("Invalid operation in authInfo failed validation");
        }

        testContext.completeNow();
    }
}
