package iudx.catalogue.server.authenticator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.apache.http.HttpStatus;

import java.util.Arrays;

import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

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

    private static final Logger LOGGER = LogManager.getLogger(AuthenticationServiceImpl.class);
    private final WebClient webClient;
    private String authHost;

    public AuthenticationServiceImpl(WebClient client, String authHost) {
        webClient = client;
        this.authHost = authHost;
    }

    static void validateAuthInfo(JsonObject authInfo) throws IllegalArgumentException {
        if (authInfo.isEmpty()) throw new IllegalArgumentException("AuthInfo argument is empty/missing");
        String token = authInfo.getString(TOKEN, "");
        String operation = authInfo.getString(OPERATION, "");
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

        JsonObject result = new JsonObject();
        String providerID;
        try {
            validateAuthInfo(authenticationInfo);
            providerID = request.getString(PROVIDER, "");
            if (providerID.isBlank()) throw new IllegalArgumentException("Missing provider in request object");
        } catch (IllegalArgumentException e) {
            result.put(STATUS, ERROR);
            result.put(MESSAGE, e.getMessage());
            handler.handle(Future.succeededFuture(result));
            return this;
        }

        JsonObject body = new JsonObject();
        body.put(TOKEN, authenticationInfo.getString(TOKEN));
        webClient
            .post(443, authHost, AUTH_TIP_PATH)
                .expect(ResponsePredicate.JSON)
                .sendJsonObject(body, httpResponseAsyncResult -> {
                    if (httpResponseAsyncResult.failed()) {
                        result.put(STATUS, ERROR);
                        result.put(MESSAGE, AUTH_SERVER_ERROR);
                        LOGGER.error(AUTH_SERVER_ERROR+";", httpResponseAsyncResult.cause());
                        handler.handle(Future.succeededFuture(result));
                        return;
                    }
                    HttpResponse<Buffer> response = httpResponseAsyncResult.result();
                    if (response.statusCode() != HttpStatus.SC_OK) {
                        result.put(STATUS, ERROR);
                        result.put(MESSAGE, response
                                .bodyAsJsonObject()
                                .getJsonObject(ERROR)
                                .getString(MESSAGE));
                        handler.handle(Future.succeededFuture(result));
                        return;
                    }

                    JsonArray responseRequests = response.bodyAsJsonObject().getJsonArray(REQUEST);
                    for (Object req : responseRequests) {
                        JsonObject requestEntry = (JsonObject) req;
                        String requestID = requestEntry.getString(ID, "");
                        JsonArray requestMethods = requestEntry.getJsonArray("methods");
                        String operation = authenticationInfo.getString(OPERATION);
                        if (isPermittedMethod(requestMethods, operation) && isPermittedProviderID(requestID, providerID)) {
                            result.put(STATUS, SUCCESS);
                            result.put(BODY, new JsonObject());
                            handler.handle(Future.succeededFuture(result));
                            return;
                        }
                    }

                    result.put(STATUS, ERROR);
                    result.put(MESSAGE, "ID/Operations not permitted with presented token");
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
