package iudx.catalogue.server.rating;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface RatingService {
  @GenIgnore
  static RatingService createProxy(Vertx vertx, String address) {
    return new RatingServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  RatingService createRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  RatingService getRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  RatingService updateRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  RatingService deleteRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);
}
