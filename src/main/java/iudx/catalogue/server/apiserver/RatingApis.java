package iudx.catalogue.server.apiserver;

import iudx.catalogue.server.auditing.AuditingService;
import iudx.catalogue.server.rating.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import static iudx.catalogue.server.apiserver.util.Constants.HEADER_TOKEN;

import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_RATING;
import static iudx.catalogue.server.auditing.util.Constants.API;
import static iudx.catalogue.server.auditing.util.Constants.IUDX_ID;
import static iudx.catalogue.server.authenticator.Constants.TOKEN;
import static iudx.catalogue.server.authenticator.Constants.API_ENDPOINT;
import static iudx.catalogue.server.authenticator.Constants.RATINGS_ENDPOINT;
import static iudx.catalogue.server.rating.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.STATUS;

import iudx.catalogue.server.validator.ValidatorService;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.rating.RatingService;

public class RatingApis {
  private static final Logger LOGGER = LogManager.getLogger(RatingApis.class);
  private AuthenticationService authService;
  private ValidatorService validatorService;
  private AuditingService auditingService;
  private RatingService ratingService;

  private boolean hasAuditService = false;

  private String host;

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
   * Create Rating handler
   *
   * @param routingContext {@link RoutingContext}
   */
  public void createRatingHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Creating Rating");

    JsonObject requestBody = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject jwtAuthenticationInfo = new JsonObject();

    String id = request.getParam(ID);

    jwtAuthenticationInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_POST)
        .put(API_ENDPOINT, RATINGS_ENDPOINT)
        .put(ID, host);

    authService.tokenInterospect(
        new JsonObject(),
        jwtAuthenticationInfo,
        authHandler -> {
          if (authHandler.failed()) {
            LOGGER.error("Error: " + authHandler.cause().getMessage());
            response
                .setStatusCode(401)
                .end(
                    new RespBuilder()
                        .withType(TYPE_TOKEN_INVALID)
                        .withTitle(TITLE_TOKEN_INVALID)
                        .withDetail(authHandler.cause().getMessage())
                        .getResponse());
          } else {
            LOGGER.debug("Success: JWT Auth successful");

            requestBody
                .put(ID, id)
                .put(USER_ID, authHandler.result().getString(USER_ID))
                .put("status", PENDING);

            LOGGER.debug(requestBody.encodePrettily());

            ratingService.createRating(
                requestBody,
                handler -> {
                  if (handler.succeeded()) {
                    response.setStatusCode(201).end(handler.result().toString());
                    if (hasAuditService) {
                      updateAuditTable(
                          authHandler.result(), new String[] {id, ROUTE_RATING, REQUEST_POST});
                    }
                  } else {
                    if (handler.cause().getLocalizedMessage().contains("Doc Already Exists")) {
                      response.setStatusCode(400);
                    } else {
                      response.setStatusCode(500);
                    }
                    response.end(handler.cause().getMessage());
                  }
                });
          }
        });
  }

  /**
   * GET Rating handler
   *
   * @param routingContext {@link RoutingContext}
   */
  public void getRatingHandler(RoutingContext routingContext) {}

  /**
   * Update Rating handler
   *
   * @param routingContext {@link RoutingContext}
   */
  public void updateRatingHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Updating Rating");

    JsonObject requestBody = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject jwtAuthenticationInfo = new JsonObject();

    String id = request.getParam(ID);

    jwtAuthenticationInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_PUT)
        .put(API_ENDPOINT, RATINGS_ENDPOINT)
        .put(ID, host);

    authService.tokenInterospect(
        new JsonObject(),
        jwtAuthenticationInfo,
        authHandler -> {
          if (authHandler.failed()) {
            LOGGER.error("Error: " + authHandler.cause().getMessage());
            response
                .setStatusCode(401)
                .end(
                    new RespBuilder()
                        .withType(TYPE_TOKEN_INVALID)
                        .withTitle(TITLE_TOKEN_INVALID)
                        .withDetail(authHandler.cause().getMessage())
                        .getResponse());
          } else {
            LOGGER.debug("Success: JWT Auth successful");

            requestBody
                .put(Constants.ID, id)
                .put(USER_ID, authHandler.result().getString(USER_ID))
                .put("status", PENDING);

            ratingService.updateRating(
                requestBody,
                handler -> {
                  if (handler.succeeded()) {
                    response.setStatusCode(200).end(handler.result().toString());
                    if (hasAuditService) {
                      updateAuditTable(
                          authHandler.result(), new String[] {id, ROUTE_RATING, REQUEST_PUT});
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
          }
        });
  }

  /**
   * Delete Rating handler
   *
   * @param routingContext {@link RoutingContext}
   */
  public void deleteRatingHandler(RoutingContext routingContext) {
    LOGGER.debug("Info: Deleting Rating");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject jwtAuthenticationInfo = new JsonObject();

    jwtAuthenticationInfo
        .put(TOKEN, request.getHeader(HEADER_TOKEN))
        .put(METHOD, REQUEST_DELETE)
        .put(API_ENDPOINT, RATINGS_ENDPOINT)
        .put(ID, host);

    authService.tokenInterospect(
        new JsonObject(),
        jwtAuthenticationInfo,
        authHandler -> {
          if (authHandler.failed()) {
            LOGGER.error("Error: " + authHandler.cause().getMessage());
            response
                .setStatusCode(401)
                .end(
                    new RespBuilder()
                        .withType(TYPE_TOKEN_INVALID)
                        .withTitle(TITLE_TOKEN_INVALID)
                        .withDetail(authHandler.cause().getMessage())
                        .getResponse());
          } else {
            LOGGER.debug("Success: JWT Auth successful");

            String id = request.getParam(ID);

            JsonObject requestBody =
                new JsonObject().put(USER_ID, authHandler.result().getString(USER_ID)).put(ID, id);

            ratingService.deleteRating(
                requestBody,
                dbHandler -> {
                  if (dbHandler.succeeded()) {
                    LOGGER.info("Success: Item deleted;");
                    LOGGER.debug(dbHandler.result().toString());
                    if (dbHandler.result().getString(STATUS).equals(TITLE_SUCCESS)) {
                      response.setStatusCode(200).end(dbHandler.result().toString());
                      if (hasAuditService) {
                        updateAuditTable(
                            authHandler.result(), new String[] {id, ROUTE_RATING, REQUEST_DELETE});
                      }
                    } else {
                      response.setStatusCode(404).end(dbHandler.result().toString());
                    }
                  } else if (dbHandler.failed()) {
                    response.setStatusCode(400).end(dbHandler.cause().getMessage());
                  }
                });
          }
        });
  }

  /**
   * function to handle call to audit service
   *
   * @param jwtDecodedInfo contains the user-role, user-id, iid
   * @param otherInfo contains item-id, api-endpoint and the HTTP method.
   */
  private void updateAuditTable(JsonObject jwtDecodedInfo, String[] otherInfo) {
    LOGGER.info("Updating audit table on successful transaction");
    JsonObject auditInfo = jwtDecodedInfo;
    auditInfo.put(IUDX_ID, otherInfo[0]).put(API, otherInfo[1]).put("httpMethod", otherInfo[2]);
    LOGGER.debug("audit data: " + auditInfo.encodePrettily());
    auditingService.executeWriteQuery(
        auditInfo,
        auditHandler -> {
          if (auditHandler.succeeded()) {
            LOGGER.info("audit table updated");
          } else {
            LOGGER.error("failed to update audit table");
          }
        });
  }
}
