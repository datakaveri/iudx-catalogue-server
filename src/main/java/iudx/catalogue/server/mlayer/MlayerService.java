package iudx.catalogue.server.mlayer;

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
public interface MlayerService {
  @GenIgnore
  static MlayerService createProxy(Vertx vertx, String address) {
    return new MlayerServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  MlayerService createMlayerInstance(JsonObject request, Handler<AsyncResult<JsonObject>> handler);
}
