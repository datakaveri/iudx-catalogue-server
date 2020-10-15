package iudx.catalogue.server.authenticator;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Random;

@ExtendWith(VertxExtension.class)
public class AuthenticationServiceTest {
    private static final Logger LOGGER = LogManager.getLogger(AuthenticationServiceTest.class);
    private static Vertx vertxObj;
    private static AuthenticationService authenticationService;
    private static JsonObject config;
    private static String TOKEN = "";
    private static String authHost = "";
    private static String dummyToken = "";

    /**
     * Initialize and start the auth service for testing.
     *
     * @param vertx       the vertx instance object. Injected by VertxExtension and not started in clustered mode.
     * @param testContext the context object injected by VertxExtension
     */
    @BeforeAll
    @DisplayName("Initialize Vertx and deploy AuthenticationVerticle")
    static void initialize(Vertx vertx, io.vertx.reactivex.core.Vertx vertx2,
        VertxTestContext testContext) {
        vertxObj = vertx;

        config = Configuration.getConfiguration("./configs/config-test.json", 1);
        authHost = config.getString("authServerHost");
        TOKEN = config.getString("token");
        dummyToken = config.getString("dummyToken");

        WebClient client = AuthenticationVerticle.createWebClient(vertxObj, config, true);
        authenticationService = new AuthenticationServiceImpl(client, authHost);
        LOGGER.info("Auth tests setup complete");
        testContext.completeNow();
    }

