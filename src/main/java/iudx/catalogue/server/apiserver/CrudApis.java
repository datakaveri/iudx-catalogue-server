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
import java.util.HashSet;

import static iudx.catalogue.server.apiserver.util.Constants.*;

import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.validator.ValidatorService;
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
    JsonObject authRequest = new JsonObject();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    Set<String> type = new HashSet<String>(new JsonArray().getList());
    try {
      type = new HashSet<String>(requestBody.getJsonArray(TYPE).getList());
    } catch (Exception e) {
      LOGGER.error("Fail: Invalid type");
      response.setStatusCode(500)
              .end("Fail: Invalid type");
    }
    type.retainAll(ITEM_TYPES);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");

    if (itemType != ITEM_TYPE_PROVIDER)  {
      String instanceID = request.getHeader(HEADER_INSTANCE);
      if (instanceID == null || instanceID == "") {
      requestBody.put(HEADER_INSTANCE, "");
      }
      requestBody.put(HEADER_INSTANCE, instanceID);
    }

    /**
     * Start insertion flow 
     **/

    /** Json schema validate item */
    validatorService.validateSchema(requestBody, schValHandler -> {
      if (schValHandler.failed()) {
        LOGGER.error("Fail: Item validation");
        response.setStatusCode(400).end();
        return;
      }
      if (schValHandler.succeeded()) {
        LOGGER.debug("Success: Schema validation");
        authenticationInfo.put(HEADER_TOKEN,
                                request.getHeader(HEADER_TOKEN))
                                .put(OPERATION, routingContext.request().method().toString());
        if (itemType == ITEM_TYPE_PROVIDER) {
          authRequest.put(REL_PROVIDER, requestBody.getString(ID));
        } else {
          authRequest.put(REL_PROVIDER, requestBody.getString(REL_PROVIDER));
        }
        /** Introspect token and authorize operation */
        authService.tokenInterospect(authRequest, authenticationInfo, authhandler -> {
          if (authhandler.failed()) {
            response.setStatusCode(401)
                    .end(authhandler.cause().toString());
            return;
          }
          else if (authhandler.result().getString(STATUS).equals(ERROR)) {
            LOGGER.error("Fail: Authentication;" 
                          + authhandler.result().getString(MESSAGE));
            response.setStatusCode(401).end();
          }
          else if (authhandler.result().getString(STATUS).equals(SUCCESS)) {
            LOGGER.debug("Success: Authenticated item creation request");

            /* Link Validating the request to ensure item correctness */
            validatorService.validateItem(requestBody, valhandler -> {
              if (valhandler.failed()) {
                LOGGER.error("Fail: Item validation failed;"
                              .concat(valhandler.cause().toString()));
                response.setStatusCode(500)
                        .end(valhandler.cause().toString());
              }
              if (valhandler.succeeded()) {
                LOGGER.debug("Success: Item link validation");

                /** If post, create. If put, update */
                if (routingContext.request().method().toString() == POST) {
                  /* Requesting database service, creating a item */
                  LOGGER.debug("Info: Inserting item");
                  dbService.createItem(valhandler.result(), dbhandler -> {
                    if (dbhandler.failed()) {
                      LOGGER.error("Fail: Item creation;"
                                    .concat(dbhandler.cause().toString()));
                      response.setStatusCode(500)
                              .end(dbhandler.cause().toString());
                    }
                    if (dbhandler.succeeded()) {
                      LOGGER.info("Success: Item created;"
                                  .concat(dbhandler.result().toString()));
                      response.setStatusCode(201)
                              .end(dbhandler.result().toString());
                    }
                  });
                } else {
                  LOGGER.debug("Info: Updating item");
                  /* Requesting database service, creating a item */
                  dbService.updateItem(valhandler.result(), dbhandler -> {
                    if (dbhandler.succeeded()) {
                      LOGGER.info("Success: Item updated;"
                                  .concat(dbhandler.result().toString()));
                      response.setStatusCode(200)
                              .end(dbhandler.result().toString());
                    } else if (dbhandler.failed()) {
                      LOGGER.error("Fail: Item update;"
                                    .concat(dbhandler.cause().toString()));
                      response.setStatusCode(500)
                              .end(dbhandler.cause().toString());
                    }
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

    LOGGER.debug("Info: Getting item; id=".concat(itemId));

    JsonObject requestBody = new JsonObject().put(ID, itemId);

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    dbService.getItem(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("Success: Retreived item");
        response.setStatusCode(200)
                .end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Fail: Item not found;"
                      .concat(dbhandler.cause().toString()));
        response.setStatusCode(400)
                .end(dbhandler.cause().toString());
      }
    });
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

    /* JsonObject of authentication related information */
    JsonObject authenticationInfo = new JsonObject();
    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_INSTANCE);
    String itemId = routingContext.queryParams().get(ID);

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    requestBody.put(HEADER_INSTANCE, instanceID);
    requestBody.put(ID, itemId);

    LOGGER.debug("Info: Deleting item; id=".concat(itemId));


    String providerId = String.join("/",
                          Arrays.copyOfRange(itemId.split("/"), 0, 2));
    LOGGER.debug("Info: Provider ID is  " + providerId);

    JsonObject authRequest = new JsonObject().put(REL_PROVIDER, providerId);
    authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN))
                                                .put(OPERATION, DELETE);

    /* Authenticating the request */
    authService.tokenInterospect(authRequest, authenticationInfo, authhandler -> {
      if (authhandler.result().getString(STATUS).equals(SUCCESS)) {
        LOGGER.debug("Success: Authenticated item deletion request");
        /* Requesting database service, creating a item */
        dbService.deleteItem(requestBody, dbhandler -> {
          if (dbhandler.succeeded()) {
            LOGGER.info("Success: Item deleted;"
                        .concat(dbhandler.result().toString()));
            response.setStatusCode(200)
                    .end(dbhandler.result().toString());
          } else if (dbhandler.failed()) {
            LOGGER.error("Fail: Item deletion;"
                          .concat(dbhandler.cause().toString()));
            response.setStatusCode(400)
                    .end(dbhandler.cause().toString());
          }
        });
      } else {
        LOGGER.error("Fail: Unathorized request"
                      .concat(authhandler.cause().toString()));
        response.setStatusCode(401)
                .end(authhandler.cause().toString());
      }
    });
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
    authRequest.put(REL_PROVIDER, catAdmin);
    /** Introspect token and authorize operation */
    authService.tokenInterospect(authRequest, authenticationInfo, authhandler -> {
      if (authhandler.failed()) {
          response.setStatusCode(401)
                .end(authhandler.cause().toString());
        return;
      }
      else if (authhandler.result().getString(STATUS).equals(ERROR)) {
        LOGGER.error("Fail: Authentication;" 
                      + authhandler.result().getString(MESSAGE));
        response.setStatusCode(401).end();
      }
      else if (authhandler.result().getString(STATUS).equals(SUCCESS)) {
        /* INSTANCE = "" to make sure createItem can be used for onboarding instance and items */
        JsonObject body = new JsonObject().put(ID, instance)
                                          .put(TYPE, new JsonArray().add(ITEM_TYPE_INSTANCE))
                                          .put(INSTANCE, "");
        dbService.createItem(body, res -> {
          if (res.succeeded()) {
            LOGGER.info("Success: Instance created;"
                .concat(res.result().toString()));
            response.setStatusCode(201)
              .end(res.result().toString());
          } else {
            LOGGER.error("Fail: Creating instance");
            response.setStatusCode(500).end();
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
    authRequest.put(REL_PROVIDER, catAdmin);
    /** Introspect token and authorize operation */
    authService.tokenInterospect(authRequest, authenticationInfo, authhandler -> {
      if (authhandler.failed()) {
          response.setStatusCode(401)
                .end(authhandler.cause().toString());
        return;
      }
      else if (authhandler.result().getString(STATUS).equals(ERROR)) {
        LOGGER.error("Fail: Authentication;" 
                      + authhandler.result().getString(MESSAGE));
        response.setStatusCode(401).end();
      }
      else if (authhandler.result().getString(STATUS).equals(SUCCESS)) {
        /* INSTANCE = "" to make sure createItem can be used for onboarding instance and items */
        JsonObject body = new JsonObject().put(ID, instance)
                                          .put(INSTANCE, "");
        dbService.deleteItem(body, res -> {
          if (res.succeeded()) {
            LOGGER.info("Success: Instance deleted;"
                .concat(res.result().toString()));
            response.setStatusCode(200)
              .end(res.result().toString());
          }
        });
        LOGGER.debug("Success: Authenticated instance creation request");
      }
    });
  }
}
