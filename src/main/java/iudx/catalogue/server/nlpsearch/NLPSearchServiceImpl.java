package iudx.catalogue.server.nlpsearch;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.CompositeFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.StringBuilder;

import static iudx.catalogue.server.util.Constants.*;

/**
 * The NLP Search Service Implementation.
 *
 * <h1>NLP Search Service Implementation</h1>
 *
 * <p>
 * The NLP Search Service implementation in the IUDX Catalogue Server implements the definitions of
 * the {@link iudx.catalogue.server.nlpsearch.NLPSearchService}.
 *
 * @version 1.0
 * @since 2020-12-21
 */
public class NLPSearchServiceImpl implements NLPSearchService {

  private static final Logger LOGGER = LogManager.getLogger(NLPSearchServiceImpl.class);
  private final WebClient webClient;
  private final String nlpServiceUrl;
  private final int nlpServicePort;

  public NLPSearchServiceImpl(WebClient client, String nlpServiceUrl, int nlpServicePort) {
    webClient = client;
    this.nlpServiceUrl = nlpServiceUrl;
    this.nlpServicePort = nlpServicePort;
}

  @Override
  public NLPSearchService search(String query, Handler<AsyncResult<JsonObject>> handler) {
    webClient
    .get(nlpServicePort, nlpServiceUrl, "/search")
    .timeout(SERVICE_TIMEOUT)
    .addQueryParam("q", query)
    .putHeader("Accept","application/json").send(ar -> {
    if(ar.succeeded()) {
      LOGGER.debug("Success: NLP Search; Request succeeded");
      handler.handle(Future.succeededFuture(ar.result().body().toJsonObject()));
    } else {
      LOGGER.error("Fail: NLP Search failed");
      handler.handle(Future.failedFuture(ar.cause()));
      }
    });
    return this;
  }

  @Override
  public NLPSearchService getEmbedding(JsonObject doc, Handler<AsyncResult<JsonObject>> handler) {
    webClient
    .post(nlpServicePort, nlpServiceUrl, "/indexdoc")
    .timeout(SERVICE_TIMEOUT)
    .sendJsonObject(doc, ar-> {
      if(ar.succeeded()) {
        LOGGER.debug("Info: Document embeddings created");
        handler.handle(Future.succeededFuture(ar.result().body().toJsonObject()));
      } else {
        LOGGER.error("Error: Document embeddings not created");
        handler.handle(Future.failedFuture(ar.cause()));
      }
    });
    return this;
  }
}
