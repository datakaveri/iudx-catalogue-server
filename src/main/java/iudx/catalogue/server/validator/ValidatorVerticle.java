package iudx.catalogue.server.validator;

import static iudx.catalogue.server.apiserver.util.Constants.UAC_DEPLOYMENT;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.catalogue.server.database.ElasticClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


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

  private ValidatorService validator;
  private String databaseIp;
  private String docIndex;
  private int databasePort;
  private String databaseUser;
  private String databasePassword;
  private ElasticClient client;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private boolean isUacInstance;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   */
  @Override
  public void start() throws Exception {
    binder = new ServiceBinder(vertx);
    databaseIp = config().getString(DATABASE_IP);
    databasePort = config().getInteger(DATABASE_PORT);
    databaseUser = config().getString(DATABASE_UNAME);
    databasePassword = config().getString(DATABASE_PASSWD);
    docIndex = config().getString(DOC_INDEX);
    isUacInstance = config().getBoolean(UAC_DEPLOYMENT);
    /* Create a reference to HazelcastClusterManager. */

    client = new ElasticClient(databaseIp, databasePort, docIndex, databaseUser, databasePassword);

    /* Create or Join a Vert.x Cluster. */

    /* Publish the Validator service with the Event Bus against an address. */

    validator = new ValidatorServiceImpl(client, docIndex, isUacInstance);
    consumer =
        binder.setAddress(VALIDATION_SERVICE_ADDRESS)
      .register(ValidatorService.class, validator);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
