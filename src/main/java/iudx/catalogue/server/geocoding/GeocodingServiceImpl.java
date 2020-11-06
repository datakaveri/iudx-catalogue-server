package iudx.catalogue.server.geocoding;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.catalogue.server.Constants.*;


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
 * @since 2020-05-31
 */
public class GeocodingServiceImpl implements GeocodingService {

  private static final Logger LOGGER = LogManager.getLogger(GeocodingServiceImpl.class);
  private final WebClient webClient;

  public GeocodingServiceImpl(WebClient client) {
    webClient = client;
}

  @Override
  public void geocoder(String location, Handler<AsyncResult<JsonObject>> handler) {
      // to be implemented
      LOGGER.info(location);
      webClient
      .get(4000,"apiserver","/v1/search")
      .addQueryParam("text", location)
      // .putHeader("Accept","application/json")
      .send(ar -> {
        if(ar.succeeded()) {
          LOGGER.info("Request succeeded!");
          LOGGER.info(ar.result());
          handler.handle(Future.succeededFuture(ar.result().body().toJsonObject()));
        }
        else {
          LOGGER.info("Failed to find coordinates");
          handler.handle(Future.failedFuture(ar.cause()));
        }
      });
    }
}

