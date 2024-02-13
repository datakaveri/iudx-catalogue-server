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

  @Fluent
  MlayerService getMlayerInstance(String instance, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MlayerService deleteMlayerInstance(String request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MlayerService updateMlayerInstance(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MlayerService createMlayerDomain(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MlayerService getMlayerDomain(String id, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MlayerService deleteMlayerDomain(String request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MlayerService updateMlayerDomain(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MlayerService getMlayerProviders(Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MlayerService getMlayerGeoQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MlayerService getMlayerAllDatasets(Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MlayerService getMlayerDataset(JsonObject requestData, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MlayerService getMlayerPopularDatasets(String instance, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MlayerService getTotalCountSizeApi(Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  MlayerService getRealTimeDataSetApi(Handler<AsyncResult<JsonObject>> handler);
}
