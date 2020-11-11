/**
 * <h1>RelationshipApis.java</h1>
 * Callback handlers for Relationship APIs
 */

package iudx.catalogue.server.apiserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import io.vertx.core.MultiMap;
import iudx.catalogue.server.apiserver.util.ResponseHandler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.apiserver.util.QueryMapper;


public final class RelationshipApis {


  private DatabaseService dbService;

  private static final Logger LOGGER = LogManager.getLogger(RelationshipApis.class);


  /**
   * Crud  constructor
   *
   * @param DBService DataBase Service class
   * @return void
   * @TODO Throw error if load failed
   */
  public RelationshipApis() {
  }

  public void setDbService(DatabaseService dbService) {
    this.dbService = dbService;
  }

  /**
   * Get all items belonging to the itemType.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void listRelationshipHandler(RoutingContext routingContext) {

    LOGGER.info("Info: Searching for relationship of resource");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject requestBody = new JsonObject();
    String instanceID = request.getHeader(HEADER_INSTANCE);

    if (request.getParam(ID) != null && request.getParam(REL_KEY) != null) {
      String id = request.getParam(ID);
      String itemType = request.getParam(REL_KEY);

      if (!id.isBlank() && ITEMS_KEY.contains(itemType)) {
        requestBody.put(ID, id);
        requestBody.put(INSTANCE, instanceID);
        requestBody.put(RELATIONSHIP, itemType);

        /*
         * Request database service with requestBody for listing resource relationship
         */
        dbService.listRelationship(requestBody, dbhandler -> {
          if (dbhandler.succeeded()) {
            LOGGER.info("Success: Retrieved relationships of " + itemType);
            response.setStatusCode(200)
                    .end(dbhandler.result().toString());
          } else if (dbhandler.failed()) {
            LOGGER.error("Fail: Issue in listing relationship;" + dbhandler.cause().getMessage());
            response.setStatusCode(400)
                    .end(dbhandler.cause().getMessage());
          }
        });
      } else {
        LOGGER.error("Fail: Issue in query parameter");
        response.setStatusCode(400)
            .end(new ResponseHandler.Builder()
                                    .withStatus(INVALID_VALUE)
                                    .build().toJsonString());
      }
    } else {
      LOGGER.error("Fail: Issue in query parameter");
      response.setStatusCode(400)
          .end(new ResponseHandler.Builder()
                                  .withStatus(INVALID_SYNTAX)
                                  .build().toJsonString());
    }
  }


  /**
   * Relationship search of the cataloque items.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void relSearchHandler(RoutingContext routingContext) {

    LOGGER.debug("Info: Relationship search");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = new JsonObject();

    String instanceID = request.getHeader(HEADER_INSTANCE);

    MultiMap queryParameters = routingContext.queryParams();

    /* validating proper actual query parameters from request */
    if (request.getParam(RELATIONSHIP) != null && request.getParam(VALUE) != null) {

      /* converting query parameters in json */
      requestBody = QueryMapper.map2Json(queryParameters);

      if (requestBody != null) {

        requestBody.put(INSTANCE, instanceID);

        /* Request database service with requestBody for listing domains */
        dbService.relSearch(requestBody, dbhandler -> {
          if (dbhandler.succeeded()) {
            LOGGER.info("Info: Relationship search completed");
            response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                    .setStatusCode(200)
                    .end(dbhandler.result().toString());
          } else if (dbhandler.failed()) {
            LOGGER.error("Fail: Issue in relationship search "
                + dbhandler.cause().getMessage());
            response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                    .setStatusCode(400)
                .end(dbhandler.cause().getMessage());
          }
        });
      } else {
        LOGGER.error("Fail: Invalid request query parameters");
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .setStatusCode(400)
                .end(new ResponseHandler.Builder()
                                        .withStatus(INVALID_VALUE)
                                        .build().toJsonString());
      }
    } else {
      LOGGER.error("Fail: Invalid request query parameters");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .setStatusCode(400)
              .end(new ResponseHandler.Builder()
                                      .withStatus(INVALID_SYNTAX)
                                      .build().toJsonString());
    }
  }
}
