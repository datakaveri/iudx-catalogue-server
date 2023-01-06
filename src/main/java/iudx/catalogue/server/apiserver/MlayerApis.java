package iudx.catalogue.server.apiserver;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.mlayer.MlayerService;
import iudx.catalogue.server.validator.ValidatorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.METHOD;
import static iudx.catalogue.server.util.Constants.*;

public class MlayerApis {
  private MlayerService mlayerService;
  private AuthenticationService authService;
  private ValidatorService validatorService;
  private String host;
  private static final Logger LOGGER = LogManager.getLogger(MlayerApis.class);

  public void setMlayerService(MlayerService mlayerService) {
    this.mlayerService = mlayerService;
  }

  public void setAuthService(AuthenticationService authService) {
    this.authService = authService;
  }

  public void setValidatorService(ValidatorService validatorService) {
    this.validatorService = validatorService;
  }

  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Create Mlayer Instance Handler
   *
   * @param routingContext {@link RoutingContext}
   */
  public void createMlayerInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Create Instance");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthenticationInfo = new JsonObject();

    jwtAuthenticationInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_POST)
        .put(API_ENDPOINT, MLAYER_INSTANCE_ENDPOINT)
        .put(ID, host);

    Future<JsonObject> authenticationFuture = inspectToken(jwtAuthenticationInfo);

    authenticationFuture
        .onSuccess(
            successHandler -> {
              validatorService.validateMlayerInstance(
                  requestBody,
                  validateHandler -> {
                    if (validateHandler.failed()) {
                      response
                          .setStatusCode(400)
                          .end(
                              new RespBuilder()
                                  .withType(TYPE_INVALID_SCHEMA)
                                  .withTitle(TITLE_INVALID_SCHEMA)
                                  .getResponse());
                    } else {
                      LOGGER.debug("Validation Successful");
                      mlayerService.createMlayerInstance(
                          requestBody,
                          handler -> {
                            if (handler.succeeded()) {
                              response.setStatusCode(201).end(handler.result().toString());

                            } else {
                              response.setStatusCode(400).end(handler.cause().getMessage());
                            }
                          });
                    }
                  });
            })
        .onFailure(
            failureHandler -> {
              response.setStatusCode(401).end(failureHandler.getMessage());
            });
  }

  private Future<JsonObject> inspectToken(JsonObject jwtAuthenticationInfo) {
    Promise<JsonObject> promise = Promise.promise();

    authService.tokenInterospect(
        new JsonObject(),
        jwtAuthenticationInfo,
        authHandler -> {
          if (authHandler.succeeded()) {
            LOGGER.debug("JWT Auth Successful");
            LOGGER.debug(authHandler.result());
            promise.complete(authHandler.result());
          } else {
            LOGGER.error(authHandler.cause().getMessage());
            promise.fail(
                new RespBuilder()
                    .withType(TYPE_TOKEN_INVALID)
                    .withTitle(TITLE_TOKEN_INVALID)
                    .withDetail(authHandler.cause().getMessage())
                    .getResponse());
          }
        });
    return promise.future();
  }
}
