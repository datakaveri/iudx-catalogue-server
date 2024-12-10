package iudx.catalogue.server.geocoding;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Geocoding Service.
 * <h1>Geocoding Service</h1>
 *
 * <p>
 * The Geocoding Service in the IUDX Catalogue Server defines the operations to be performed
 * with the IUDX Geocoding  server.
 * </p>
 *
 *
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-11-05
 */

@VertxGen
@ProxyGen
public interface GeocodingService {

  /**
   * This method performs geocoding using a third-party service and
   * returns a JSON array of geocoding results for the specified location.
   * @param location which is a String
   * @param handler which is a request handler
   */
  void geocoder(String location, Handler<AsyncResult<String>> handler);

  /**
   * This method performs geocoding using a third-party API and returns the result to the handler.
   * @param lat which is a Float
   * @param lon which is a Float
   * @param handler which is a request handler
   */
  void reverseGeocoder(String lat, String lon, Handler<AsyncResult<JsonObject>> handler);

  /**
   * This method performs a reverse geocoding request to a Pelias geocoding service
   * using latitude and longitude coordinates.
   * @param doc which is a JsonObject
   */
  void geoSummarize(JsonObject doc, Handler<AsyncResult<String>> handler);

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return GeocodingService
   */
  @GenIgnore
  static GeocodingService createProxy(Vertx vertx, String address) {
    return new GeocodingServiceVertxEBProxy(vertx, address);
  }
}
