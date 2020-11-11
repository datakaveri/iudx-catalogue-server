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

import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.apiserver.util.QueryMapper;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;


public final class SearchApis {


  private DatabaseService dbService;

  private static final Logger LOGGER = LogManager.getLogger(SearchApis.class);


  /**
   * Crud  constructor
   *
   * @param DBService DataBase Service class
   * @return void
   * @TODO Throw error if load failed
   */
  public SearchApis() {
  }

  public void setDbService(DatabaseService dbService) {
    this.dbService = dbService;
  }

  /**
   * Processes the attribute, geoSpatial, and text search requests and returns the results from the
   * database.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void searchHandler(RoutingContext routingContext) {

    String path =  routingContext.normalisedPath();

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);


    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_INSTANCE);

    MultiMap queryParameters = routingContext.queryParams();

    LOGGER.debug("Info: routed to search/count");
    LOGGER.debug("Info: instance;" + instanceID);

    /* validating proper actual query parameters from request */
    if ((request.getParam(PROPERTY) == null || request.getParam(VALUE) == null)
        && (request.getParam(GEOPROPERTY) == null
            || request.getParam(GEORELATION) == null
            || request.getParam(GEOMETRY) == null
            || request.getParam(COORDINATES) == null)
        && request
            .getParam(Q_VALUE) == null) {

      LOGGER.error("Fail: Invalid Syntax");
      response.setStatusCode(400)
        .end(new ResponseHandler.Builder()
                                .withStatus(INVALID_SYNTAX)
                                .build().toJsonString());
      return;

      /* checking the values of the query parameters */
    } else if (request.getParam(PROPERTY) != null
        && !request.getParam(PROPERTY).isBlank()) {

      /* converting query parameters in json */
      requestBody = QueryMapper.map2Json(queryParameters);

      /* checking the values of the query parameters for geo related count */
    } else if (LOCATION.equals(request.getParam(GEOPROPERTY))
                && GEOMETRIES.contains(request.getParam(GEOMETRY))
        && GEORELS.contains(request.getParam(GEORELATION))) {
      requestBody = QueryMapper.map2Json(queryParameters);
    } else if (request.getParam(Q_VALUE) != null
        && !request.getParam(Q_VALUE).isBlank()) {
      /* checking the values of the query parameters */

      requestBody = QueryMapper.map2Json(queryParameters);

    } else {
          response.setStatusCode(400)
                  .end(new ResponseHandler.Builder()
                                          .withStatus(INVALID_VALUE)
                                          .build().toJsonString());
      return;
    }

    if (requestBody != null) {
      requestBody.put(HEADER_INSTANCE, instanceID);
      
      if (path.equals(ROUTE_SEARCH)) {
        dbService.searchQuery(requestBody, handler -> {
          if (handler.succeeded()) {
            JsonObject resultJson = handler.result();
            String status = resultJson.getString(STATUS);
            if (status.equalsIgnoreCase(SUCCESS)) {
              LOGGER.info("Success: search query");
              response.setStatusCode(200);
            } else if (status.equalsIgnoreCase(PARTIAL_CONTENT)) {
              LOGGER.info("Success: search query");
              response.setStatusCode(206);
            } else {
              LOGGER.error("Fail: search query");
              response.setStatusCode(400);
            }
            response.end(resultJson.toString());
          } else if (handler.failed()) {
            LOGGER.error("Fail: Search;" + handler.cause().getMessage());
                response.setStatusCode(400)
                        .end(handler.cause().getMessage());
          }
        });
      }
      else {
        dbService.countQuery(requestBody, handler -> {
          if (handler.succeeded()) {
            JsonObject resultJson = handler.result();
            String status = resultJson.getString(STATUS);
            if (status.equalsIgnoreCase(SUCCESS)) {
              LOGGER.info("Success: count query");
              response.setStatusCode(200);
            } else if (status.equalsIgnoreCase(PARTIAL_CONTENT)) {
              LOGGER.info("Success: count query");
              response.setStatusCode(206);
            } else {
              LOGGER.error("Fail: count query");
              response.setStatusCode(400);
            }
            response.end(resultJson.toString());
          } else if (handler.failed()) {
            LOGGER.error("Fail: Count;" + handler.cause().getMessage());
            response.setStatusCode(400).end(handler.cause().getMessage());
          }
        });
      }
    } else {
      LOGGER.error("Fail: Search/Count; Invalid request query parameters");
          response.setStatusCode(400)
                  .end(new ResponseHandler.Builder()
                                          .withStatus(INVALID_SYNTAX)
                                          .build().toJsonString());
    }

  }
}
