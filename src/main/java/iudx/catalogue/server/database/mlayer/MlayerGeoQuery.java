package iudx.catalogue.server.database.mlayer;

import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.DETAIL_INTERNAL_SERVER_ERROR;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerGeoQuery {
  private static final Logger LOGGER = LogManager.getLogger(MlayerGeoQuery.class);
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  ElasticClient client;
  String docIndex;

  public MlayerGeoQuery(ElasticClient client, String docIndex) {
    this.client = client;
    this.docIndex = docIndex;
  }

  public void getMlayerGeoQuery(String query, Handler<AsyncResult<JsonObject>> handler) {
    client.searchAsyncGeoQuery(
        query,
        docIndex,
        resultHandler -> {
          if (resultHandler.succeeded()) {

            LOGGER.debug("Success: Successful DB Request");
            handler.handle(Future.succeededFuture(resultHandler.result()));

          } else {

            LOGGER.error("Fail: failed DB request");
            handler.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }
}
