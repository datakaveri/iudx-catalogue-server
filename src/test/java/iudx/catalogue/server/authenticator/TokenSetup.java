package iudx.catalogue.server.authenticator;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import static iudx.catalogue.server.authenticator.TokensForITs.*;

public class TokenSetup {
    private static final Logger LOGGER = LogManager.getLogger(TokenSetup.class);
    private static WebClient webClient;

    public static void setupTokens(String authEndpoint, String providerClientId, String providerClientSecret, String consumerClientId, String consumerClientSecret) {
        // Fetch tokens asynchronously and wait for all completions
        CompositeFuture.all(
                fetchToken("provider", authEndpoint, providerClientId, providerClientSecret),
                fetchToken("admin", authEndpoint, consumerClientId,consumerClientSecret),
                fetchToken("cosAdmin", authEndpoint, providerClientId, providerClientSecret),
                fetchToken("consumer", authEndpoint, consumerClientId,consumerClientSecret)
        ).onComplete(result -> {
            if (result.succeeded()) {
                LOGGER.debug("Tokens setup completed successfully");
            } else {
                // Handle failure, e.g., log the error
                LOGGER.debug("errorrrr...");
                result.cause().printStackTrace();
            }
        });
    }

    private static Future<String> fetchToken(String userType, String authEndpoint, String clientID, String clientSecret) {
        //LOGGER.debug("user type is..." + userType);
        Promise<String> promise = Promise.promise();
        JsonObject jsonPayload = getPayload(userType);
        //LOGGER.debug("payload: " + jsonPayload);

        // Create a WebClient to make the HTTP request
        webClient = WebClient.create(Vertx.vertx(), new WebClientOptions().setSsl(true));

        webClient.postAbs(authEndpoint)
                .putHeader("Content-Type", "application/json")
                .putHeader("clientID", clientID)
                .putHeader("clientSecret", clientSecret)
                .sendJson(jsonPayload)
                .onComplete(result -> {
                    if (result.succeeded()) {
                        HttpResponse<Buffer> response = result.result();
                        if (response.statusCode() == 200) {
                        JsonObject jsonResponse = response.bodyAsJsonObject();
                        //LOGGER.debug("response is.. " + jsonResponse);
                        String accessToken = jsonResponse.getJsonObject("results").getString("accessToken");
                        //LOGGER.debug("access token is..." + accessToken);
                        // Store the token based on user type
                        switch (userType) {
                            case "provider":
                                providerToken = accessToken;
                                delegateToken =accessToken;
                                token = accessToken;
                                break;
                            case "admin":
                                adminToken = accessToken;
                                break;
                            case "cosAdmin":
                                cosAdminToken = accessToken;
                                openToken = accessToken;
                                break;
                            case "consumer":
                                consumerToken=accessToken;
                        }
                        promise.complete(accessToken);
                    } else {
                        promise.fail("Failed to get token. Status code: " + response.statusCode());
                    }
                    }else {
                        LOGGER.error("Failed to fetch token", result.cause());
                        promise.fail(result.cause());
                    }
                })
                .onFailure(throwable -> {
                    throwable.printStackTrace();
                    promise.fail(throwable);
                })
                .onComplete(result -> {
                    webClient.close();
                });

        return promise.future();
    }

    @NotNull
    private static JsonObject getPayload(String userType) {
        JsonObject jsonPayload = new JsonObject();
        switch (userType) {
            case "consumer":
                jsonPayload.put("itemId", "rs.iudx.io");
                jsonPayload.put("itemType", "resource_server");
                jsonPayload.put("role", "consumer");
                break;
            case "provider":
                jsonPayload.put("itemId", "rs.iudx.io");
                jsonPayload.put("itemType", "resource_server");
                jsonPayload.put("role", "provider");
                break;
            case "admin":
                jsonPayload.put("itemId", "rs.iudx.io");
                jsonPayload.put("itemType", "resource_server");
                jsonPayload.put("role", "admin");
                break;
            case "cosAdmin":
                jsonPayload.put("itemId", "cos.iudx.io");
                jsonPayload.put("itemType", "cos");
                jsonPayload.put("role", "cos_admin");
                break;
        }
        return jsonPayload;
    }
}
