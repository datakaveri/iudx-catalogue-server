package iudx.catalogue.server.database;

import static iudx.catalogue.server.util.Constants.*;
import io.vertx.core.AbstractVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import iudx.catalogue.server.database.ElasticClient;
/**
 * The Database Verticle.
 * <h1>Database Verticle</h1>
 * <p>
 * The Database Verticle implementation in the the IUDX Catalogue Server exposes the
 * {@link iudx.catalogue.server.database.DatabaseService} over the Vert.x Event Bus.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class DatabaseVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseVerticle.class);
  private DatabaseService database;
  private String databaseIP;
  private String docIndex;
  private String databaseUser;
  private String databasePassword;
  private int databasePort;
  private ElasticClient client;
  private WebClient webClient;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   * 
   * @throws Exception which is a start up exception.
   */

  @Override
  public void start() throws Exception {

    databaseIP = config().getString(DATABASE_IP);
    databasePort = config().getInteger(DATABASE_PORT);
    databaseUser = config().getString(DATABASE_UNAME);
    databasePassword = config().getString(DATABASE_PASSWD);
    docIndex = config().getString(DOC_INDEX);


    client = new ElasticClient(databaseIP, databasePort, docIndex, databaseUser, databasePassword);

    WebClientOptions webClientOptions = new WebClientOptions();
    webClientOptions.setTrustAll(true).setVerifyHost(false);
    webClient = WebClient.create(vertx, webClientOptions);

    database = new DatabaseServiceImpl(client, webClient);
    new ServiceBinder(vertx).setAddress(DATABASE_SERVICE_ADDRESS)
      .register(DatabaseService.class, database);

  }

}
