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
  public void geocoder(String location, Handler<AsyncResult<JsonObject>> handler) {
    // LOGGER.info(location);
    webClient
    .get(4000,"127.0.0.1","/v1/search")
    .addQueryParam("text", location)
    .putHeader("Accept","application/json").send(ar -> {
      if(ar.succeeded()) {
        LOGGER.info("Request succeeded!");
        //getJsonArray("bbox")
        handler.handle(Future.succeededFuture(ar.result().body().toJsonObject()));
      }
      else {
        LOGGER.info("Failed to find coordinates");
        handler.handle(Future.failedFuture(ar.cause()));
      }
    });
  }

  private Promise<JsonObject> Geocoderhelper(String location) {
    Promise<JsonObject> promise = Promise.promise();
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
    .get(4000,"127.0.0.1","/v1/reverse")
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

  private Promise<JsonObject> reverseGeocoderhelper(String lat, String lon) {
    Promise<JsonObject> promise = Promise.promise();
    reverseGeocoder(lat, lon, ar -> {
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
  public void geoSummarize(JsonObject doc, Handler<AsyncResult<JsonObject>> handler) {
    Promise<JsonObject> p1 = Promise.promise();
    Promise<JsonObject> p2 = Promise.promise();

    if(doc.containsKey("location")) {

      /* Geocoding information*/
      JsonObject location = doc.getJsonObject("location");
      String address = location.getString("address");
      if(address!=null) {
        p1 = Geocoderhelper(address);
      }
      else {
        p1.complete(new JsonObject());
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
        p2.complete(new JsonObject());
      } 
    }
    CompositeFuture.all(p1.future(),p2.future()).onSuccess(successHandler-> {
      JsonObject j1 = successHandler.resultAt(0);
      JsonObject j2 = successHandler.resultAt(1);
      JsonObject res = new JsonObject();
      res.put("geocoding", j1);
      res.put("reverseGeocoding",j2);
      LOGGER.info(res);
      handler.handle(Future.succeededFuture(res));
    //   }).onFailure(failedHandler -> {
    //      JsonObject result = new JsonObject();
    //      result.put("status", "error");
    //      result.put("message", failedHandler.getMessage());
    //      handler.handle(Future.failedFuture(result));
    });
  }
}

