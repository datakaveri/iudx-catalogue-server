package iudx.catalogue.server.validator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.apache.http.HttpHost;

import iudx.catalogue.server.database.ElasticClient;

/**
 * The Validator Verticle.
 *
 * <h1>Validator Verticle</h1>
 *
 * <p>The Validator Verticle implementation in the the IUDX Catalogue Server exposes the {@link
 * iudx.catalogue.server.validator.ValidatorService} over the Vert.x Event Bus.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class ValidatorVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(ValidatorVerticle.class);
  private Vertx vertx;
  private ClusterManager mgr;
  private VertxOptions options;
  private ServiceDiscovery discovery;
  private Record record;
  private ValidatorService validator;
  private Properties properties;
  private InputStream inputstream;
  private String databaseIP;
  private int databasePort;
  private ElasticClient client;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   */
  @Override
  public void start(Future<Void> startFuture) throws Exception {

    properties = new Properties();
    inputstream = null;

    try {

      inputstream = new FileInputStream("config.properties");
      properties.load(inputstream);

      databaseIP = properties.getProperty("databaseIP");
      databasePort = Integer.parseInt(properties.getProperty("databasePort"));

    } catch (Exception ex) {

      logger.info(ex.toString());
    }
    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);
    client = new ElasticClient(databaseIP, databasePort);

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(
        options,
        res -> {
          if (res.succeeded()) {
            vertx = res.result();

            /* Publish the Validator service with the Event Bus against an address. */

            validator = new ValidatorServiceImpl(client);
            new ServiceBinder(vertx)
                .setAddress("iudx.catalogue.validator.service")
                .register(ValidatorService.class, validator);

            /* Get a handler for the Service Discovery interface and publish a service record. */

            discovery = ServiceDiscovery.create(vertx);
            record =
                EventBusService.createRecord(
                    "iudx.catalogue.validator.service",
                    "iudx.catalogue.validator.service",
                    ValidatorService.class);

            discovery.publish(
                record,
                publishRecordHandler -> {
                  if (publishRecordHandler.succeeded()) {
                    Record publishedRecord = publishRecordHandler.result();
                    startFuture.complete();
                    logger.info("Publication succeeded " + publishedRecord.toJson());
                  } else {
                    logger.info("Publication failed " + publishRecordHandler.result());
                  }
                });
          }
        });
  }
}
