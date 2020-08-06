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
   * Get all resources belonging to a resourceGroup.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void listResourceRelationship(RoutingContext routingContext) {

    LOGGER.info("Info: Searching for relationship of resource");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    String instanceID = request.getHeader(HEADER_HOST);

    String id = request.getParam(ID);

    if (id != null && !id.isBlank()) {
      requestBody.put(ID, id);
      requestBody.put(INSTANCE_ID_KEY, instanceID);
      requestBody.put(RELATIONSHIP, REL_RESOURCE);

      /*
       * Request database service with requestBody for listing resource relationship
       */
      dbService.listResourceRelationship(requestBody, dbhandler -> {
        if (dbhandler.succeeded()) {
          LOGGER.info("Success: List of resources belonging to resourceGroups");
          response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                  .setStatusCode(200)
                  .end(dbhandler.result().toString());
        } else if (dbhandler.failed()) {
          LOGGER.error("Fail: Issue in listing resource relationship;"
                          .concat(dbhandler.cause().toString()));
          response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                  .setStatusCode(400)
                  .end(dbhandler.cause().toString());
        }
      });
    } else {
      LOGGER.error("Fail: Issue in path parameter");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .setStatusCode(400)
              .end(new ResponseHandler.Builder()
                                      .withStatus(INVALID_SYNTAX)
                                      .build().toJsonString());
    }
  }

  /**
   * Get all resourceGroup relationships.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void listResourceGroupRelationship(RoutingContext routingContext) {

    LOGGER.info("Info: Searching for relationship of resource and resourceGroup");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    String instanceID = request.getHeader(HEADER_HOST);
    String id = request.getParam(ID);

    if (id != null && !id.isBlank()) {

      requestBody.put(ID, id);
      requestBody.put(INSTANCE_ID_KEY, instanceID);
      requestBody.put(RELATIONSHIP, REL_RESOURCE_GRP);

      /*
       * Request database service with requestBody for listing resource group relationship
       */
      dbService.listResourceGroupRelationship(requestBody, dbhandler -> {
        if (dbhandler.succeeded()) {
          LOGGER.info("Success: List of resourceGroup belonging to resource");
          response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                  .setStatusCode(200)
                  .end(dbhandler.result().toString());
        } else if (dbhandler.failed()) {
          LOGGER.error("Fail: Issue in listing resourceGroup relationship;"
                        .concat(dbhandler.cause().toString()));
          response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                  .setStatusCode(400)
                  .end(dbhandler.cause().getLocalizedMessage());
        }
      });
    } else {
      LOGGER.error("Fail: Issue in path parameter");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .setStatusCode(400)
              .end(new ResponseHandler.Builder().withStatus(INVALID_SYNTAX)
              .build().toJsonString());
    }
  }


  /**
   * Queries the database and returns all resource servers belonging to an item.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void listResourceServerRelationship(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = new JsonObject();
    String instanceID = routingContext.request().host();
    String id = routingContext.request().getParam(ID);
    queryJson.put(INSTANCE_ID_KEY, instanceID).put(ID, id)
        .put(RELATIONSHIP, REL_RESOURCE_SVR);
    LOGGER.debug("Info: search query : " + queryJson);
    dbService.listResourceServerRelationship(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(STATUS);
        if (status.equalsIgnoreCase(SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        LOGGER.info("Success: Retrieved relationships");
        response.headers()
                .add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .add(HEADER_CONTENT_LENGTH,
                      String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        response.end();
      } else if (handler.failed()) {
        LOGGER.error(handler.cause().getMessage());
        response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
        response.setStatusCode(400);
        response.end(handler.cause().getLocalizedMessage());
      }
    });
  }

  /**
   * Queries the database and returns provider of an item.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void listProviderRelationship(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = new JsonObject();
    String instanceID = routingContext.request().host();
    String id = routingContext.request().getParam(ID);
    queryJson
        .put(INSTANCE_ID_KEY, instanceID)
        .put(ID, id)
        .put(RELATIONSHIP, REL_PROVIDER);
    LOGGER.debug("Info: search query : " + queryJson);
    dbService.listProviderRelationship(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(STATUS);
        if (status.equalsIgnoreCase(SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        LOGGER.info("Success: Retrieved relationships");
        response.headers()
                .add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .add(HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        response.end();
      } else if (handler.failed()) {
        LOGGER.error(handler.cause().getMessage());
        response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
        response.setStatusCode(400);
        response.end(handler.cause().getLocalizedMessage());
      }
    });
  }

  /**
   * Queries the database and returns data model of an item.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void listTypes(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = new JsonObject();
    String instanceID = routingContext.request().host();
    String id = routingContext.request().getParam(ID);
    queryJson
        .put(INSTANCE_ID_KEY, instanceID)
        .put(ID, id)
        .put(RELATIONSHIP, REL_TYPE);
    LOGGER.info("Info: search query : " + queryJson);
    dbService.listTypes(
        queryJson,
        handler -> {
          if (handler.succeeded()) {
            JsonObject resultJson = handler.result();
            String status = resultJson.getString(STATUS);
            if (status.equalsIgnoreCase(SUCCESS)) {
              response.setStatusCode(200);
            } else {
              response.setStatusCode(400);
            }
            response
                .headers()
                .add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .add(
                    HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            LOGGER.info("Success: Retrieved relationships");
            response.end();
          } else if (handler.failed()) {
            LOGGER.error(handler.cause().getMessage());
            response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
            response.setStatusCode(400);
            response.end(handler.cause().getLocalizedMessage());
          }
        });
  }

  /**
   * Relationship search of the cataloque items.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void relSearch(RoutingContext routingContext) {

    LOGGER.debug("Info: Relationship search");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = new JsonObject();

    String instanceID = request.getHeader(HEADER_HOST);

    MultiMap queryParameters = routingContext.queryParams();

    /* validating proper actual query parameters from request */
    if (request.getParam(RELATIONSHIP) != null && request.getParam(VALUE) != null) {

      /* converting query parameters in json */
      requestBody = QueryMapper.map2Json(queryParameters);

      if (requestBody != null) {

        requestBody.put(INSTANCE_ID_KEY, instanceID);

        /* Request database service with requestBody for listing domains */
        dbService.relSearch(requestBody, dbhandler -> {
          if (dbhandler.succeeded()) {
            LOGGER.info("Info: Relationship search completed, response: "
                          .concat(dbhandler.result().toString()));
            response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                    .setStatusCode(200)
                    .end(dbhandler.result().toString());
          } else if (dbhandler.failed()) {
            LOGGER.error("Fail: Issue in relationship search "
                          .concat(dbhandler.cause().toString()));
            response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                    .setStatusCode(400)
                    .end(dbhandler.cause().getLocalizedMessage());
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
