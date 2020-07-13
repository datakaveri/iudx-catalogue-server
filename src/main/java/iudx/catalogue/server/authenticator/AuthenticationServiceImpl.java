package iudx.catalogue.server.authenticator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.http.HttpStatus;

import java.util.regex.Pattern;

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
        JsonArray typeArray = request.getJsonArray("type", new JsonArray());
        String itemType;
        try {
            validateAuthInfo(authenticationInfo);
            itemType = extractItemType(typeArray);
            if (itemType.equals(Constants.INVALID_TYPE_STRING))
                throw new IllegalStateException("Invalid item type values in the request object");
        } catch (IllegalArgumentException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            handler.handle(Future.succeededFuture(result));
            return this;
        }

        webClient.post(443, Constants.AUTH_SERVER_HOST, Constants.AUTH_TIP_PATH).send(httpResponseAsyncResult -> {
            HttpResponse<Buffer> response = httpResponseAsyncResult.result();
            if (response.statusCode() != HttpStatus.SC_OK) {
                result.put("status", "error");
                result.put("message", response
                        .bodyAsJsonObject()
                        .getJsonObject("error")
                        .getString("message"));
                handler.handle(Future.succeededFuture(result));
                return;
            }

            JsonArray responseRequests = response.bodyAsJsonObject().getJsonArray("request");
            for (Object req : responseRequests) {
                JsonObject requestEntry = (JsonObject) req;
                String requestID = requestEntry.getString("id", "");
                JsonArray requestMethods = requestEntry.getJsonArray("methods");
                String operation = authenticationInfo.getString("operation");
                if (isPermittedMethod(requestMethods, operation) &&
                        isPermittedID(requestID, request.getString("id", ""))) {
                    result.put("status", "success");
                    result.put("body", new JsonObject().put("id", constructID(request, operation, itemType)));
                    handler.handle(Future.succeededFuture(result));
                    return;
                }
            }

            result.put("status", "error");
            result.put("message", "ID/Operations not permitted with presented token");
            handler.handle(Future.succeededFuture(result));
        });

        return null;
    }

    private String constructID(JsonObject request, String operation, String itemType) {
        if (!operation.equals(HttpMethod.POST.toString())) return request.getString("id");
        if (itemType.equals(Constants.RESOURCE_TYPE_STRING)) {
            return request.getString("resourceGroup") + request.getString("name");
        } else if (itemType.equals(Constants.RESOURCE_GROUP_TYPE_STRING)) {
            return request.getString("resourceServer") + request.getString("name");
        }
        return null;
    }

    private boolean isPermittedID(String responseID, String requestID) {
        if (requestID.equals("")) return true;
        Pattern pattern = Pattern.compile(
                responseID
                        .replace("/", "\\/")
                        .replace(".", "\\.")
                        .replace("*", ".*")
        );
        return pattern.matcher(requestID).matches();
    }

    private boolean isPermittedMethod(JsonArray methods, String operation) {
        for (Object o : methods) {
            String method = (String) o;
            if (method.equals("*") || method.equals(operation)) return true;
        }
        return false;
    }

}
