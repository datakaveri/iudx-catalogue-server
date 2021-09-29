/**
 * <h1>CrudApis.java</h1>
 * Callback handlers for CRUD
 */

package iudx.catalogue.server.apiserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.HashSet;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.authenticator.Constants.API_ENDPOINT;
import static iudx.catalogue.server.authenticator.Constants.ITEM_ENDPOINT;
import io.vertx.core.http.HttpMethod;
import static iudx.catalogue.server.authenticator.Constants.TOKEN;
import static iudx.catalogue.server.util.Constants.*;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.validator.ValidatorService;
import iudx.catalogue.server.apiserver.util.ResponseHandler;
import iudx.catalogue.server.authenticator.AuthenticationService;


public final class CrudApis {


  private DatabaseService dbService;
  private AuthenticationService authService;
  private ValidatorService validatorService;

  private static final Logger LOGGER = LogManager.getLogger(CrudApis.class);


  /**
   * Crud  constructor
   *
   * @param DBService DataBase Service class
   * @return void
   * @TODO Throw error if load failed
   */
  public CrudApis() {
  }

  public void setDbService(DatabaseService dbService) {
    this.dbService = dbService;
  }

  public void setAuthService(AuthenticationService authService) {
    this.authService = authService;
  }

  public void setValidatorService(ValidatorService validatorService) {
    this.validatorService = validatorService;
  }

  /**
   * Create/Update Item
   *
   * @param context {@link RoutingContext}
   * @TODO Throw error if load failed
   */
  // tag::db-service-calls[]
  public void createItemHandler(RoutingContext routingContext) {

    LOGGER.debug("Info: Creating/Updating item");

    /* Contains the cat-item */
    JsonObject requestBody = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject authenticationInfo = new JsonObject();
    JsonObject jwtAuthenticationInfo = new JsonObject();
    JsonObject authRequest = new JsonObject();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    Set<String> type = new HashSet<String>(new JsonArray().getList());
    try {
      type = new HashSet<String>(requestBody.getJsonArray(TYPE).getList());
    } catch (Exception e) {
      LOGGER.error("Fail: Invalid type");
      response.setStatusCode(400)
              .end("Fail: Invalid type");
    }
    type.retainAll(ITEM_TYPES);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");

    LOGGER.debug("Info: itemType;" + itemType);

    /* checking the operation type */
    String methodType =
        routingContext.request().method().toString() == REQUEST_POST ? INSERT : UPDATE;

    /**
     * Start insertion flow 
     **/

    /** Json schema validate item */
    validatorService.validateSchema(requestBody, schValHandler -> {
      if (schValHandler.failed()) {
        // response.setStatusCode(400).end(schValHandler.cause().getMessage());
        response.setStatusCode(400)
                .end(new ResponseHandler.Builder()
                                        .withStatus(FAILED)
                                        .withResults(requestBody.getString(ID, ""),
                                            methodType, FAILED,
                                            schValHandler.cause().getMessage())
                                        .build()
                                        .toJsonString());
        return;
      }
      if (schValHandler.succeeded()) {
        LOGGER.debug("Success: Schema validation");
        authenticationInfo.put(HEADER_TOKEN,
                                request.getHeader(HEADER_TOKEN))
                                .put(OPERATION, routingContext.request().method().toString());
        // populating jwt authentication info ->
        jwtAuthenticationInfo
                .put(TOKEN,request.getHeader(HEADER_TOKEN)) // getHeader
                .put(METHOD, REQUEST_POST)
                .put(API_ENDPOINT, ITEM_ENDPOINT); //FIXME: not sure about this one, needs verification

        if (itemType.equals(ITEM_TYPE_PROVIDER)) {
          authRequest.put(PROVIDER, requestBody.getString(ID));
          // JWT Auth specific ->
          jwtAuthenticationInfo.put(ID, requestBody.getString(ID));
        } else {
          authRequest.put(PROVIDER, requestBody.getString(PROVIDER));
          // JWT Auth specific ->
          jwtAuthenticationInfo.put(ID, requestBody.getString(PROVIDER));
        }

        LOGGER.debug("Info: AuthRequest;" + authRequest.toString());
        LOGGER.debug("Info: JWT Authentication Info: " + jwtAuthenticationInfo);

        /* JWT implementation of tokenInterospect */
        authService.tokenInterospect(new JsonObject(),
            jwtAuthenticationInfo, authHandler -> {
          if(authHandler.failed()) {
            LOGGER.error("Error: "+authHandler.cause().getMessage());
            response.setStatusCode(401)
                    .end(new ResponseHandler.Builder()
                            .withStatus(FAILED)
                            .withResults(requestBody.getString(ID),
                                    routingContext.request().method().toString(), ERROR,
                                    authHandler.cause().getMessage())
                            .build()
                            .toJsonString());
          } else {
            LOGGER.debug("Success: JWT Auth successful");

            /* Link Validating the request to ensure item correctness */
            validatorService.validateItem(requestBody, valhandler -> {
              if (valhandler.failed()) {
                LOGGER.error("Fail: Item validation failed;" + valhandler.cause().getMessage());
                response.setStatusCode(400)
                        .end(new ResponseHandler.Builder()
                                .withStatus(FAILED)
                                .withResults(requestBody.getString(ID, ""),
                                        methodType, FAILED,
                                        valhandler.cause().getMessage()+ " for " +itemType)
                                .build()
                                .toJsonString());
              }
              if (valhandler.succeeded()) {
                LOGGER.debug("Success: Item link validation");

                /** If post, create. If put, update */
                if (routingContext.request().method().toString() == REQUEST_POST) {
                  /* Requesting database service, creating a item */
                  LOGGER.debug("Info: Inserting item");
                  dbService.createItem(valhandler.result(), dbhandler -> {
                    if (dbhandler.failed()) {
                      LOGGER.error("Fail: Item creation;" + dbhandler.cause().getMessage());
                      response.setStatusCode(400)
                              .end(dbhandler.cause().getMessage());
                    }
                    if (dbhandler.succeeded()) {
                      LOGGER.info("Success: Item created;");
                      response.setStatusCode(201)
                              .end(dbhandler.result().toString());
                      // TODO: call auditing service here
                    }
                  });
                } else {
                  LOGGER.debug("Info: Updating item");
                  /* Requesting database service, creating a item */
                  dbService.updateItem(valhandler.result(), dbhandler -> {
                    if (dbhandler.succeeded()) {
                      LOGGER.info("Success: Item updated;");
                      response.setStatusCode(200)
                              .end(dbhandler.result().toString());
                      // TODO: call auditing service here
                    } else if (dbhandler.failed()) {
                      LOGGER.error("Fail: Item update;" + dbhandler.cause().getMessage());
                      if (dbhandler.cause().getLocalizedMessage().contains("Doc doesn't exist")) {
                        response.setStatusCode(404);
                      } else {
                        response.setStatusCode(400);
                      }
                    }
                    response.end(dbhandler.cause().getMessage());
                  });
                }
              }
            });
          }
        });
      }
    });
  }

