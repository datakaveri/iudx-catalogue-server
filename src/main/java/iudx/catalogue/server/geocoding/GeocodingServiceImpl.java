package iudx.catalogue.server.geocoding;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.CompositeFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.StringBuilder;

import static iudx.catalogue.server.util.Constants.*;


/**
 * The Geocoding Service Implementation.
 *
 * <h1>Geocoding Service Implementation</h1>
 *
 * <p>
 * The Geocoding Service implementation in the IUDX Catalogue Server implements the definitions of
 * the {@link iudx.catalogue.server.geocoding.GeocodingService}.
 *
 * @version 1.0
 * @since 2020-11-05
 */
public class GeocodingServiceImpl implements GeocodingService {

  private static final Logger LOGGER = LogManager.getLogger(GeocodingServiceImpl.class);
  private final WebClient webClient;
  StringBuilder sb = new StringBuilder(); 

  public GeocodingServiceImpl(WebClient client) {
    webClient = client;
}

  @Override
  public void geocoder(String location, Handler<AsyncResult<String>> handler) {
    // LOGGER.info(location);
    webClient
    .get(4000,"pelias_api","/v1/search")
    .addQueryParam("text", location)
    .putHeader("Accept","application/json").send(ar -> {
      if(ar.succeeded()) {
        LOGGER.info("Request succeeded!");
        LOGGER.info(ar.result().body());
        handler.handle(Future.succeededFuture(ar.result().body().toJsonObject().getJsonArray("bbox").toString()));
      }
      else {
        LOGGER.info("Failed to find coordinates");
        handler.handle(Future.failedFuture(ar.cause()));
      }
    });
  }

  private Promise<String> geocoderhelper(String location) {
    Promise<String> promise = Promise.promise();
    geocoder(location, ar -> {
      if(ar.succeeded()){
        promise.complete(ar.result());
      }
      else {
        LOGGER.info("Request failed!");
      }
     
    });
   return promise;
  }

  @Override
  public void reverseGeocoder(String lat, String lon, Handler<AsyncResult<JsonObject>> handler) {
    webClient
    .get(4000,"pelias_api","/v1/reverse")
    .addQueryParam("point.lon", lon)
    .addQueryParam("point.lat", lat)
    .putHeader("Accept","application/json").send(ar -> {
      if(ar.succeeded()) {
        LOGGER.info("Request succeeded!");
        // LOGGER.info(ar.result().body().toJsonObject().getJsonArray("features").getJsonObject("properties"));
        handler.handle(Future.succeededFuture(ar.result().body().toJsonObject()));
      }
      else {
        LOGGER.info("Failed to find location");
        handler.handle(Future.failedFuture(ar.cause()));
      }
    });
  }

  private Promise<String> reverseGeocoderhelper(String lat, String lon) {
    Promise<String> promise = Promise.promise();
    reverseGeocoder(lat, lon, ar -> {
      if(ar.succeeded()){
        JsonArray res = ar.result().getJsonArray("features");
        JsonObject properties = res.getJsonObject(0).getJsonObject("properties");
        JsonObject addr = new JsonObject();
        addr.put("name", properties.getString("name"));
        addr.put("borough",properties.getString("borough"));
        addr.put("locality", properties.getString("locality"));
        promise.complete(addr.toString());
      }
      else {
        LOGGER.info("Request failed!");
      }
    });
   return promise;
  }

  @Override
  public void geoSummarize(JsonObject doc, Handler<AsyncResult<String>> handler) {
    Promise<String> p1 = Promise.promise();
    Promise<String> p2 = Promise.promise();

    if(doc.containsKey("location")) {

      /* Geocoding information*/
      JsonObject location = doc.getJsonObject("location");
      String address = location.getString("address");
      if(address!=null) {
        p1 = geocoderhelper(address);
      }
      else {
        p1.complete(new String());
      }
      
      /* Reverse Geocoding information */
      if(location.containsKey("geometry")) {
        JsonObject geometry = location.getJsonObject("geometry");
        JsonArray pos = geometry.getJsonArray("coordinates");
        String lon = pos.getString(0);
        String lat = pos.getString(1);
        p2 = reverseGeocoderhelper(lat, lon);
      }
      else {
        p2.complete(new String());
      } 
    }
    CompositeFuture.all(p1.future(),p2.future()).onSuccess(successHandler-> {
      String j1 = successHandler.resultAt(0);
      String j2 = successHandler.resultAt(1);
      JsonObject res = new JsonObject();
      res.put("_geocoded", j1);
      res.put("_reverseGeocoded",j2);
      handler.handle(Future.succeededFuture(res.toString()));
    });
    return;
  }
}

