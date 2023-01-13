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
import iudx.catalogue.server.util.Api;
import iudx.catalogue.server.util.Constants;

import iudx.catalogue.server.validator.ValidatorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.*;

import static iudx.catalogue.server.mlayer.util.Constants.INSTANCE_ID;
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

  private Api api;

  public MlayerApis(Api api) {
    this.api = api;
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

  /**
   * Get mlayer instance handler
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching mlayer Instances");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    JsonObject authenticationInfo =
        new JsonObject()
            .put(TOKEN, request.getHeader(TOKEN))
            .put(METHOD, REQUEST_GET)
            .put(API_ENDPOINT, MLAYER_INSTANCE_ENDPOINT)
            .put(ID, host);
    Future<JsonObject> authenticationFuture = inspectToken(authenticationInfo);
    authenticationFuture
        .onSuccess(
            successHandler -> {
              LOGGER.debug("authentication successful ");
              mlayerService.getMlayerInstance(
                  handler -> {
                    if (handler.succeeded()) {
                      response.setStatusCode(200).end(handler.result().toString());
                    } else {
                      response.setStatusCode(400).end(handler.cause().getMessage());
                    }
                  });
            })
        .onFailure(
            failureHandler -> {
              response.setStatusCode(401).end(failureHandler.getMessage());
            });
  }

  /**
   * Delete Mlayer Instance Handler
   *
   * @param routingContext {@link RoutingContext}
   */
  public void deleteMlayerInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : deleting mlayer Instance");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthenticationInfo = new JsonObject();

    jwtAuthenticationInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(Constants.METHOD, REQUEST_DELETE)
        .put(API_ENDPOINT, MLAYER_INSTANCE_ENDPOINT)
        .put(ID, host);

    Future<JsonObject> authenticationFuture = inspectToken(jwtAuthenticationInfo);
    authenticationFuture
        .onSuccess(
            successHandler -> {
              String instanceId = request.getParam(INSTANCE_ID);
              JsonObject requestBody = new JsonObject().put(INSTANCE_ID, instanceId);
              mlayerService.deleteMlayerInstance(
                  requestBody,
                  dbHandler -> {
                    if (dbHandler.succeeded()) {
                      LOGGER.info("Success: Item deleted");
                      LOGGER.debug(dbHandler.result().toString());
                      response.setStatusCode(200).end(dbHandler.result().toString());
                    } else {
                      response.setStatusCode(400).end(dbHandler.cause().toString());
                    }
                  });
            })
        .onFailure(
            failureHandler -> {
              response.setStatusCode(401).end(failureHandler.getMessage());
            });
  }

  public void updateMlayerInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Updating Mlayer Instance");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String requestBodyName = requestBody.getString(NAME);
    String parameterName = request.getParam(NAME);

    JsonObject jwtAuthenticationInfo = new JsonObject();
    jwtAuthenticationInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_PUT)
        .put(API_ENDPOINT, MLAYER_INSTANCE_ENDPOINT)
        .put(ID, host);

    Future<JsonObject> authenticationFuture = inspectToken(jwtAuthenticationInfo);
    authenticationFuture
        .onSuccess(
            successHandler -> {
              validatorService.validateMlayerInstance(
                  requestBody,
                  validationHandler -> {
                    if (validationHandler.failed()) {
                      response
                          .setStatusCode(400)
                          .end(
                              new RespBuilder()
                                  .withType(TYPE_INVALID_SCHEMA)
                                  .withTitle(TITLE_INVALID_SCHEMA)
                                  .getResponse());
                    } else {
                      if (parameterName.equals(requestBodyName)) {

                        mlayerService.updateMlayerInstance(
                            requestBody,
                            handler -> {
                              if (handler.succeeded()) {
                                response.setStatusCode(200).end(handler.result().toString());
                              } else {
                                response.setStatusCode(400).end(handler.cause().getMessage());
                              }
                            });
                      } else {
                        response
                            .setStatusCode(400)
                            .end(
                                new RespBuilder()
                                    .withType(INVALID_VALUE)
                                    .withDetail(
                                        "Parameter instance name and request body instance name not same")
                                    .getResponse());
                      }
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
