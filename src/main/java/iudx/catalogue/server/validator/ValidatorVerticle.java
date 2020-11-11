package iudx.catalogue.server.validator;

import static iudx.catalogue.server.util.Constants.*;
import io.vertx.core.AbstractVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.serviceproxy.ServiceBinder;
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

  private static final Logger LOGGER = LogManager.getLogger(ValidatorVerticle.class);

  private ValidatorService validator;
  private String databaseIP;
  private int databasePort;
  private String databaseUser;
  private String databasePassword;
  private ElasticClient client;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   */
  @Override
  public void start() throws Exception {

    databaseIP = config().getString(DATABASE_IP);
    databasePort = config().getInteger(DATABASE_PORT);
    databaseUser = config().getString(DATABASE_UNAME);
    databasePassword = config().getString(DATABASE_PASSWD);
    /* Create a reference to HazelcastClusterManager. */

    client = new ElasticClient(databaseIP, databasePort, databaseUser, databasePassword);

    /* Create or Join a Vert.x Cluster. */

    /* Publish the Validator service with the Event Bus against an address. */

    validator = new ValidatorServiceImpl(client);
    new ServiceBinder(vertx)
      .setAddress(VALIDATION_SERVICE_ADDRESS)
      .register(ValidatorService.class, validator);
  }
}
