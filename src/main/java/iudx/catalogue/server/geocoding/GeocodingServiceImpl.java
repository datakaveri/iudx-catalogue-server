package iudx.catalogue.server.geocoding;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.CompositeFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.StringBuilder;

import static iudx.catalogue.server.geocoding.util.Constants.*;
import static iudx.catalogue.server.util.Constants.TITLE_INVALID_SYNTAX;
import static iudx.catalogue.server.util.Constants.TYPE_INVALID_SYNTAX;
// import static iudx.catalogue.server.util.Constants.*;

/**
 * The Geocoding Service Implementation.
 *
 * <h1>Geocoding Service Implementation</h1>
 *
 * <p>The Geocoding Service implementation in the IUDX Catalogue Server implements the definitions
 * of the {@link iudx.catalogue.server.geocoding.GeocodingService}.
 *
 * @version 1.0
 * @since 2020-11-05
 */
public class GeocodingServiceImpl implements GeocodingService {

  private static final Logger LOGGER = LogManager.getLogger(GeocodingServiceImpl.class);
  static WebClient webClient;
  private final String peliasUrl;
  private final int peliasPort;
  StringBuilder sb = new StringBuilder();

  public GeocodingServiceImpl(WebClient webClient, String peliasUrl, int peliasPort) {
    this.webClient = webClient;
    this.peliasUrl = peliasUrl;
    this.peliasPort = peliasPort;
  }

  @Override
  public void geocoder(String location, Handler<AsyncResult<String>> handler) {
    webClient
        .get(peliasPort, peliasUrl, "/v1/search")
        .timeout(SERVICE_TIMEOUT)
        .addQueryParam("text", location)
        .putHeader("Accept", "application/json")
        .send(
            ar -> {
              if (ar.succeeded()
                  && ar.result().body().toJsonObject().containsKey(FEATURES)
                  && !ar.result().body().toJsonObject().getJsonArray(FEATURES).isEmpty()) {
                JsonArray features = ar.result().body().toJsonObject().getJsonArray(FEATURES);
                JsonObject property, feature, resultEntry;
                double confidence = 0;
                JsonArray resultArray = new JsonArray();
                for (int i = 0; i < features.size(); i++) {
                  feature = features.getJsonObject(i);
                  property = feature.getJsonObject(PROPERTIES);
                  resultEntry = generateGeocodingJson(property);
                  if ((confidence < property.getDouble(CONFIDENCE) && resultArray.isEmpty())
                      || (confidence == property.getDouble(CONFIDENCE))) {
                    confidence = property.getDouble(CONFIDENCE);
                  } else if (confidence < property.getDouble(CONFIDENCE)
                      && !resultArray.isEmpty()) {
                    confidence = property.getDouble(CONFIDENCE);
                    resultArray = new JsonArray();
                    resultArray.add(resultEntry);
                  }

                  if (feature.getJsonArray(BBOX) == null) {
                    continue;
                  } else {
                    resultEntry.put(BBOX, feature.getJsonArray(BBOX));
                    resultArray.add(resultEntry);
                  }
                }
                LOGGER.debug("Request succeeded!");
                JsonObject result = new JsonObject().put(RESULTS, resultArray);
                handler.handle(Future.succeededFuture(result.toString()));

              } else {
                LOGGER.error("Failed to find coordinates");
                handler.handle(
                    Future.failedFuture(
                        new JsonObject()
                            .put("type", TYPE_INVALID_SYNTAX)
                            .put("title", TITLE_INVALID_SYNTAX)
                            .put("detail", "Failed to find coordinates")
                            .toString()));
              }
            });
  }

  private JsonObject generateGeocodingJson(JsonObject property) {
    JsonObject resultEntry = new JsonObject();
    if (property.containsKey(NAME)) resultEntry.put(NAME, property.getString(NAME));
    if (property.containsKey(COUNTRY)) resultEntry.put(COUNTRY, property.getString(COUNTRY));
    if (property.containsKey(REGION)) resultEntry.put(REGION, property.getString(REGION));
    if (property.containsKey(COUNTY)) resultEntry.put(COUNTY, property.getString(COUNTY));
    if (property.containsKey(LOCALITY)) resultEntry.put(LOCALITY, property.getString(LOCALITY));
    if (property.containsKey(BOROUGH)) resultEntry.put(BOROUGH, property.getString(BOROUGH));

    return resultEntry;
  }

  private Promise<JsonObject> geocoderHelper(String location) {
    Promise<JsonObject> promise = Promise.promise();
    geocoder(
        location,
        ar -> {
          if (ar.succeeded()) {
            LOGGER.debug(ar.result());
            JsonObject arResToJson = new JsonObject(ar.result());
            promise.complete(arResToJson);
          } else {
            LOGGER.error("Request failed!");
            promise.complete(new JsonObject());
          }
        });
    return promise;
  }

  @Override
  public void reverseGeocoder(String lat, String lon, Handler<AsyncResult<JsonObject>> handler) {
    webClient
        .get(peliasPort, peliasUrl, "/v1/reverse")
        .timeout(SERVICE_TIMEOUT)
        .addQueryParam("point.lon", lon)
        .addQueryParam("point.lat", lat)
        .putHeader("Accept", "application/json")
        .send(
            ar -> {
              if (ar.succeeded()) {
                LOGGER.debug("Request succeeded!");
                handler.handle(Future.succeededFuture(ar.result().body().toJsonObject()));
              } else {
                LOGGER.error("Failed to find location");
                handler.handle(Future.failedFuture(ar.cause()));
              }
            });
  }

  private Promise<JsonObject> reverseGeocoderHelper(String lat, String lon) {
    Promise<JsonObject> promise = Promise.promise();
    reverseGeocoder(
        lat,
        lon,
        ar -> {
          if (ar.succeeded()) {
            JsonArray res = ar.result().getJsonArray(FEATURES);
            JsonObject properties = res.getJsonObject(0).getJsonObject(PROPERTIES);
            JsonObject addr = generateGeocodingJson(properties);
            promise.complete(addr);
          } else {
            LOGGER.error("Request failed!");
            promise.complete(new JsonObject());
          }
        });
    return promise;
  }

  @Override
  public void geoSummarize(JsonObject doc, Handler<AsyncResult<String>> handler) {
    Promise<JsonObject> p1 = Promise.promise();
    Promise<JsonObject> p2 = Promise.promise();

    if (doc.containsKey(LOCATION)) {

      /* Geocoding information*/
      JsonObject location = doc.getJsonObject(LOCATION);
      String address = location.getString(ADDRESS);
      if (address != null) {
        p1 = geocoderHelper(address);
      } else {
        p1.complete(new JsonObject());
      }

      /* Reverse Geocoding information */
      if (location.containsKey(GEOMETRY)
          && location.getJsonObject(GEOMETRY).getString(TYPE).equalsIgnoreCase("Point")) {
        JsonObject geometry = location.getJsonObject(GEOMETRY);
        JsonArray pos = geometry.getJsonArray(COORDINATES);
        String lon = pos.getString(0);
        String lat = pos.getString(1);
        p2 = reverseGeocoderHelper(lat, lon);
      } else {
        p2.complete(new JsonObject());
      }
    }
    CompositeFuture.all(p1.future(), p2.future())
        .onSuccess(
            successHandler -> {
              JsonObject j1 = successHandler.resultAt(0);
              JsonObject j2 = successHandler.resultAt(1);
              JsonObject res = new JsonObject().put(GEOCODED, j1).put(REVERSE_GEOCODED, j2);
              handler.handle(Future.succeededFuture(res.toString()));
            });
  }
}
