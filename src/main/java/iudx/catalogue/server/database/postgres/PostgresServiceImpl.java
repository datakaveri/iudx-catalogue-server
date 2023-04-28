package iudx.catalogue.server.database.postgres;


import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import iudx.catalogue.server.database.RespBuilder;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class PostgresServiceImpl implements PostgresService {
  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImpl.class);
  private final PgPool client;

  public PostgresServiceImpl(final PgPool pgclient) {
    this.client = pgclient;
  }

  @Override
  public PostgresService executeQuery(
      final String query, Handler<AsyncResult<JsonObject>> handler) {

    Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());

    client
        .withConnection(
            connection ->
                connection.query(query).collecting(rowCollector).execute().map(row -> row.value()))
        .onSuccess(
            successHandler -> {
              JsonArray result = new JsonArray(successHandler);
              RespBuilder respBuilder =
                  new RespBuilder().withType(TYPE_SUCCESS).withTitle(SUCCESS).withResult(result);

              handler.handle(Future.succeededFuture(respBuilder.getJsonResponse()));
            })
        .onFailure(
            failureHandler -> {
              LOGGER.debug(failureHandler);
              RespBuilder respBuilder =
                  new RespBuilder()
                      .withType(TYPE_DB_ERROR)
                      .withTitle(TITLE_DB_ERROR)
                      .withDetail(failureHandler.getLocalizedMessage());

              handler.handle(Future.failedFuture(respBuilder.getResponse()));
            });
    return this;
  }

  @Override
  public PostgresService executeCountQuery(
      final String query, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug(query);
    client
        .withConnection(
            connection ->
                connection.query(query).execute().map(rows -> rows.iterator().next().getInteger(0)))
        .onSuccess(
            count -> {
              handler.handle(Future.succeededFuture(new JsonObject().put("totalHits", count)));
            })
        .onFailure(
            failureHandler -> {
              LOGGER.debug(failureHandler);
              String response =
                  new RespBuilder()
                      .withType(TYPE_DB_ERROR)
                      .withTitle(TITLE_DB_ERROR)
                      .withDetail(failureHandler.getLocalizedMessage())
                      .getResponse();
              handler.handle(Future.failedFuture(response));
            });
    return this;
  }
}
