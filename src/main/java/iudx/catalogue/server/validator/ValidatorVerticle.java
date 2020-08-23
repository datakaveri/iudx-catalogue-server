package iudx.catalogue.server.validator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.serviceproxy.ServiceBinder;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

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

  private static final String VALIDATION_SERVICE_ADDRESS = "iudx.catalogue.validator.service";
  private static final Logger LOGGER = LogManager.getLogger(ValidatorVerticle.class);

  private Vertx vertx;
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

      LOGGER.info(ex.toString());
    }
    /* Create a reference to HazelcastClusterManager. */

    client = new ElasticClient(databaseIP, databasePort);

    /* Create or Join a Vert.x Cluster. */

    /* Publish the Validator service with the Event Bus against an address. */

    validator = new ValidatorServiceImpl(client);
    new ServiceBinder(vertx)
      .setAddress(VALIDATION_SERVICE_ADDRESS)
      .register(ValidatorService.class, validator);
  }
}
