package iudx.catalogue.server.authenticator;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
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
     * the token itself is invalid, since that's the job of the auth server.
     *
     * @param testContext the context object injected by VertxExtension
     */
    @Test
    @DisplayName("Test authInfo validation of the tokenInterospect call")
    public void testTIPAuthInfoArgValidation(VertxTestContext testContext) {
        JsonObject authInfo = new JsonObject();
        String dummyToken = properties.getProperty(Constants.DUMMY_TOKEN_KEY);
        authInfo.put("token", dummyToken);
        try {
            AuthenticationServiceImpl.validateAuthInfo(authInfo);
            logger.info("Dummy token value: " + dummyToken);
            logger.info("Validation of proper auth info arg succeeded");
        } catch (IllegalArgumentException e) {
            logger.error("Valid auth info arg failed validation");
            testContext.failNow(e.getCause());
            return;
        }

        authInfo.clear();
        try {
            AuthenticationServiceImpl.validateAuthInfo(authInfo);
            logger.error("Empty authInfo without token passed validation");
            testContext.failNow(new IllegalArgumentException());
            return;
        } catch (IllegalArgumentException e) {
            logger.info(e.getMessage());
            logger.info("Empty authInfo failed validation");
        }

        testContext.completeNow();
    }

    /**
     * Test the request argument validation method. The validations required are:
     * 1. Check if the request object is empty and throw error if true
     * 2. Check if type array is missing or empty and throw error is true
     * 3. Check if type array contains the valid item string: "iudx:Resource", and throw error if false
     * 4. If ID is missing or blank, check if name + resourceGroup fields are missing or blank, and throw error if true
     *
     * @param testContext the context object injected by VertxExtension
     */
    @Test
    @DisplayName("Test resource item of the tokenInterospect call")
    public void testTIPResourceItemArgsValidation(VertxTestContext testContext) {
        JsonObject request = new JsonObject();

        try {
            AuthenticationServiceImpl.validateRequest(request);
            logger.error("Empty request passed validation");
            testContext.failNow(new IllegalArgumentException());
            return;
        } catch (IllegalArgumentException e) {
            logger.info(e.getMessage());
            logger.info("Empty request failed validation");
        }

        request.clear();
        request.put("type", new JsonArray());
        try {
            AuthenticationServiceImpl.validateRequest(request);
            logger.error("Empty type array passed validation");
            testContext.failNow(new IllegalArgumentException());
            return;
        } catch (IllegalArgumentException e) {
            logger.info(e.getMessage());
            logger.info("Empty type failed validation");
        }

        request.clear();
        request.put("type", new JsonArray().add(Constants.INVALID_TYPE_STRING));
        try {
            AuthenticationServiceImpl.validateRequest(request);
            logger.error("Invalid type was accepted by validator");
            testContext.failNow(new IllegalArgumentException());
            return;
        } catch (IllegalArgumentException e) {
            logger.info(e.getMessage());
            logger.info("Invalid type field rejected by validator");
        }

        request.clear();
        request.put("type", new JsonArray().add(Constants.RESOURCE_TYPE_STRING));
        try {
            AuthenticationServiceImpl.validateRequest(request);
            logger.error("Missing ID and Name+ResourceGroup fields but accepted by validator");
            testContext.failNow(new IllegalArgumentException());
            return;
        } catch (IllegalArgumentException e) {
            logger.info(e.getMessage());
            logger.info("Missing ID and Name+ResourceGroup fields and rejected by validator");
        }

        request.clear();
        request.put("type", new JsonArray().add(Constants.RESOURCE_TYPE_STRING));
        request.put("id", Constants.DUMMY_RESOURCE_ID);
        try {
            AuthenticationServiceImpl.validateRequest(request);
            logger.info("Request validator success for valid type and ID of resource Item");
        } catch (IllegalArgumentException e) {
            logger.error("Valid request arg rejected by validator");
            testContext.failNow(e.getCause());
            return;
        }

        request.clear();
        request.put("type", new JsonArray().add(Constants.RESOURCE_TYPE_STRING));
        request.put("name", Constants.DUMMY_RESOURCE_NAME);
        request.put("resourceGroup", Constants.DUMMY_RESOURCE_GROUP);
        try {
            AuthenticationServiceImpl.validateRequest(request);
            logger.info("Request validator success for valid type and name+resourceGroup of resource Item");
        } catch (IllegalArgumentException e) {
            logger.error("Valid request arg rejected by validator");
            testContext.failNow(e.getCause());
            return;
        }

        testContext.completeNow();
    }
}
