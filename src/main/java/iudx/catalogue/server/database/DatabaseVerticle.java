package iudx.catalogue.server.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.serviceproxy.ServiceBinder;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

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
  private Properties properties;
  private InputStream inputstream;
  private String databaseIP;
  private int databasePort;
  private ElasticClient client;
  private static final String DATABASE_SERVICE_ADDRESS = "iudx.catalogue.database.service";

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   * 
   * @throws Exception which is a start up exception.
   */

  @Override
  public void start() throws Exception {

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

    client = new ElasticClient(databaseIP, databasePort);

    database = new DatabaseServiceImpl(client);
    new ServiceBinder(vertx).setAddress(DATABASE_SERVICE_ADDRESS)
      .register(DatabaseService.class, database);

  }

}