  /**
   * Get Item
   *
   * @param context {@link RoutingContext}
   * @TODO Throw error if load failed
   */
  // tag::db-service-calls[]
  public void getItemHandler(RoutingContext routingContext) {

    /* Id in path param */
    HttpServerResponse response = routingContext.response();

    String itemId = routingContext.queryParams().get(ID);

    LOGGER.debug("Info: Getting item; id=" + itemId);

    JsonObject requestBody = new JsonObject().put(ID, itemId);
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    
    if (validateId(itemId) == false) {
      dbService.getItem(requestBody, dbhandler -> {
        if (dbhandler.succeeded()) {
          if(dbhandler.result().getInteger(TOTAL_HITS) == 0) {
            LOGGER.error("Fail: Item not found");
            JsonObject result = dbhandler.result();
            result.put(STATUS, ERROR);
            response.setStatusCode(404)
                    .end(dbhandler.result().toString());
          } else {
            LOGGER.info("Success: Retreived item");
            response.setStatusCode(200)
                    .end(dbhandler.result().toString());
          }
        } else if (dbhandler.failed()) {
          LOGGER.error("Fail: Item not found;" + dbhandler.cause().getMessage());
          response.setStatusCode(400)
              .end(dbhandler.cause().getMessage());
        }
      });
    } else {
      LOGGER.error("Fail: Invalid request payload");
      response.setStatusCode(400)
              .end(new ResponseHandler
                  .Builder()
                  .withStatus(INVALID_VALUE)
                  .build()
                  .toJsonString());
    }
  }


