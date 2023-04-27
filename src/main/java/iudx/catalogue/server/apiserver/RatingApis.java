package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.auditing.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.rating.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.auditing.AuditingService;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.rating.RatingService;
import iudx.catalogue.server.rating.util.Constants;
import iudx.catalogue.server.util.Api;
import iudx.catalogue.server.validator.ValidatorService;
import java.time.ZonedDateTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class RatingApis {
  private static final Logger LOGGER = LogManager.getLogger(RatingApis.class);
  private AuthenticationService authService;
  private ValidatorService validatorService;
  private AuditingService auditingService;
  private RatingService ratingService;

  private boolean hasAuditService = false;

  private String host;
  private Api api;

  public RatingApis(Api api) {
    this.api = api;
  }

  public void setRatingService(RatingService ratingService) {
    this.ratingService = ratingService;
  }

  public void setAuthService(AuthenticationService authService) {
    this.authService = authService;
  }

  public void setValidatorService(ValidatorService validatorService) {
    this.validatorService = validatorService;
  }

  public void setAuditingService(AuditingService auditingService) {
    this.auditingService = auditingService;
    hasAuditService = true;
  }

  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Create Rating handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void createRatingHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Creating Rating");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthenticationInfo = new JsonObject();

    String id = request.getParam(iudx.catalogue.server.util.Constants.ID);

    jwtAuthenticationInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(iudx.catalogue.server.util.Constants.METHOD, REQUEST_POST)
        .put(API_ENDPOINT, RATINGS_ENDPOINT)
        .put(iudx.catalogue.server.util.Constants.ID, host);

    Future<JsonObject> authenticationFuture = inspectToken(jwtAuthenticationInfo);

    authenticationFuture
        .onSuccess(
            successHandler -> {
              requestBody
                  .put(Constants.ID, id)
                  .put(Constants.USER_ID, successHandler.getString(Constants.USER_ID))
                  .put("status", PENDING);

              validatorService.validateRating(
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
                      ratingService.createRating(
                          requestBody,
                          handler -> {
                            if (handler.succeeded()) {
                              response.setStatusCode(201).end(handler.result().toString());
                              if (hasAuditService) {
                                updateAuditTable(
                                    successHandler, new String[] {id, ROUTE_RATING, REQUEST_POST});
                              }
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
   * GET Rating handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getRatingHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: fetching ratings");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String id = request.getParam(Constants.ID);

    JsonObject requestBody = new JsonObject().put(Constants.ID, id);

    if (request.params().contains("type")) {
      String requestType = request.getParam("type");
      if (requestType.equalsIgnoreCase("average") || requestType.equalsIgnoreCase("group")) {
        requestBody.put("type", requestType);
      } else {
        response
            .setStatusCode(400)
            .end(
                new RespBuilder()
                    .withType(TYPE_INVALID_QUERY_PARAM_VALUE)
                    .withTitle(TITLE_INVALID_QUERY_PARAM_VALUE)
                    .withDetail("Query parameter type cannot have value : " + requestType)
                    .getResponse());
      }

      ratingService.getRating(
          requestBody,
          handler -> {
            if (handler.succeeded()) {
              if (!handler.result().getJsonArray(iudx.catalogue.server.util.Constants.RESULTS)
                      .isEmpty()) {
                response.setStatusCode(200).end(handler.result().toString());
              } else {
                response.setStatusCode(204).end();
              }
            } else {
              if (handler.cause().getLocalizedMessage().contains("Doc doesn't exist")) {
                response.setStatusCode(404);
              } else {
                response.setStatusCode(400);
              }
              response.end(handler.cause().getMessage());
            }
          });
    } else {
      JsonObject authenticationInfo =
          new JsonObject()
              .put(TOKEN, request.getHeader(TOKEN))
              .put(iudx.catalogue.server.util.Constants.METHOD, REQUEST_GET)
              .put(API_ENDPOINT, RATINGS_ENDPOINT)
              .put(Constants.ID, host);

      Future<JsonObject> authenticationFuture = inspectToken(authenticationInfo);

      authenticationFuture
          .onSuccess(
              successHandler -> {
                LOGGER.debug(successHandler.getString(Constants.USER_ID));
                requestBody.put(Constants.USER_ID, successHandler.getString(Constants.USER_ID));

                ratingService.getRating(
                    requestBody,
                    handler -> {
                      if (handler.succeeded()) {
                        if (!handler.result().getJsonArray(
                                iudx.catalogue.server.util.Constants.RESULTS).isEmpty()) {
                          response.setStatusCode(200).end(handler.result().toString());
                        } else {
                          response.setStatusCode(204).end();
                        }
                        if (hasAuditService) {
                          updateAuditTable(
                              successHandler, new String[] {id, ROUTE_RATING, REQUEST_GET});
                        }
                      } else {
                        if (handler.cause().getLocalizedMessage().contains("Doc doesn't exist")) {
                          response.setStatusCode(404);
                        } else {
                          response.setStatusCode(400);
                        }
                        response.end(handler.cause().getMessage());
                      }
                    });
              })
          .onFailure(
              failureHandler -> {
                response.setStatusCode(401).end(failureHandler.getMessage());
              });
    }
  }

  /**
   * Update Rating handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void updateRatingHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Updating Rating");

    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthenticationInfo = new JsonObject();

    String id = request.getParam(Constants.ID);

    jwtAuthenticationInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(iudx.catalogue.server.util.Constants.METHOD, REQUEST_PUT)
        .put(API_ENDPOINT, RATINGS_ENDPOINT)
        .put(Constants.ID, host);

    Future<JsonObject> authenticationFuture = inspectToken(jwtAuthenticationInfo);

    authenticationFuture
        .onSuccess(
            successHandler -> {
              requestBody
                  .put(Constants.ID, id)
                  .put(Constants.USER_ID, successHandler.getString(Constants.USER_ID))
                  .put("status", PENDING);

              validatorService.validateRating(
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
                      ratingService.updateRating(
                          requestBody,
                          handler -> {
                            if (handler.succeeded()) {
                              response.setStatusCode(200).end(handler.result().toString());
                              if (hasAuditService) {
                                updateAuditTable(
                                    successHandler, new String[] {id, ROUTE_RATING, REQUEST_PUT});
                              }
                            } else {
                              if (handler
                                  .cause()
                                  .getLocalizedMessage()
                                  .contains("Doc doesn't exist")) {
                                response.setStatusCode(404);
                              } else {
                                response.setStatusCode(400);
                              }
                              response.end(handler.cause().getMessage());
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
   * Delete Rating handler.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void deleteRatingHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Deleting Rating");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject jwtAuthenticationInfo = new JsonObject();

    jwtAuthenticationInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(iudx.catalogue.server.util.Constants.METHOD, REQUEST_DELETE)
        .put(API_ENDPOINT, RATINGS_ENDPOINT)
        .put(Constants.ID, host);

    Future<JsonObject> authenticationFuture = inspectToken(jwtAuthenticationInfo);

    authenticationFuture
        .onSuccess(
            successHandler -> {
              String id = request.getParam(Constants.ID);

              JsonObject requestBody =
                  new JsonObject().put(Constants.USER_ID, successHandler.getString(
                          Constants.USER_ID)).put(Constants.ID, id);

              ratingService.deleteRating(
                  requestBody,
                  dbHandler -> {
                    if (dbHandler.succeeded()) {
                      LOGGER.info("Success: Item deleted;");
                      LOGGER.debug(dbHandler.result().toString());
                      if (dbHandler.result().getString(iudx.catalogue.server.util.Constants.STATUS)
                              .equals(TITLE_SUCCESS)) {
                        response.setStatusCode(200).end(dbHandler.result().toString());
                        if (hasAuditService) {
                          updateAuditTable(
                              successHandler, new String[] {id, ROUTE_RATING, REQUEST_DELETE});
                        }
                      } else {
                        response.setStatusCode(404).end(dbHandler.result().toString());
                      }
                    } else if (dbHandler.failed()) {
                      response.setStatusCode(400).end(dbHandler.cause().getMessage());
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
                    .withDetail(DETAIL_INVALID_TOKEN)
                    .getResponse());
          }
        });
    return promise.future();
  }

  /**
   * function to handle call to audit service.
   *
   * @param jwtDecodedInfo contains the user-role, user-id, iid
   * @param otherInfo contains item-id, api-endpoint and the HTTP method.
   */
  private void updateAuditTable(JsonObject jwtDecodedInfo, String[] otherInfo) {
    LOGGER.info("Updating audit table on successful transaction");
    JsonObject auditInfo = jwtDecodedInfo;
    ZonedDateTime zst = ZonedDateTime.now();
    LOGGER.debug("TIME ZST: " + zst);
    long epochTime = getEpochTime(zst);
    auditInfo
        .put(IUDX_ID, otherInfo[0])
        .put(API, otherInfo[1])
        .put(HTTP_METHOD, otherInfo[2])
        .put(EPOCH_TIME, epochTime)
        .put(USERID, jwtDecodedInfo.getString(USERID));
    LOGGER.debug("audit auditInfo: " + auditInfo);
    auditingService.insertAuditngValuesInRmq(
        auditInfo,
        auditHandler -> {
          if (auditHandler.succeeded()) {
            LOGGER.info("message published in RMQ.");
          } else {
            LOGGER.error("failed to publish message in RMQ.");
          }
        });
  }

  private long getEpochTime(ZonedDateTime zst) {
    return zst.toInstant().toEpochMilli();
  }
}
