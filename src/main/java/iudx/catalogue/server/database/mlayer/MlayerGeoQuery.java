package iudx.catalogue.server.database.mlayer;

import static iudx.catalogue.server.database.elastic.query.Queries.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.DETAIL_INTERNAL_SERVER_ERROR;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

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

  public void getMlayerGeoQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String instance = request.getString(INSTANCE);
    JsonArray id = request.getJsonArray("id");
    List<Query> boolGeoQueries = new ArrayList<>();
    for (int i = 0; i < id.size(); i++) {
      String datasetId = id.getString(i);
      boolGeoQueries.add(buildMlayerBoolGeoQuery(instance, datasetId));
    }
    Query query = buildMlayerGeoQuery(boolGeoQueries);
    List<String> includes = List.of("id", "location", "instance", "label");
    SourceConfig sourceConfig = buildSourceConfig(includes);
    client.searchAsyncGeoQuery(
        query,
        sourceConfig,
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
