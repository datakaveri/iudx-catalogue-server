package iudx.catalogue.server.rating;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.database.postgres.PostgresService;
import iudx.catalogue.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.catalogue.server.util.Constants.*;

/**
 *
 *
 * <h1>Rating Verticle</h1>
 *
 * <p>The Rating Verticle implementation in the IUDX Catalogue Server exposes the {@link
 * iudx.catalogue.server.rating.RatingService} over the Vert.x Event Bus
 *
 * @version 1.0
 * @since 2022-05-30
 */
public class RatingVerticle extends AbstractVerticle {

  private static final String RATING_SERVICE_ADDRESS = "iudx.catalogue.rating.service";
  private static final Logger LOGGER = LogManager.getLogger(RatingVerticle.class);

  DatabaseService databaseService;
  DataBrokerService dataBrokerService;
  PostgresService postgresService;
  private String ratingExchangeName;
  private String rsauditingtable;
  private int minReadNumber;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private RatingService rating;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   *
   * @throws Exception which is a start up exception.
   */
  @Override
  public void start() throws Exception {

    ratingExchangeName = config().getString("ratingExchangeName");
    rsauditingtable = config().getString("rsAuditingTableName");
    minReadNumber = config().getInteger("minReadNumber");

    databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
    dataBrokerService = DataBrokerService.createProxy(vertx, BROKER_SERVICE_ADDRESS);
    postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);

    binder = new ServiceBinder(vertx);
    rating = new RatingServiceImpl(ratingExchangeName, rsauditingtable, minReadNumber, databaseService, dataBrokerService, postgresService);
    consumer = binder.setAddress(RATING_SERVICE_ADDRESS).register(RatingService.class, rating);
    LOGGER.info("Rating Service Started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
