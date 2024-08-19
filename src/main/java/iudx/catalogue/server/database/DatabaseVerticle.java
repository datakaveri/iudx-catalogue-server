package iudx.catalogue.server.database;

import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.catalogue.server.geocoding.GeocodingService;
import iudx.catalogue.server.nlpsearch.NLPSearchService;

/**
 * The Database Verticle.
 *
 * <h1>Database Verticle</h1>
 *
 * <p>The Database Verticle implementation in the the IUDX Catalogue Server exposes the {@link
 * iudx.catalogue.server.database.DatabaseService} over the Vert.x Event Bus.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DatabaseVerticle extends AbstractVerticle {

  private DatabaseService database;
  private String databaseIp;
  private String docIndex;
  private String ratingIndex;
  private String mlayerIndex;
  private String mlayerDomainIndex;
  private String databaseUser;
  private String databasePassword;
  private int databasePort;
  private ElasticClient client;
  private JsonArray optionalModules;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  /**
   * Helper function to create a WebClient to talk to the vocabulary server.
   *
   * @param vertx the vertx instance
   * @return a web client initialized with the relevant client certificate
   */
  static WebClient createWebClient(Vertx vertx) {
    WebClientOptions webClientOptions = new WebClientOptions();
    return WebClient.create(vertx, webClientOptions);
  }

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
    databaseIp = config().getString(DATABASE_IP);
    databasePort = config().getInteger(DATABASE_PORT);
    databaseUser = config().getString(DATABASE_UNAME);
    databasePassword = config().getString(DATABASE_PASSWD);
    docIndex = config().getString(DOC_INDEX);
    ratingIndex = config().getString(RATING_INDEX);
    mlayerIndex = config().getString(MLAYER_INSTANCE_INDEX);
    mlayerDomainIndex = config().getString(MLAYER_DOMAIN_INDEX);
    optionalModules = config().getJsonArray(OPTIONAL_MODULES);

    client = new ElasticClient(databaseIp, databasePort, docIndex, databaseUser, databasePassword);

    if (optionalModules.contains(NLPSEARCH_PACKAGE_NAME)
        && optionalModules.contains(GEOCODING_PACKAGE_NAME)) {
      NLPSearchService nlpService = NLPSearchService.createProxy(vertx, NLP_SERVICE_ADDRESS);
      GeocodingService geoService = GeocodingService.createProxy(vertx, GEOCODING_SERVICE_ADDRESS);
      database =
          new DatabaseServiceImpl(
              createWebClient(vertx),
              client,
              docIndex,
              ratingIndex,
              mlayerIndex,
              mlayerDomainIndex,
              nlpService,
              geoService);
    } else {
      database =
          new DatabaseServiceImpl(
              createWebClient(vertx),
              client,
              docIndex,
              ratingIndex,
              mlayerIndex,
              mlayerDomainIndex);
    }

    consumer =
        binder.setAddress(DATABASE_SERVICE_ADDRESS).register(DatabaseService.class, database);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