  /**
   * Delete Item
   *
   * @param context {@link RoutingContext}
   * @TODO Throw error if load failed
   */
  // tag::db-service-calls[]
  public void deleteItemHandler(RoutingContext routingContext) {

    /* Id in path param */
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    /* JsonObject of authentication related information */
    JsonObject authenticationInfo = new JsonObject();
    JsonObject jwtAuthenticationInfo = new JsonObject();
    JsonObject requestBody = new JsonObject();

    String itemId = routingContext.queryParams().get(ID);
    requestBody.put(ID, itemId);

    LOGGER.debug("Info: Deleting item; id=" + itemId);

    if (validateId(itemId) == false) {
      String providerId = String.join("/", Arrays.copyOfRange(itemId.split("/"), 0, 2));
      LOGGER.debug("Info: Provider ID is  " + providerId);

      JsonObject authRequest = new JsonObject().put(PROVIDER, providerId);
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN)).put(OPERATION,
          REQUEST_DELETE);

      // populating JWT authentication info ->
      jwtAuthenticationInfo
              .put(TOKEN, request.getHeader(HEADER_TOKEN))
                      .put(METHOD,request.method().toString())
                              .put(API_ENDPOINT,request.toString())
                                      .put(ID,providerId);

      /* JWT implementation of tokenInterospect */
      authService.tokenInterospect(new JsonObject(), jwtAuthenticationInfo, authHandler -> {
        if(authHandler.failed()) {
          LOGGER.error("Error: "+authHandler.cause().getMessage());
          response.setStatusCode(401)
                  .end(new ResponseHandler.Builder()
                          .withStatus(FAILED)
                          .withResults(requestBody.getString(ID),
                                  routingContext.request().method().toString(), ERROR,
                                  authHandler.cause().getMessage())
                          .build()
                          .toJsonString());
        } else {
          LOGGER.debug("Success: JWT Auth successful");

          /* Requesting database service, deleting a item */
          dbService.deleteItem(requestBody, dbHandler -> {
            if (dbHandler.succeeded()) {
              LOGGER.info("Success: Item deleted;");
              if (dbHandler.result().getString(STATUS).equals(SUCCESS)) {
                response.setStatusCode(200).end(dbHandler.result().toString());
                // TODO: call auditing service here
              } else if (dbHandler.result().getString(STATUS).equals(ERROR)) {
                response.setStatusCode(404)
                        .end(dbHandler.result().toString());
              }
            } else if (dbHandler.failed()) {
              response.setStatusCode(400)
                      .end(dbHandler.cause().getMessage());
            }
          });
        }
      });

