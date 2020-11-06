/**
 * <h1>GeocodingApis.java</h1>
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

import javax.naming.Context;

import static iudx.catalogue.server.Constants.*;

import iudx.catalogue.server.geocoding.GeocodingService;
import iudx.catalogue.server.apiserver.util.QueryMapper;


public final class GeocodingApis {


  private GeocodingService geoService;

  private static final Logger LOGGER = LogManager.getLogger(GeocodingApis.class);


  /**
   * Crud  constructor
   *
   * @param DBService DataBase Service class
   * @return void
   * @TODO Throw error if load failed
   */
  public GeocodingApis() {
  }

  public void setGeoService(GeocodingService geoService) {
    this.geoService = geoService;
  }

  /**
   * Get all items belonging to the itemType.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void getCoordinates(RoutingContext routingContext) {
    String location = "";
    LOGGER.info("heeloo");
    try {
      if(routingContext.queryParams().contains("q")) {
        location = routingContext.queryParams().get("q");
      }
      if(location.length() == 0) {
        routingContext.response().setStatusCode(404).end();
        return;
      }
    }
    catch (Exception e) {
      LOGGER.info("No query parameter");
      routingContext.response().setStatusCode(404).end();
      return;
    }
    geoService.geocoder(location, reply -> {
      if(reply.succeeded()) {
        routingContext.response().putHeader("content-type","application/json")
        .setStatusCode(200)
        .end(reply.result().encode());
      }
      else {
        LOGGER.info("Failed to find coordinates");
        routingContext.response()
        .putHeader("content-type", "application/json")
        .setStatusCode(404)
        .end();
      }
    });
    }
  }