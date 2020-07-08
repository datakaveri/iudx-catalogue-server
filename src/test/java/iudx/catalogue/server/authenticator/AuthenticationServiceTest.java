package iudx.catalogue.server.authenticator;

import io.vertx.core.Vertx;
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
}
