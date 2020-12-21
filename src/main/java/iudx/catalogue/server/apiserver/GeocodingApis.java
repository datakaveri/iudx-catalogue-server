/**
 * <h1>GeocodingApis.java</h1>
 * Callback handlers for Geocoding APIs
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

import static iudx.catalogue.server.util.Constants.*;

import iudx.catalogue.server.geocoding.GeocodingService;
import iudx.catalogue.server.apiserver.util.QueryMapper;


public final class GeocodingApis {


  private GeocodingService geoService;

  private static final Logger LOGGER = LogManager.getLogger(GeocodingApis.class);

  public GeocodingApis() {
  }

  public void setGeoService(GeocodingService geoService) {
    this.geoService = geoService;
  }

  /**
   * Get bounding box for location
   * @param routingContext handles web requests in Vert.x Web
   */
  public void getCoordinates(RoutingContext routingContext) {
    String location = "";
    try {
      if(routingContext.queryParams().contains("q")) {
        location = routingContext.queryParams().get("q");
      }
      if(location.length() == 0) {
        routingContext.response().setStatusCode(400).end();
        return;
      }
    }
    catch (Exception e) {
      LOGGER.error("No query parameter");
      routingContext.response().setStatusCode(400).end();
      return;
    }
    geoService.geocoder(location, reply -> {
      if(reply.succeeded()) {
        routingContext.response().putHeader("content-type","application/json")
        .setStatusCode(200)
        .end(reply.result());
      }
      else {
        LOGGER.error("Failed to find coordinates");
        routingContext.response()
        .putHeader("content-type", "application/json")
        .setStatusCode(400)
        .end();
      }
    });
  }

  /**
   * Get location for coordinates
   * @param routingContext handles web requests in Vert.x Web
   */
  public void getLocation(RoutingContext routingContext) {
    String lat = "";
    String lon = "";
    
    try {
      if(routingContext.queryParams().contains("lat")) {
        lat = routingContext.queryParams().get("lat");
      }
      if(routingContext.queryParams().contains("lon")) {
        lon = routingContext.queryParams().get("lon");
      }
    }
    catch (Exception e) {
      LOGGER.error("No query parameter");
      routingContext.response().setStatusCode(400).end();
      return;
    }
    geoService.reverseGeocoder(lat, lon, reply -> {
      if(reply.succeeded()) {
        routingContext.response().putHeader("content-type","application/json")
        .setStatusCode(200)
        .end(reply.result().encode());
      }
      else {
        LOGGER.error("Failed to find location");
        routingContext.response()
        .putHeader("content-type", "application/json")
        .setStatusCode(400)
        .end();
      }
    });
    }
  }
