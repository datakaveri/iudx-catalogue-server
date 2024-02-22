package iudx.catalogue.server.database.rating;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AverageRating {
  private static final Logger LOGGER = LogManager.getLogger(AverageRating.class);
  private static final String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  ElasticClient client;
  String ratingIndex;
  String docIndex;

  public AverageRating(ElasticClient client, String ratingIndex, String docIndex) {
    this.client = client;
    this.ratingIndex = ratingIndex;
    this.docIndex = docIndex;
  }

  public void getAverageRating(String id, Handler<AsyncResult<JsonObject>> handler) {
    Future<List<String>> getAssociatedIdFuture = getAssociatedIDs(id);
    getAssociatedIdFuture.onComplete(
        ids -> {
          StringBuilder avgQuery = new StringBuilder(GET_AVG_RATING_PREFIX);
          if (ids.succeeded()) {
            ids.result().stream()
                .forEach(
                    v -> {
                      avgQuery.append(GET_AVG_RATING_MATCH_QUERY.replace("$1", v));
                    });
            avgQuery.deleteCharAt(avgQuery.lastIndexOf(","));
            avgQuery.append(GET_AVG_RATING_SUFFIX);
            LOGGER.debug(avgQuery);
            client.ratingAggregationAsync(
                avgQuery.toString(),
                ratingIndex,
                getRes -> {
                  if (getRes.succeeded()) {
                    LOGGER.debug("Success: Successful DB request");
                    JsonObject result = getRes.result();
                    handler.handle(Future.succeededFuture(result));
                  } else {
                    LOGGER.error("Fail: failed getting average rating: " + getRes.cause());
                    handler.handle(Future.failedFuture(internalErrorResp));
                  }
                });
          } else {
            handler.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  private Future<List<String>> getAssociatedIDs(String id) {
    Promise<List<String>> promise = Promise.promise();

    StringBuilder query =
        new StringBuilder(GET_ASSOCIATED_ID_QUERY.replace("$1", id).replace("$2", id));
    LOGGER.debug(query);
    client.searchAsync(
        query.toString(),
        docIndex,
        res -> {
          if (res.succeeded()) {
            List<String> idCollector =
                res.result().getJsonArray(RESULTS).stream()
                    .map(JsonObject.class::cast)
                    .map(d -> d.getString(ID))
                    .collect(Collectors.toList());
            promise.complete(idCollector);
          } else {
            LOGGER.error("Fail: Get average rating failed");
            promise.fail("Fail: Get average rating failed");
          }
        });
    return promise.future();
  }
}
