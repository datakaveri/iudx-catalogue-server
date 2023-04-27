package iudx.catalogue.server.databroker;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;

/**
 * The Data Broker Verticle.
 *
 * <h1>Data Broker Verticle</h1>
 *
 * <p>The Data Broker Verticle implementation in the the IUDX Catalogue Server exposes the {@link
 * iudx.catalogue.server.databroker.DataBrokerService} over the Vert.x Event Bus.
 *
 * @version 1.0
 * @since 2022-06-23
 */
public class DataBrokerVerticle extends AbstractVerticle {

  public static final String BROKER_SERVICE_ADDRESS = "iudx.catalogue.databroker.service";

  private DataBrokerService dataBrokerService;
  private RabbitMQOptions options;
  private RabbitMQClient client;
  private String dataBrokerIp;
  private int dataBrokerPort;
  private String dataBrokerVhost;
  private String dataBrokerUserName;
  private String dataBrokerPassword;
  private int connectionTimeout;
  private int requestedHeartbeat;
  private int handshakeTimeout;
  private int requestedChannelMax;
  private int networkRecoveryInterval;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   */
  @Override
  public void start() throws Exception {
    dataBrokerIp = config().getString("dataBrokerIP");
    dataBrokerPort = config().getInteger("dataBrokerPort");
    dataBrokerVhost = config().getString("dataBrokerVhost");
    dataBrokerUserName = config().getString("dataBrokerUserName");
    dataBrokerPassword = config().getString("dataBrokerPassword");
    connectionTimeout = config().getInteger("connectionTimeout");
    requestedHeartbeat = config().getInteger("requestedHeartbeat");
    handshakeTimeout = config().getInteger("handshakeTimeout");
    requestedChannelMax = config().getInteger("requestedChannelMax");
    networkRecoveryInterval = config().getInteger("networkRecoveryInterval");

    options = new RabbitMQOptions();
    options.setUser(dataBrokerUserName);
    options.setPassword(dataBrokerPassword);
    options.setHost(dataBrokerIp);
    options.setPort(dataBrokerPort);
    options.setVirtualHost(dataBrokerVhost);
    options.setConnectionTimeout(connectionTimeout);
    options.setRequestedHeartbeat(requestedHeartbeat);
    options.setHandshakeTimeout(handshakeTimeout);
    options.setRequestedChannelMax(requestedChannelMax);
    options.setNetworkRecoveryInterval(networkRecoveryInterval);
    options.setAutomaticRecoveryEnabled(true);

    client = RabbitMQClient.create(vertx, options);

    binder = new ServiceBinder(vertx);
    dataBrokerService = new DataBrokerServiceImpl(client);

    consumer =
        binder
            .setAddress(BROKER_SERVICE_ADDRESS)
            .register(DataBrokerService.class, dataBrokerService);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
