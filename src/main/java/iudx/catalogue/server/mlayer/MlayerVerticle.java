package iudx.catalogue.server.mlayer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.catalogue.server.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.catalogue.server.util.Constants.DATABASE_SERVICE_ADDRESS;

public class MlayerVerticle extends AbstractVerticle {
  private static final String MLAYER_SERVICE_ADDRESS = "iudx.catalogue.mlayer.service";
  private static final Logger LOGGER = LogManager.getLogger(MlayerVerticle.class);
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private MlayerService mlayer;
  DatabaseService databaseService;

  @Override
  public void start() throws Exception {
    databaseService = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
    binder = new ServiceBinder(vertx);
    mlayer = new MlayerServiceImpl(databaseService);
    consumer = binder.setAddress(MLAYER_SERVICE_ADDRESS).register(MlayerService.class, mlayer);
    LOGGER.info("Mlayer Service Started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
