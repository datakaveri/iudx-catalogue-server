package iudx.catalogue.server.database.listapis;

import static iudx.catalogue.server.util.Constants.TITLE_INTERNAL_SERVER_ERROR;
import static iudx.catalogue.server.util.Constants.TYPE_INTERNAL_SERVER_ERROR;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.database.QueryDecoder;
import iudx.catalogue.server.database.RespBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ListService {
  private static final Logger LOGGER = LogManager.getLogger(ListService.class);
  private final QueryDecoder queryDecoder = new QueryDecoder();
  ElasticClient client;
  String docIndex;

  public ListService(ElasticClient client, String docIndex) {
    this.client = client;
    this.docIndex = docIndex;
  }

  public void listItems(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String elasticQuery = queryDecoder.listItemQuery(request);

    LOGGER.debug("Info: Listing items;" + elasticQuery);

    client.listAggregationAsync(
        elasticQuery,
        clientHandler -> {
          if (clientHandler.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            JsonObject responseJson = clientHandler.result();
            handler.handle(Future.succeededFuture(responseJson));
          } else {
            LOGGER.error("Fail: DB request has failed;" + clientHandler.cause());
            /* Handle request error */
            handler.handle(
                Future.failedFuture(
                    respBuilder
                        .withType(TYPE_INTERNAL_SERVER_ERROR)
                        .withTitle(TITLE_INTERNAL_SERVER_ERROR)
                        .getResponse()));
          }
        });
  }

  public void listOwnerOrCos(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String elasticQuery = queryDecoder.listItemQuery(request);

    LOGGER.debug("Info: Listing items;" + elasticQuery);

    client.searchAsync(
        elasticQuery,
        docIndex,
        clientHandler -> {
          if (clientHandler.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            JsonObject responseJson = clientHandler.result();
            handler.handle(Future.succeededFuture(responseJson));
          } else {
            LOGGER.error("Fail: DB request has failed;" + clientHandler.cause());
            /* Handle request error */
            handler.handle(
                Future.failedFuture(
                    respBuilder
                        .withType(TYPE_INTERNAL_SERVER_ERROR)
                        .withTitle(TITLE_INTERNAL_SERVER_ERROR)
                        .getResponse()));
          }
        });
  }
}
