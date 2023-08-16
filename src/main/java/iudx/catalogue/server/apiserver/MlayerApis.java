package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.METHOD;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.mlayer.MlayerService;
import iudx.catalogue.server.mlayer.util.Constants;
import iudx.catalogue.server.util.Api;
import iudx.catalogue.server.validator.ValidatorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


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
   * Create Mlayer Instance Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void createMlayerInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Create Instance");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthInfo = new JsonObject();

    jwtAuthInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_POST)
        .put(API_ENDPOINT, MLAYER_INSTANCE_ENDPOINT)
        .put(ID, host);

    Future<JsonObject> authFuture = inspectToken(jwtAuthInfo);

    authFuture
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
                                  .withDetail("The Schema of requested body is invalid.")
                                  .getResponse());
                    } else {
                      LOGGER.debug("Validation Successful");
                      mlayerService.createMlayerInstance(
                          requestBody,
                          handler -> {
                            if (handler.succeeded()) {
                              response.setStatusCode(201).end(handler.result().toString());

                            } else {
                              if (handler.cause().getMessage().contains("Item already exists")) {
                                response.setStatusCode(409).end(handler.cause().getMessage());
                              } else {
                                response.setStatusCode(400).end(handler.cause().getMessage());
                              }
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
   * Get mlayer instance handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching mlayer Instance");
    String id = "";
    if (routingContext.request().getParam("id") != null) {
      id = routingContext.request().getParam("id");
    }
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    mlayerService.getMlayerInstance(
        id, handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
        });
  }

  /**
   * Delete Mlayer Instance Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void deleteMlayerInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : deleting mlayer Instance");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthInfo = new JsonObject();

    jwtAuthInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_DELETE)
        .put(API_ENDPOINT, MLAYER_INSTANCE_ENDPOINT)
        .put(ID, host);

    Future<JsonObject> authFuture = inspectToken(jwtAuthInfo);
    authFuture
        .onSuccess(
            successHandler -> {
              String instanceId = request.getParam(MLAYER_ID);
              mlayerService.deleteMlayerInstance(
                  instanceId,
                  dbHandler -> {
                    if (dbHandler.succeeded()) {
                      LOGGER.info("Success: Item deleted");
                      LOGGER.debug(dbHandler.result().toString());
                      response.setStatusCode(200).end(dbHandler.result().toString());
                    } else {
                      if (dbHandler.cause().getMessage().contains("urn:dx:cat:ItemNotFound")) {
                        response.setStatusCode(404).end(dbHandler.cause().getMessage());
                      } else {
                        response.setStatusCode(400).end(dbHandler.cause().getMessage());
                      }
                    }
                  });
            })
        .onFailure(
            failureHandler -> {
              response.setStatusCode(401).end(failureHandler.getMessage());
            });
  }

  /**
   * Update Mlayer Instance Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void updateMlayerInstanceHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Updating Mlayer Instance");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthInfo = new JsonObject();
    jwtAuthInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_PUT)
        .put(API_ENDPOINT, MLAYER_INSTANCE_ENDPOINT)
        .put(ID, host);

    Future<JsonObject> authFuture = inspectToken(jwtAuthInfo);
    authFuture
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
                                  .withDetail("The Schema of requested body is invalid.")
                                  .getResponse());
                    } else {

                      String instanceId = request.getParam(MLAYER_ID);
                      requestBody.put(INSTANCE_ID, instanceId);
                      mlayerService.updateMlayerInstance(
                          requestBody,
                          handler -> {
                            if (handler.succeeded()) {
                              response.setStatusCode(200).end(handler.result().toString());
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

  private Future<JsonObject> inspectToken(JsonObject jwtAuthInfo) {
    Promise<JsonObject> promise = Promise.promise();

    authService.tokenInterospect(
        new JsonObject(),
        jwtAuthInfo,
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
                    .withDetail(DETAIL_INVALID_TOKEN)
                    .getResponse());
          }
        });
    return promise.future();
  }

  /**
   * Create Mlayer Domain Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void createMlayerDomainHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Doamin Created");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthInfo = new JsonObject();
    jwtAuthInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_POST)
        .put(API_ENDPOINT, MLAYER_DOMAIN_ENDPOINT)
        .put(ID, host);

    Future<JsonObject> authenticationFuture = inspectToken(jwtAuthInfo);
    authenticationFuture
        .onSuccess(
            successHandler -> {
              validatorService.validateMlayerDomain(
                  requestBody,
                  validateHandler -> {
                    if (validateHandler.failed()) {
                      response
                          .setStatusCode(400)
                          .end(
                              new RespBuilder()
                                  .withType(TYPE_INVALID_SCHEMA)
                                  .withTitle(TITLE_INVALID_SCHEMA)
                                  .withDetail("The Schema of requested body is invalid.")
                                  .getResponse());
                    } else {
                      LOGGER.debug("Validation Successful");
                      mlayerService.createMlayerDomain(
                          requestBody,
                          handler -> {
                            if (handler.succeeded()) {
                              response.setStatusCode(201).end(handler.result().toString());
                            } else {
                              if (handler.cause().getMessage().contains("Item already exists")) {
                                response.setStatusCode(409).end(handler.cause().getMessage());
                              } else {
                                response.setStatusCode(400).end(handler.cause().getMessage());
                              }
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
   * Get Mlayer Domain Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerDomainHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: fetching mlayer domains");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    mlayerService.getMlayerDomain(
        handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
        });
  }

  /**
   * Update Mlayer Domain Handler.
   *
   * @param routingContext {@link RoutingContext}
   */

  public void updateMlayerDomainHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Updating Mlayer Instance");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthInfo = new JsonObject();
    jwtAuthInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_PUT)
        .put(API_ENDPOINT, MLAYER_DOMAIN_ENDPOINT)
        .put(ID, host);

    Future<JsonObject> authFuture = inspectToken(jwtAuthInfo);
    authFuture
        .onSuccess(
            successHandler -> {
              validatorService.validateMlayerDomain(
                  requestBody,
                  validationHandler -> {
                    if (validationHandler.failed()) {
                      response
                          .setStatusCode(400)
                          .end(
                              new RespBuilder()
                                  .withType(TYPE_INVALID_SCHEMA)
                                  .withTitle(TITLE_INVALID_SCHEMA)
                                  .withDetail("The Schema of requested body is invalid.")
                                  .getResponse());
                    } else {

                      String domainId = request.getParam(MLAYER_ID);
                      requestBody.put(DOMAIN_ID, domainId);
                      mlayerService.updateMlayerDomain(
                          requestBody,
                          handler -> {
                            if (handler.succeeded()) {
                              response.setStatusCode(200).end(handler.result().toString());
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
   * Delete Mlayer Domain Handler.
   *
   * @param routingContext {@link RoutingContext}
   */

  public void deleteMlayerDomainHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : deleting mlayer Domain");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthInfo = new JsonObject();

    jwtAuthInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_DELETE)
        .put(API_ENDPOINT, MLAYER_DOMAIN_ENDPOINT)
        .put(ID, host);

    Future<JsonObject> authenticationFuture = inspectToken(jwtAuthInfo);
    authenticationFuture
        .onSuccess(
            successHandler -> {
              String domainId = request.getParam(MLAYER_ID);
              mlayerService.deleteMlayerDomain(
                  domainId,
                  dbHandler -> {
                    if (dbHandler.succeeded()) {
                      LOGGER.info("Success: Item deleted");
                      LOGGER.debug(dbHandler.result().toString());
                      response.setStatusCode(200).end(dbHandler.result().toString());
                    } else {
                      if (dbHandler.cause().getMessage().contains("urn:dx:cat:ItemNotFound")) {
                        response.setStatusCode(404).end(dbHandler.cause().getMessage());
                      } else {
                        response.setStatusCode(400).end(dbHandler.cause().getMessage());
                      }
                    }
                  });
            })
        .onFailure(
            failureHandler -> {
              response.setStatusCode(401).end(failureHandler.getMessage());
            });
  }

  /**
   * Get mlayer providers handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerProvidersHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching mlayer Providers");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    mlayerService.getMlayerProviders(
            handler -> {
              if (handler.succeeded()) {
                response.setStatusCode(200).end(handler.result().toString());
              } else {
                response.setStatusCode(400).end(handler.cause().getMessage());
              }
            });
  }

  /**
   * Get mlayer GeoQuery Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerGeoQueryHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching location and label of datasets");
    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    validatorService.validateMlayerGeoQuery(
        requestBody,
        validationHandler -> {
          if (validationHandler.failed()) {
            response
                .setStatusCode(400)
                .end(
                    new RespBuilder()
                        .withType(TYPE_INVALID_SCHEMA)
                        .withTitle(TITLE_INVALID_SCHEMA)
                        .withDetail("The Schema of requested body is invalid.")
                        .getResponse());
          } else {
            mlayerService.getMlayerGeoQuery(
                requestBody,
                handler -> {
                  if (handler.succeeded()) {
                    response.setStatusCode(200).end(handler.result().toString());
                  } else {
                    response.setStatusCode(400).end(handler.cause().getMessage());
                  }
                });
          }
        });
  }

  /**
   * Get mlayer All Datasets Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerAllDatasetsHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching all datasets that belong to IUDX");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    mlayerService.getMlayerAllDatasets(
        handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
        });
  }

  /**
   * Get mlayer Dataset Handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerDatasetHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching details of the dataset");
    HttpServerResponse response = routingContext.response();
    HttpServerRequest request = routingContext.request();
    String datasetId = request.getParam(ID);

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    validatorService.validateMlayerDatasetId(
        datasetId,
        validationHandler -> {
          if (validationHandler.failed()) {
            response
                    .setStatusCode(400)
                    .end(
                            new RespBuilder()
                                    .withType(TYPE_INVALID_SCHEMA)
                                    .withTitle(TITLE_INVALID_SCHEMA)
                                    .withDetail("The Schema of requested body is invalid.")
                                    .getResponse());
          } else {
            LOGGER.debug("Validation of dataset Id Successful");
            mlayerService.getMlayerDataset(
                    datasetId,
                    handler -> {
                      if (handler.succeeded()) {
                        response.setStatusCode(200).end(handler.result().toString());
                      } else {
                        if (handler.cause().getMessage().contains("urn:dx:cat:ItemNotFound")) {
                          response.setStatusCode(404).end(handler.cause().getMessage());
                        } else {
                          response.setStatusCode(400).end(handler.cause().getMessage());
                        }
                      }
                    });
            }
        });
  }

  /**
   * Get mlayer popular Datasets handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getMlayerPopularDatasetsHandler(RoutingContext routingContext) {
    LOGGER.debug("Info : fetching the data for the landing Page");
    String instance = "";
    if (routingContext.request().getParam(INSTANCE) != null) {
      instance = routingContext.request().getParam(INSTANCE);
    }
    LOGGER.debug("Instance {}", instance);
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    mlayerService.getMlayerPopularDatasets(instance,
        handler -> {
          if (handler.succeeded()) {
            response.setStatusCode(200).end(handler.result().toString());
          } else {
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
        });
  }
}
