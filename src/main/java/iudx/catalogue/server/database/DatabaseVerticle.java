package iudx.catalogue.server.database;

import static iudx.catalogue.server.util.Constants.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.nlpsearch.NLPSearchService;
import iudx.catalogue.server.geocoding.GeocodingService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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
  private String ratingIndex;
  private String databaseUser;
  private String databasePassword;
  private int databasePort;
  private ElasticClient client;
  private JsonArray optionalModules;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   * 
   * @throws Exception which is a start up exception.
   */

  @Override
  public void start() throws Exception {
    binder = new ServiceBinder(vertx);
    databaseIP = config().getString(DATABASE_IP);
    databasePort = config().getInteger(DATABASE_PORT);
    databaseUser = config().getString(DATABASE_UNAME);
    databasePassword = config().getString(DATABASE_PASSWD);
    docIndex = config().getString(DOC_INDEX);
    ratingIndex = config().getString(RATING_INDEX);
    optionalModules = config().getJsonArray(OPTIONAL_MODULES);

    client = new ElasticClient(databaseIP, databasePort, docIndex, databaseUser, databasePassword);

    if(optionalModules.contains(NLPSEARCH_PACKAGE_NAME) 
        && optionalModules.contains(GEOCODING_PACKAGE_NAME)) {
      NLPSearchService nlpService = NLPSearchService.createProxy(vertx, NLP_SERVICE_ADDRESS);
      GeocodingService geoService = GeocodingService.createProxy(vertx, GEOCODING_SERVICE_ADDRESS);
      database = new DatabaseServiceImpl(client, docIndex, ratingIndex, nlpService, geoService);
    } else {
      database = new DatabaseServiceImpl(client, docIndex, ratingIndex);
    }

    consumer =
        binder.setAddress(DATABASE_SERVICE_ADDRESS)
      .register(DatabaseService.class, database);

  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
