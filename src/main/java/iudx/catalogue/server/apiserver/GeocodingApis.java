/**
 *
 *
 * <h1>GeocodingApis.java</h1>
 *
 *<p>Callback handlers for Geocoding APIs</p>
 */

package iudx.catalogue.server.apiserver;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.geocoding.GeocodingService;
import iudx.catalogue.server.util.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;


public final class GeocodingApis {

  private GeocodingService geoService;
  private static final Logger LOGGER = LogManager.getLogger(GeocodingApis.class);
  private Api api;

  public GeocodingApis(Api api) {
    this.api = api;
  }

  public void setGeoService(GeocodingService geoService) {
    this.geoService = geoService;
  }

  /**
   * Get bounding box for location.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void getCoordinates(RoutingContext routingContext) {
    String location = null;
    try {
      if (routingContext.queryParams().contains("q")) {
        location = routingContext.queryParams().get("q");
      }
      if (location.length() == 0) {
        LOGGER.error("NO location found");
        routingContext
            .response()
            .putHeader("content-type", "application/json")
            .setStatusCode(400)
            .end(
                new RespBuilder()
                    .withType(TYPE_ITEM_NOT_FOUND)
                    .withTitle(LOCATION_NOT_FOUND)
                    .withDetail(FAILED)
                    .getResponse());
        return;
      }
    } catch (Exception e) {
      LOGGER.error("No query parameter");
      routingContext
          .response()
          .putHeader("content-type", "application/json")
          .setStatusCode(400)
          .end(
              new RespBuilder()
                  .withType(TYPE_INVALID_QUERY_PARAM_VALUE)
                  .withTitle(INVALID_VALUE)
                  .withDetail(FAILED)
                  .getResponse());
      return;
    }
    geoService.geocoder(
        location,
        reply -> {
          if (reply.succeeded()) {
            JsonObject  result = new JsonObject(reply.result());
            routingContext
                .response()
                .putHeader("content-type", "application/json")
                .setStatusCode(200)
                .end(
                    new RespBuilder()
                        .withType(TYPE_SUCCESS)
                        .withTitle(TITLE_SUCCESS)
                        .totalHits(result.getJsonArray(RESULTS))
                        .withResult(result.getJsonArray(RESULTS))
                        .getJsonResponse().toString());
          } else {
            LOGGER.error("Failed to find coordinates");
            routingContext
                .response()
                .putHeader("content-type", "application/json")
                .setStatusCode(400)
                .end(
                    new RespBuilder()
                        .withType(TYPE_INVALID_GEO_VALUE)
                        .withTitle(TITLE_INVALID_GEO_VALUE)
                        .withDetail(FAILED)
                        .getResponse());
          }
        });
  }

  /**
   * Get location for coordinates.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void getLocation(RoutingContext routingContext) {

    JsonArray coordinates = new JsonArray();
    String geometry = "Point";

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    if (request.getParam(COORDINATES) == null || request.getParam(GEOMETRY) == null) {
      LOGGER.error("Fail: Invalid Syntax");
      response
          .setStatusCode(400)
          .end(
              new RespBuilder()
                  .withType(TYPE_INVALID_SYNTAX)
                  .withTitle(TITLE_INVALID_SYNTAX)
                  .getResponse());
      return;
    }

    try {
      coordinates = new JsonArray(request.getParam(COORDINATES));
      geometry = request.getParam(GEOMETRY);
      if (geometry != POINT) {
        // go to catch
        throw new Exception();
      }
    } catch (Exception e) {
      LOGGER.error("Failed to find location");
      routingContext
          .response()
          .putHeader("content-type", "application/json")
          .setStatusCode(400)
          .end();
    }

    geoService.reverseGeocoder(
        coordinates.getString(1),
        coordinates.getString(0),
        reply -> {
          if (reply.succeeded()) {
            routingContext
                .response()
                .putHeader("content-type", "application/json")
                .setStatusCode(200)
                .end(reply.result().encode());
          } else {
            LOGGER.error("Failed to find location");
            routingContext
                .response()
                .putHeader("content-type", "application/json")
                .setStatusCode(400)
                .end();
          }
        });
  }
}