    @Test
    @DisplayName("Testing setup")
    public void shouldSucceed(VertxTestContext testContext) {
        LOGGER.info("Default test is passing");
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
        WebClient client = AuthenticationVerticle.createWebClient(vertxObj, config, true);
        String host = config.getString("authServerHost");
        client.post(443, host, Constants.AUTH_CERTINFO_PATH).send(httpResponseAsyncResult -> {
            if (httpResponseAsyncResult.failed()) {
                LOGGER.error("Cert info call to auth server failed");
                testContext.failNow(httpResponseAsyncResult.cause());
                return;
            }
            LOGGER.info("Cert info call to auth server succeeded");
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
        int rnd = new Random().nextInt(HttpMethod.values().length);
        String dummyMethod = HttpMethod.values()[rnd].toString();
        authInfo.put("token", dummyToken);
        authInfo.put("operation", dummyMethod);
        try {
            AuthenticationServiceImpl.validateAuthInfo(authInfo);
            LOGGER.info("Dummy token value: " + dummyToken);
            LOGGER.info("Dummy operation value: " + dummyMethod);
            LOGGER.info("Validation of proper auth info arg succeeded");
        } catch (IllegalArgumentException e) {
            LOGGER.error("Valid auth info arg failed validation");
            testContext.failNow(e.getCause());
            return;
        }

        authInfo.clear();
        try {
            AuthenticationServiceImpl.validateAuthInfo(authInfo);
            LOGGER.error("Empty authInfo without token/operation passed validation");
            testContext.failNow(new IllegalArgumentException());
            return;
        } catch (IllegalArgumentException e) {
            LOGGER.info(e.getMessage());
            LOGGER.info("Empty authInfo failed validation");
        }

        authInfo.clear();
        authInfo.put("token", dummyToken);
        authInfo.put("operation", "");
        try {
            AuthenticationServiceImpl.validateAuthInfo(authInfo);
            LOGGER.error("Blank token/operation in authInfo passed validation");
            testContext.failNow(new IllegalArgumentException());
            return;
        } catch (IllegalArgumentException e) {
            LOGGER.info(e.getMessage());
            LOGGER.info("Blank token/operation in authInfo failed validation");
        }

        authInfo.clear();
        authInfo.put("token", dummyToken);
        authInfo.put("operation", "NONSENSE");
        try {
            AuthenticationServiceImpl.validateAuthInfo(authInfo);
            LOGGER.error("Invalid operation in authInfo passed validation");
            testContext.failNow(new IllegalArgumentException());
            return;
        } catch (IllegalArgumentException e) {
            LOGGER.info(e.getMessage());
            LOGGER.info("Invalid operation in authInfo failed validation");
        }

        testContext.completeNow();
    }

    @Test
    @DisplayName("Test happy path of the TIP call with dummy PUT call")
    public void testValidTIP(VertxTestContext testContext) {
        JsonObject request = new JsonObject();
        request.put("provider", Constants.DUMMY_PROVIDER_PREFIX);
        JsonObject authInfo = new JsonObject();
        authInfo.put("token", TOKEN);
        authInfo.put("operation", HttpMethod.PUT.toString());
        authenticationService.tokenInterospect(request, authInfo, jsonObjectAsyncResult -> {
            if (jsonObjectAsyncResult.failed()) {
                LOGGER.error("Async response of valid TIP call failed");
                testContext.failNow(jsonObjectAsyncResult.cause());
                return;
            }
            JsonObject result = jsonObjectAsyncResult.result();
            String status = result.getString("status");
            if (status.equals("error")) {
                LOGGER.error("Valid TIP call failed");
                testContext.failNow(new Exception(result.getString("message")));
            } else if (status.equals("success")) {
                LOGGER.info("Valid TIP call success");
                testContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test missing provider in request object for the TIP call")
    public void testMissingProviderInTIP(VertxTestContext testContext) {
        JsonObject request = new JsonObject();
        JsonObject authInfo = new JsonObject();
        authInfo.put("token", dummyToken);
        authInfo.put("operation", HttpMethod.PUT.toString());
        authenticationService.tokenInterospect(request, authInfo, jsonObjectAsyncResult -> {
            JsonObject result = jsonObjectAsyncResult.result();
            String status = result.getString("status");
            if (status.equals("error")) {
                LOGGER.info("Missing provider info failed validation in TIP call");
                testContext.completeNow();
            } else {
                LOGGER.error("Missing provider info succeeded validation in TIP call");
                testContext.failNow(new IllegalArgumentException());
            }
        });
    }

    @Test
    @DisplayName("Test invalid token for the TIP call")
    public void testInvalidTokenInTIP(VertxTestContext testContext) {
        JsonObject request = new JsonObject();
        request.put("provider", Constants.DUMMY_PROVIDER_PREFIX);
        JsonObject authInfo = new JsonObject();
        String invalidToken = dummyToken.substring(0, dummyToken.length() - 1);
        authInfo.put("token", invalidToken);
        authInfo.put("operation", HttpMethod.PUT.toString());
        authenticationService.tokenInterospect(request, authInfo, jsonObjectAsyncResult -> {
            JsonObject result = jsonObjectAsyncResult.result();
            String status = result.getString("status");
            if (status.equals("error")) {
                LOGGER.info("Invalid token failed TIP call");
                LOGGER.info(result.getString("message"));
                testContext.completeNow();
            } else {
                LOGGER.error("Invalid token succeeded TIP call");
                testContext.failNow(new IllegalArgumentException());
            }
        });
    }

    @Test
    @DisplayName("Test impermissible operation for the TIP call")
    public void testInvalidPermissionInTIP(VertxTestContext testContext) {
        JsonObject request = new JsonObject();
        request.put("provider", Constants.DUMMY_PROVIDER_PREFIX);
        JsonObject authInfo = new JsonObject();
        authInfo.put("token", dummyToken);
        authInfo.put("operation", HttpMethod.OTHER.toString());
        authenticationService.tokenInterospect(request, authInfo, jsonObjectAsyncResult -> {
            JsonObject result = jsonObjectAsyncResult.result();
            String status = result.getString("status");
            if (status.equals("error")) {
                LOGGER.info("Invalid operation failed TIP call");
                LOGGER.info(result.getString("message"));
                testContext.completeNow();
            } else {
                LOGGER.error("Invalid operation succeeded TIP call");
                testContext.failNow(new IllegalArgumentException());
            }
        });
    }
}
