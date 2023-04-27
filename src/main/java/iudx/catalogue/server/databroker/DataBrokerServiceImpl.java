package iudx.catalogue.server.databroker;

import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * The Data Broker Service Implementation.
 *
 * <h1>Data Broker Service Implementation</h1>
 *
 * <p>The Data Broker Service implementation in the IUDX Catalogue Server implements the definitions
 * of the {@link iudx.catalogue.server.databroker.DataBrokerService}.
 *
 * @version 1.0
 * @since 2022-06-23
 */
public class DataBrokerServiceImpl implements DataBrokerService {
  private static final Logger LOGGER = LogManager.getLogger(DataBrokerServiceImpl.class);

  private RabbitMQClient client;

  public DataBrokerServiceImpl(RabbitMQClient client) {
    this.client = client;
    this.client.start(
        startHandler -> {
          if (startHandler.succeeded()) {
            LOGGER.info("RMQ started");
          } else {
            LOGGER.error("RMQ startup failed");
          }
        });
  }

  /** This method will only publish messages to internal-communication exchanges. */
  @Override
  public DataBrokerService publishMessage(
      JsonObject body,
      String toExchange,
      String routingKey,
      Handler<AsyncResult<JsonObject>> handler) {

    Buffer buffer = Buffer.buffer(body.toString());

    if (!client.isConnected()) {
      client.start();
    }

    client.basicPublish(
        toExchange,
        routingKey,
        buffer,
        publishHandler -> {
          if (publishHandler.succeeded()) {
            JsonObject result = new JsonObject().put("type", TYPE_SUCCESS);
            handler.handle(Future.succeededFuture(result));
          } else {
            RespBuilder respBuilder =
                new RespBuilder()
                    .withType(TYPE_INTERNAL_SERVER_ERROR)
                    .withTitle(TITLE_INTERNAL_SERVER_ERROR)
                    .withDetail(publishHandler.cause().getLocalizedMessage());
            handler.handle(Future.failedFuture(respBuilder.getResponse()));
          }
        });
    return this;
  }
}
