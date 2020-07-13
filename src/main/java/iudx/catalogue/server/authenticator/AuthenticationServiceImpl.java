package iudx.catalogue.server.authenticator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

/**
 * The Authentication Service Implementation.
 * <h1>Authentication Service Implementation</h1>
 * <p>
 * The Authentication Service implementation in the IUDX Catalogue Server implements the definitions
 * of the {@link iudx.catalogue.server.authenticator.AuthenticationService}.
 * </p>
 *
 * @version 1.0
 * @since 2020-05-31
 */

public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    private final WebClient webClient;

    public AuthenticationServiceImpl(WebClient client) {
        webClient = client;
    }

    private static String extractItemType(JsonArray typeArray) {
        for (Object o : typeArray) {
            for (String validType : Constants.VALID_TYPE_STRINGS) {
                String candidateType = (String) o;
                if (candidateType.equals(validType)) return candidateType;
            }
        }
        return Constants.INVALID_TYPE_STRING;
    }

    static void validateAuthInfo(JsonObject authInfo) throws IllegalArgumentException {
        if (authInfo.isEmpty()) throw new IllegalArgumentException("AuthInfo argument is empty/missing");
        String token = authInfo.getString("token", "");
        String operation = authInfo.getString("operation", "");
        if (token.isBlank() || operation.isBlank())
            throw new IllegalArgumentException("Token/Operation in authenticationInfo is blank/missing");
        try {
            HttpMethod.valueOf(operation);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid operation in the authenticationInfo object");
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthenticationService tokenInterospect(JsonObject request, JsonObject authenticationInfo,
                                                  Handler<AsyncResult<JsonObject>> handler) {

        // TODO: Stub code, to be removed after use
        JsonObject result = new JsonObject();
        try {
            validateAuthInfo(authenticationInfo);
        } catch (IllegalArgumentException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            handler.handle(Future.succeededFuture(result));
            return this;
        }
        handler.handle(Future.succeededFuture(result));
        return null;
    }

}
