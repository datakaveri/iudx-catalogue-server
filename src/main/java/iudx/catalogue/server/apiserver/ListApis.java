/**
 * <h1>SearchApis.java</h1>
 * Callback handlers for CRUD
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


public final class ListApis {


  private DatabaseService dbService;

  private static final Logger LOGGER = LogManager.getLogger(ListApis.class);


  /**
   * Crud  constructor
   *
   * @param DBService DataBase Service class
   * @return void
   * @TODO Throw error if load failed
   */
  public ListApis() {
  }

  public void setDbService(DatabaseService dbService) {
    this.dbService = dbService;
  }

  /**
   * Get the list of items for a catalogue instance.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void listItems(RoutingContext routingContext) {

    LOGGER.debug("Info: Listing items");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    String itemType = request.getParam(ITEM_TYPE);
    requestBody.put(ITEM_TYPE, itemType);
    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    String type = null;

    switch (itemType) {
      case "resourceGroup":
        type = ITEM_TYPE_RESOURCE_GROUP;
        break;
      case "resourceServer":
    	  type = ITEM_TYPE_RESOURCE_SERVER;
        break;
      case "provider":
    	  type = ITEM_TYPE_PROVIDER;
        break;
      case "instances":
      case "tags":
        type = itemType;
        break;
      default:
        LOGGER.info("invalid itemType:" + itemType);
        response
            .setStatusCode(400)
            .end("Invalid itemType");
        return;
    }
    requestBody.put("type", type);


    /* Request database service with requestBody for listing items */
    dbService.listItems(
        requestBody,
        dbhandler -> {
          if (dbhandler.succeeded()) {
            LOGGER.info("Success: Successfull DB request");
            response
                .setStatusCode(200)
                .end(dbhandler.result().toString());
          } else if (dbhandler.failed()) {
            LOGGER.error(
                "Fail: Issue in listing " + itemType + ": ".concat(dbhandler.cause().toString()));
            response
                .setStatusCode(400)
                .end(dbhandler.cause().toString());
          }
        });
  }

  /**
   * Queries the database and returns data model of an item.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void listTypes(RoutingContext routingContext) {
    LOGGER.debug("Listing type of item");

    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject queryJson = new JsonObject();
    String instanceID = routingContext.request().host();
    String id = routingContext.request().getParam(ID);
    queryJson
        .put(INSTANCE_ID_KEY, instanceID)
        .put(ID, id)
        .put(RELATIONSHIP, REL_TYPE);
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
            response.write(resultJson.toString());
            LOGGER.info("Success: Listed type;" + resultJson.toString() );
            response.end();
          } else if (handler.failed()) {
            LOGGER.error(handler.cause().getMessage());
            response.setStatusCode(500);
            response.end(INTERNAL_SERVER_ERROR);
          }
        });
  }
}
