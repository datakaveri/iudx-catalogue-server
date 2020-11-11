/**
 * <h1>SearchApis.java</h1>
 * Callback handlers for List APIS
 */

package iudx.catalogue.server.apiserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import iudx.catalogue.server.database.DatabaseService;


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
  public void listItemsHandler(RoutingContext routingContext) {

    LOGGER.debug("Info: Listing items");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_INSTANCE);

    String itemType = request.getParam(ITEM_TYPE);
    requestBody.put(ITEM_TYPE, itemType);
    /* Populating query mapper */
    requestBody.put(HEADER_INSTANCE, instanceID);

    String type = null;

    switch (itemType) {
      case INSTANCE:
        type = ITEM_TYPE_INSTANCE;
        break;
      case RESOURCE_GRP:
        type = ITEM_TYPE_RESOURCE_GROUP;
        break;
      case RESOURCE_SVR:
    	  type = ITEM_TYPE_RESOURCE_SERVER;
        break;
      case PROVIDER:
    	  type = ITEM_TYPE_PROVIDER;
        break;
      case TAGS:
        type = itemType;
        break;
      default:
        LOGGER.error("Fail: Invalid itemType:" + itemType);
        response
            .setStatusCode(400)
            .end(new JsonObject().put(STATUS, ERROR).put("message", "Invalid itemType").toString());
        return;
    }
    requestBody.put(TYPE, type);


    /* Request database service with requestBody for listing items */
    dbService.listItems(
        requestBody,
        dbhandler -> {
          if (dbhandler.succeeded()) {
            LOGGER.info("Success: Item listing");
            response
                .setStatusCode(200)
                .end(dbhandler.result().toString());
          } else if (dbhandler.failed()) {
            LOGGER.error(
                "Fail: Issue in listing " + itemType + ": " + dbhandler.cause().getMessage());
            response
                .setStatusCode(400)
                .end(dbhandler.cause().getMessage());
          }
        });
  }
}
