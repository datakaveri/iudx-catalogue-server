package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.API_ENDPOINT;
import static iudx.catalogue.server.authenticator.Constants.TOKEN;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.METHOD;
import static iudx.catalogue.server.validator.Constants.INVALID_SCHEMA_MSG;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.stack.StackServiceImpl;
import iudx.catalogue.server.apiserver.stack.StackSevice;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.auditing.AuditingService;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.util.Api;
import iudx.catalogue.server.validator.ValidatorService;
import java.time.ZonedDateTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StackRestApi {
  private static final Logger LOGGER = LogManager.getLogger(StackRestApi.class);

  private AuthenticationService authService;
  private ValidatorService validatorService;
  private AuditingService auditingService;
  private Api api;
  private Router router;
  private ElasticClient elasticClient;
  private StackSevice stackSevice;
  private RespBuilder respBuilder;

  public StackRestApi(
      Router router,
      Api api,
      JsonObject config,
      ValidatorService validatorService,
      AuthenticationService authService,
      AuditingService auditingService) {
    this.api = api;
    this.router = router;
    this.authService = authService;
    this.validatorService = validatorService;
    this.auditingService = auditingService;
    elasticClient =
        new ElasticClient(
            config.getString("databaseIP"),
            config.getInteger("databasePort"),
            config.getString("docIndex"),
            config.getString("databaseUser"),
            config.getString("databasePassword"));
    stackSevice = new StackServiceImpl(elasticClient, config.getString("docIndex"));
  }

  Router init() {
    router
        .post(api.getStackRestApis())
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_TOKEN)) {
                handlePostStackRequest(routingContext);
              } else {
                LOGGER.warn("Unauthorized CRUD operation");
                HttpServerResponse response = routingContext.response();
                respBuilder =
                    new RespBuilder()
                        .withType(TYPE_MISSING_TOKEN)
                        .withTitle("Not authorized")
                        .withDetail("Token needed but not present");
                response.setStatusCode(400).end(respBuilder.getResponse());
              }
            });
    router
        .patch(api.getStackRestApis())
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_TOKEN)) {
                handlePatchStackRequest(routingContext);
              } else {
                LOGGER.warn("Unauthorized CRUD operation");
                HttpServerResponse response = routingContext.response();
                respBuilder =
                    new RespBuilder()
                        .withType(TYPE_MISSING_TOKEN)
                        .withTitle("Not authorized")
                        .withDetail("Token needed but not present");
                response.setStatusCode(400).end(respBuilder.getResponse());
              }
            });

    router
        .get(api.getStackRestApis())
        .handler(routingContext -> handleGetStackRequest(routingContext));
    router
        .delete(api.getStackRestApis())
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_TOKEN)) {
                deleteStackHandler(routingContext);
              } else {
                LOGGER.warn("Unathorized CRUD operation");
                HttpServerResponse response = routingContext.response();
                respBuilder =
                    new RespBuilder()
                        .withType(TYPE_MISSING_TOKEN)
                        .withTitle("Not authorized")
                        .withDetail("Token needed but not present");
                response.setStatusCode(400).end(respBuilder.getResponse());
              }
            });

    return this.router;
  }

  public void handleGetStackRequest(RoutingContext routingContext) {
    LOGGER.debug("method HandleGetStackRequest() started");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String stackId = routingContext.queryParams().get(ID);
    LOGGER.debug("stackId:: {}", stackId);
    if (validateId(stackId)) {
      stackSevice
          .get(stackId)
          .onComplete(
              stackHandler -> {
                if (stackHandler.succeeded()) {
                  JsonObject resultJson = stackHandler.result();
                  handleSuccessResponse(response, 200, resultJson.toString());
                } else {
                  LOGGER.error("Fail: Stack not found;" + stackHandler.cause().getMessage());
                  processBackendResponse(response, stackHandler.cause().getMessage());
                }
              });

    } else {
      respBuilder =
          new RespBuilder()
              .withType(TYPE_INVALID_UUID)
              .withTitle(TITLE_INVALID_UUID)
              .withDetail("The id is invalid");
      LOGGER.error("Error invalid id : {}", stackId);
      processBackendResponse(response, respBuilder.getResponse());
    }
  }

  public void handlePostStackRequest(RoutingContext routingContext) {
    LOGGER.debug("method handlePostStackRequest() started");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    HttpServerRequest request = routingContext.request();
    JsonObject requestBody = routingContext.body().asJsonObject();
    JsonObject validationJson = requestBody.copy();
    validationJson.put("stack_type", "post:Stack");

    validatorService.validateSchema(
        validationJson,
        validationHandler -> {
          if (validationHandler.succeeded()) {
            JsonObject jwtAuthenticationInfo =
                new JsonObject()
                    .put(TOKEN, request.getHeader(HEADER_TOKEN))
                    .put(METHOD, REQUEST_POST)
                    .put(API_ENDPOINT, api.getStackRestApis());
            authService.tokenInterospect(
                new JsonObject(),
                jwtAuthenticationInfo,
                authHandler -> {
                  if (authHandler.succeeded()) {
                    JsonObject authInfo = authHandler.result();
                    LOGGER.info("authInfo: " + authInfo);

                    stackSevice
                        .create(requestBody)
                        .onComplete(
                            stackHandler -> {
                              if (stackHandler.succeeded()) {
                                JsonObject resultJson = stackHandler.result();
                                response.setStatusCode(200).end(resultJson.toString());
                              } else {
                                LOGGER.error(
                                    "Fail: DB request has failed;"
                                        + stackHandler.cause().getMessage());
                                processBackendResponse(response, stackHandler.cause().getMessage());
                              }
                            });
                  } else {
                    LOGGER.error("auth failure: " + authHandler.cause().getMessage());
                    respBuilder =
                        new RespBuilder()
                            .withType(TYPE_TOKEN_INVALID)
                            .withTitle(TITLE_TOKEN_INVALID)
                            .withDetail(authHandler.cause().getMessage());
                    processBackendResponse(response, respBuilder.getResponse());
                  }
                });

          } else {
            respBuilder =
                new RespBuilder()
                    .withType(TYPE_INVALID_SCHEMA)
                    .withTitle(INVALID_SCHEMA_MSG)
                    .withDetail(validationHandler.cause().getMessage());
            processBackendResponse(response, respBuilder.getResponse());
          }
        });
  }

  public void handlePatchStackRequest(RoutingContext routingContext) {
    LOGGER.debug("method handlePatchStackRequest() started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject requestBody = routingContext.body().asJsonObject();
    JsonObject validationJson = requestBody.copy();
    validationJson.put("stack_type", "patch:Stack");

    validatorService.validateSchema(
        validationJson,
        valHandler -> {
          if (valHandler.succeeded()) {
            JsonObject jwtAuthenticationInfo =
                new JsonObject()
                    .put(TOKEN, request.getHeader(HEADER_TOKEN))
                    .put(METHOD, REQUEST_PATCH)
                    .put(API_ENDPOINT, api.getStackRestApis());
            authService.tokenInterospect(
                new JsonObject(),
                jwtAuthenticationInfo,
                authHandler -> {
                  if (authHandler.succeeded()) {
                    JsonObject authInfo = authHandler.result();
                    LOGGER.info("authInfo: " + authInfo);
                    stackSevice
                        .update(requestBody)
                        .onComplete(
                            updateHandler -> {
                              if (updateHandler.succeeded()) {
                                JsonObject resultJson = updateHandler.result();
                                handleSuccessResponse(response, 201, resultJson.toString());
                              } else {
                                processBackendResponse(
                                    response, updateHandler.cause().getMessage());
                              }
                            });
                  } else {
                    LOGGER.error("auth failure: " + authHandler.cause().getMessage());
                    respBuilder =
                        new RespBuilder()
                            .withType(TYPE_TOKEN_INVALID)
                            .withTitle(TITLE_TOKEN_INVALID)
                            .withDetail(authHandler.cause().getMessage());
                    processBackendResponse(response, respBuilder.getResponse());
                  }
                });

          } else {
            respBuilder =
                new RespBuilder()
                    .withType(TYPE_INVALID_SCHEMA)
                    .withTitle(INVALID_SCHEMA_MSG)
                    .withDetail(valHandler.cause().getMessage());
            processBackendResponse(response, respBuilder.getResponse());
          }
        });
  }

  public void deleteStackHandler(RoutingContext routingContext) {
    LOGGER.debug("method deleteStackHandler() started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String stackId = routingContext.queryParams().get(ID);
    LOGGER.debug("stackId:: {}", stackId);
    if (validateId(stackId)) {
      JsonObject jwtAuthenticationInfo =
          new JsonObject()
              .put(TOKEN, request.getHeader(HEADER_TOKEN))
              .put(METHOD, REQUEST_PATCH)
              .put(API_ENDPOINT, api.getStackRestApis());

      authService.tokenInterospect(
          new JsonObject(),
          jwtAuthenticationInfo,
          authHandler -> {
            if (authHandler.succeeded()) {
              JsonObject authInfo = authHandler.result();
              LOGGER.info("authInfo: " + authInfo);
              stackSevice
                  .delete(stackId)
                  .onComplete(
                      deleteHandler -> {
                        if (deleteHandler.succeeded()) {
                          JsonObject result = deleteHandler.result();
                          Future.future(fu -> updateAuditTable(authInfo));
                          handleSuccessResponse(response, 200, result.toString());
                        } else {
                          LOGGER.error(
                              "Fail: request has failed;" + deleteHandler.cause().getMessage());
                          processBackendResponse(response, deleteHandler.cause().getMessage());
                        }
                      });
            } else {
              LOGGER.error("auth failure: " + authHandler.cause().getMessage());
              respBuilder =
                  new RespBuilder()
                      .withType(TYPE_TOKEN_INVALID)
                      .withTitle(TITLE_TOKEN_INVALID)
                      .withDetail(authHandler.cause().getMessage());
              processBackendResponse(response, respBuilder.getResponse());
            }
          });
    } else {
      respBuilder =
          new RespBuilder()
              .withType(TYPE_INVALID_UUID)
              .withTitle(TITLE_INVALID_UUID)
              .withDetail("The id is invalid");
      LOGGER.error("Invalid id : {}", stackId);
      processBackendResponse(response, respBuilder.getResponse());
    }
  }

  private boolean validateId(String itemId) {
    return UUID_PATTERN.matcher(itemId).matches();
  }

  private void processBackendResponse(HttpServerResponse response, String failureMessage) {
    int statusCode;

    try {
      JsonObject json = new JsonObject(failureMessage);
      switch (json.getString("type")) {
        case TYPE_ITEM_NOT_FOUND:
          statusCode = 404;
          break;
        case TYPE_CONFLICT:
          statusCode = 409;
          break;
        case TYPE_TOKEN_INVALID:
          statusCode = 401;
          break;
        case TYPE_INVALID_UUID:
        case TYPE_INVALID_SCHEMA:
          statusCode = 400;
          break;
        default:
          statusCode = 500;
          break;
      }
      response.setStatusCode(statusCode).end(failureMessage);
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
      response.setStatusCode(400).end(BAD_REQUEST);
    }
  }

  private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  private void updateAuditTable(JsonObject jwtDecodedInfo) {
    LOGGER.info("Updating audit table on successful transaction");

    JsonObject auditInfo = jwtDecodedInfo;
    ZonedDateTime zst = ZonedDateTime.now();
    LOGGER.debug("TIME ZST: " + zst);
    long epochTime = getEpochTime(zst);
    /*auditInfo
            .put(IUDX_ID, otherInfo[0])
            .put(API, otherInfo[1])
            .put(HTTP_METHOD, otherInfo[2])
            .put(EPOCH_TIME, epochTime)
            .put(USERID, jwtDecodedInfo.getString(USER_ID));
    */
    LOGGER.debug("audit data: " + auditInfo.encodePrettily());
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
