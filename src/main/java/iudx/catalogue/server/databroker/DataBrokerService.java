package iudx.catalogue.server.databroker;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Data Broker Service.
 *
 * <h1>Data Broker Service</h1>
 *
 * <p>The Data Broker Service in the IUDX Catalogue Server defines the operations to be performed
 * with the IUDX Data Broker server.
 *
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2022-06-23
 */
@VertxGen
@ProxyGen
public interface DataBrokerService {

  @Fluent
  DataBrokerService publishMessage(
      JsonObject body,
      String toExchange,
      String routingKey,
      Handler<AsyncResult<JsonObject>> handler);

  @GenIgnore
  static DataBrokerService createProxy(Vertx vertx, String address) {
    return new DataBrokerServiceVertxEBProxy(vertx, address);
  }
}
