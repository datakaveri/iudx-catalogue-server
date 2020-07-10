package iudx.catalogue.server.authenticator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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

    static void validateRequest(JsonObject request) throws IllegalArgumentException {
        if (request.isEmpty()) throw new IllegalArgumentException("Request argument is empty/missing");
        JsonArray typeArray = request.getJsonArray("type", new JsonArray());
        if (typeArray.isEmpty()) throw new IllegalArgumentException("Type array is empty/missing");
        String itemType = extractItemType(typeArray);
        if (itemType.equals(Constants.INVALID_TYPE_STRING))
            throw new IllegalArgumentException("Missing valid item type");
        String itemID = request.getString("id", "");
        String itemName = request.getString("name", "");
        String itemResourceGroup = request.getString("resourceGroup", "");
        if (itemID.isBlank() &&
                (itemName.isBlank() || itemResourceGroup.isBlank()))
            throw new IllegalArgumentException("Missing ID and name/resource group");
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
        if (token.isBlank()) throw new IllegalArgumentException("Token in authenticationInfo is blank/missing");
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
            validateRequest(request);
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
