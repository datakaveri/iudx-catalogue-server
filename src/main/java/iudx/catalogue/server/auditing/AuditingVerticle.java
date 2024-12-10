package iudx.catalogue.server.auditing;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Auditing Verticle.
 * <h1>Auditing Verticle</h1>
 *
 * <p>
 * The Auditing Verticle implementation in the IUDX Catalouge Server exposes the
 * {@link iudx.catalogue.server.auditing.AuditingService} over the Vert.x Event Bus.
 * </p>
 *
 * @version 1.0
 * @since 2021-08-30
 */

public class AuditingVerticle extends AbstractVerticle {

  private static final String AUDITING_SERVICE_ADDRESS = "iudx.catalogue.auditing.service";
  private static final Logger LOGGER = LogManager.getLogger(AuditingVerticle.class);
  PgConnectOptions connectOptions;
  PoolOptions poolOptions;
  PgPool pool;
  private String databaseIp;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private String databaseTableName;
  private int poolSize;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private AuditingService auditing;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   *
   * @throws Exception which is a start up exception.
   */

  @Override
  public void start() throws Exception {

    databaseIp = config().getString("auditingDatabaseIP");
    databasePort = config().getInteger("auditingDatabasePort");
    databaseName = config().getString("auditingDatabaseName");
    databaseUserName = config().getString("auditingDatabaseUserName");
    databasePassword = config().getString("auditingDatabasePassword");
    databaseTableName = config().getString("auditingDatabaseTableName");
    poolSize = config().getInteger("auditingPoolSize");

    JsonObject propObj = new JsonObject();
    propObj.put("auditingDatabaseIP", databaseIp);
    propObj.put("auditingDatabasePort", databasePort);
    propObj.put("auditingDatabaseName", databaseName);
    propObj.put("auditingDatabaseUserName", databaseUserName);
    propObj.put("auditingDatabasePassword", databasePassword);
    propObj.put("auditingDatabaseTableName", databaseTableName);
    propObj.put("auditingPoolSize", poolSize);

    binder = new ServiceBinder(vertx);
    auditing = new AuditingServiceImpl(propObj, vertx);
    consumer = binder.setAddress(AUDITING_SERVICE_ADDRESS)
            .register(AuditingService.class, auditing);
    LOGGER.info("Auditing Service Started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
