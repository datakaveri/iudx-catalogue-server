package iudx.catalogue.server.postgres;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.postgres.PostgresServiceVertxEBProxy;

@VertxGen
@ProxyGen
public interface PostgresService {
  @Fluent
  PostgresService executeQuery(final String query, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The executeCountQuery implements a count of records operation on the database.
   *
   * @param query which is a String
   * @param handler which is a Request Handler
   * @return PostgresService which is a service
   */
  @Fluent
  PostgresService executeCountQuery(final String query, Handler<AsyncResult<JsonObject>> handler);

  @GenIgnore
  static PostgresService createProxy(Vertx vertx, String address) {
    return new PostgresServiceVertxEBProxy(vertx, address);
  }
}
