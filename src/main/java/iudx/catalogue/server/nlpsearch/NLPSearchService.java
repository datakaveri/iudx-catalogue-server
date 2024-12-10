package iudx.catalogue.server.nlpsearch;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The NLP Search Service.
 * <h1>NLP Search Service</h1>
 *
 * <p>
 * The NLP Search service in the IUDX Catalogue Server defines the operations to be performed
 * with the IUDX NLP Search  server.
 * </p>
 *
 *
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-11-05
 */

@VertxGen
@ProxyGen
public interface NLPSearchService {

  /**
   * Search - NLP Search.
   *
   *
   * @param query Query string
   * @param handler Result handler
   * @return {@link NLPSearchService}
   */
  @Fluent
  NLPSearchService search(String query, Handler<AsyncResult<JsonObject>> handler);

  /**
   * Search - NLP Search.
   *
   *
   * @param doc the document for which an embedding is required
   * @param handler Result handler
   * @return {@link NLPSearchService}
   */
  @Fluent
  NLPSearchService getEmbedding(JsonObject doc, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return GeocodingService
   */
  @GenIgnore
  static NLPSearchService createProxy(Vertx vertx, String address) {
    return new NLPSearchServiceVertxEBProxy(vertx, address);
  }
}