      /* Authenticating the request */
      authService.tokenInterospect(authRequest, authenticationInfo, authhandler -> {
        if (authhandler.result().getString(STATUS).equals(SUCCESS)) {
          LOGGER.debug("Success: Authenticated item deletion request");
          /* Requesting database service, creating a item */
          dbService.deleteItem(requestBody, dbhandler -> {
            if (dbhandler.succeeded()) {
              LOGGER.info("Success: Item deleted;");
              if (dbhandler.result().getString(STATUS).equals(SUCCESS)) {
                response.setStatusCode(200).end(dbhandler.result().toString());
                // TODO: call auditing service here
              } else if (dbhandler.result().getString(STATUS).equals(ERROR)) {
                response.setStatusCode(404)
                        .end(dbhandler.result().toString());
              }
            } else if (dbhandler.failed()) {
              response.setStatusCode(400)
                      .end(dbhandler.cause().getMessage());
            }
          });
        } else {
          LOGGER.error("Fail: Unathorized request" + authhandler.result());
          response.setStatusCode(401)
              .end(new ResponseHandler.Builder().withStatus(FAILED)
                  .withResults(itemId, DELETE, ERROR, authhandler.result().getString(MESSAGE))
                  .build()
                  .toJsonString());
        }
      });
    } else {
      LOGGER.error("Fail: Invalid request payload");
      response.setStatusCode(400)
              .end(new ResponseHandler
                  .Builder()
                  .withStatus(INVALID_VALUE)
                  .build()
                  .toJsonString());
    }
  }

  public void createInstanceHandler(RoutingContext routingContext, String catAdmin) {

    LOGGER.debug("Info: Creating new instance");

    HttpServerResponse response = routingContext.response();
    HttpServerRequest request = routingContext.request();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);


    JsonObject authenticationInfo = new JsonObject();
    JsonObject authRequest = new JsonObject();

    String instance = routingContext.queryParams().get(ID);


    /**
     * Start insertion flow 
     **/

    /** Json schema validate item */
    authenticationInfo.put(HEADER_TOKEN,
                            request.getHeader(HEADER_TOKEN))
                            .put(OPERATION, routingContext.request().method().toString());
    authRequest.put(PROVIDER, catAdmin);
    /** Introspect token and authorize operation */
    authService.tokenInterospect(authRequest, authenticationInfo, authhandler -> {
      if (authhandler.failed()) {
          response.setStatusCode(401)
              .end(new ResponseHandler.Builder()
                                  .withStatus(FAILED)
                                  .withResults(instance, 
                                      INSERT, ERROR,
                                      authhandler.cause().getMessage())
                                  .build()
                                  .toJsonString());
        return;
      }
      else if (authhandler.result().getString(STATUS).equals(ERROR)) {
        LOGGER.error("Fail: Authentication;" 
                      + authhandler.result().getString(MESSAGE));
        response.setStatusCode(401)
          .end(new ResponseHandler.Builder()
                            .withStatus(FAILED)
                            .withResults(instance, 
                                INSERT, ERROR,
                                authhandler.result().getString(MESSAGE))
                            .build()
                            .toJsonString());
      }
      else if (authhandler.result().getString(STATUS).equals(SUCCESS)) {
        /* INSTANCE = "" to make sure createItem can be used for onboarding instance and items */
        JsonObject body = new JsonObject().put(ID, instance)
                                          .put(TYPE, new JsonArray().add(ITEM_TYPE_INSTANCE))
                                          .put(INSTANCE, "");
        dbService.createItem(body, res -> {
          if (res.succeeded()) {
            LOGGER.info("Success: Instance created;");
            response.setStatusCode(201)
              .end(res.result().toString());
            // TODO: call auditing service here
          } else {
            LOGGER.error("Fail: Creating instance");
            response.setStatusCode(400).end(res.cause().getMessage());
          }
        });
        LOGGER.debug("Success: Authenticated instance creation request");
      }
    });
  }

  public void deleteInstanceHandler(RoutingContext routingContext, String catAdmin) {

    LOGGER.debug("Info: Deleting instance");

    HttpServerResponse response = routingContext.response();
    HttpServerRequest request = routingContext.request();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject authenticationInfo = new JsonObject();
    JsonObject authRequest = new JsonObject();

    String instance = routingContext.queryParams().get(ID);

    /**
     * Start insertion flow 
     **/

    /** Json schema validate item */
    authenticationInfo.put(HEADER_TOKEN,
                            request.getHeader(HEADER_TOKEN))
                            .put(OPERATION, routingContext.request().method().toString());
    authRequest.put(PROVIDER, catAdmin);
    /** Introspect token and authorize operation */
    authService.tokenInterospect(authRequest, authenticationInfo, authhandler -> {
      if (authhandler.failed()) {
        response.setStatusCode(401)
            .end(new ResponseHandler.Builder()
                            .withStatus(FAILED)
                            .withResults(instance, 
                                INSERT, ERROR,
                                authhandler.cause().getMessage())
                            .build()
                            .toJsonString());
        return;
      }
      else if (authhandler.result().getString(STATUS).equals(ERROR)) {
        LOGGER.error("Fail: Authentication;" 
                      + authhandler.result().getString(MESSAGE));
        response.setStatusCode(401)
          .end(new ResponseHandler.Builder()
                          .withStatus(FAILED)
                          .withResults(instance, 
                              INSERT, ERROR,
                              authhandler.result().getString(MESSAGE))
                          .build()
                          .toJsonString());
      }
      else if (authhandler.result().getString(STATUS).equals(SUCCESS)) {
        /* INSTANCE = "" to make sure createItem can be used for onboarding instance and items */
        JsonObject body = new JsonObject().put(ID, instance)
                                          .put(INSTANCE, "");
        dbService.deleteItem(body, res -> {
          if (res.succeeded()) {
            LOGGER.info("Success: Instance deleted;");
            response.setStatusCode(200)
              .end(res.result().toString());
            // TODO: call auditing service here
          }
        });
        LOGGER.debug("Success: Authenticated instance creation request");
      }
    });
  }

  /**
   * Check if the itemId contains certain invalid characters.
   * 
   * @param itemId
   * @return
   */
  private boolean validateId(String itemId) {
    Pattern pattern = Pattern.compile("[<>;=]");
    boolean flag = pattern.matcher(itemId).find();

    return flag;
  }
}
