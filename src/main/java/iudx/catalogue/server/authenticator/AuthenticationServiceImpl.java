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
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.apache.http.HttpStatus;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

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
    private static final Properties properties = new Properties();
    private final WebClient webClient;

    public AuthenticationServiceImpl(WebClient client) {
        webClient = client;
        try {
            FileInputStream configFile = new FileInputStream(Constants.CONFIG_FILE);
            if (properties.isEmpty()) properties.load(configFile);
        } catch (IOException e) {
            logger.error("Could not load properties from config file", e);
        }
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
        String providerID;
        try {
            validateAuthInfo(authenticationInfo);
            providerID = request.getString("provider", "");
            if (providerID.isBlank()) throw new IllegalArgumentException("Missing provider in request object");
        } catch (IllegalArgumentException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            handler.handle(Future.succeededFuture(result));
            return this;
        }

        JsonObject body = new JsonObject();
        body.put("token", authenticationInfo.getString("token"));
        webClient
                .post(443, properties.getProperty(Constants.AUTH_SERVER_HOST), Constants.AUTH_TIP_PATH)
                .expect(ResponsePredicate.JSON)
                .sendJsonObject(body, httpResponseAsyncResult -> {
                    if (httpResponseAsyncResult.failed()) {
                        result.put("status", "error");
                        result.put("message", "Error calling the Auth Server");
                        logger.error("Error calling the auth server", httpResponseAsyncResult.cause());
                        handler.handle(Future.succeededFuture(result));
                        return;
                    }
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
                        if (isPermittedMethod(requestMethods, operation) && isPermittedProviderID(requestID, providerID)) {
                            result.put("status", "success");
                            result.put("body", new JsonObject());
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

    private boolean isPermittedProviderID(String requestID, String providerID) {
        String tipProvider = String.join("/", Arrays.asList(requestID.split("/", 3)).subList(0, 2));
        return providerID.equals(tipProvider);
    }

    private boolean isPermittedMethod(JsonArray methods, String operation) {
        for (Object o : methods) {
            String method = (String) o;
            if (method.equals("*") || method.equals(operation)) return true;
        }
        return false;
    }

}
