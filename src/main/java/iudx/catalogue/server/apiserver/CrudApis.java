/**
 * <h1>CrudApis.java</h1>
 * Callback handlers for CRUD
 */

package iudx.catalogue.server.apiserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.ext.web.RoutingContext;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import iudx.catalogue.server.apiserver.util.QueryMapper;
import iudx.catalogue.server.apiserver.util.ResponseHandler;

import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.validator.ValidatorService;
import iudx.catalogue.server.authenticator.AuthenticationService;

interface CrudInterface {
}

public final class CrudApis implements CrudInterface {


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
   * Create Item
   *
   * @param context {@link RoutingContext}
   * @TODO Throw error if load failed
   */
  // tag::db-service-calls[]
  public void createItemHandler(RoutingContext routingContext) {

    /* Contains the cat-item */
    JsonObject requestBody = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject authenticationInfo = new JsonObject();

    /**
     * Start insertion flow 
     **/

    /** Json schema validate item */
    validatorService.validateSchema(requestBody, schValHandler -> {
      if (schValHandler.failed()) {
        LOGGER.error("Item validation failed");
        response.setStatusCode(400).end();
        return;
      }
      if (schValHandler.succeeded()) {
        LOGGER.info("Schema validation success");
        String providerId = requestBody.getString(REL_PROVIDER);
        JsonObject authRequest = new JsonObject().put(REL_PROVIDER, providerId);
        authenticationInfo.put(HEADER_TOKEN,
                              request.getHeader(HEADER_TOKEN))
                        .put(OPERATION, POST);

        /** Introspect token and authorize operation */
        authService.tokenInterospect(authRequest, authenticationInfo, authhandler -> {
          if (authhandler.failed()) {
            response.setStatusCode(401)
                    .end(authhandler.cause().toString());
            return;
          }
          else if (authhandler.result()
                  .getString(STATUS)
                  .equals(ERROR)) {
            LOGGER.info("Authentication failed");
            response.setStatusCode(401)
                    .end();
                  }
          else if (authhandler.result()
                  .getString(STATUS)
                  .equals(SUCCESS)) {
            LOGGER.info("Authenticated item creation request "
                .concat(authhandler.result().toString()));

            /* Link Validating the request to ensure item correctness */
            validatorService.validateItem(requestBody, valhandler -> {
              if (valhandler.failed()) {
                LOGGER.error("Item validation failed".concat(valhandler.cause().toString()));
                response.setStatusCode(500)
                        .end(valhandler.cause().toString());
              }
              if (valhandler.succeeded()) {
                LOGGER.info("Item link validation successful");

                /* Requesting database service, creating a item */
                dbService.createItem(valhandler.result(), dbhandler -> {
                  if (dbhandler.failed()) {
                    LOGGER.error("Item creation failed".concat(dbhandler.cause().toString()));
                    response.setStatusCode(500)
                            .end(dbhandler.cause().toString());
                  }
                  if (dbhandler.succeeded()) {
                    LOGGER.info("Item created".concat(dbhandler.result().toString()));
                    response.setStatusCode(201)
                            .end(dbhandler.result().toString());
                  }
                });
              }
            });
          } 
        });
      }
    });
    /** End insertion flow */
  }
}
