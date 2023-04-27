package iudx.catalogue.server.auditing;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import iudx.catalogue.server.auditing.util.QueryBuilder;
import iudx.catalogue.server.auditing.util.ResponseBuilder;
import iudx.catalogue.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.catalogue.server.auditing.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

public class AuditingServiceImpl implements AuditingService {

  private static final Logger LOGGER = LogManager.getLogger(AuditingServiceImpl.class);
  private final String METHOD_COLUMN_NAME;
  private final String TIME_COLUMN_NAME;
  private final String USERID_COLUMN_NAME;
  private final String BODY_COLUMN_NAME;
  private final String ENDPOINT_COLUMN_NAME;
  private final String API_COLUMN_NAME;
  private final String IID_COLUMN_NAME;
  private final String IUDX_COLUMN_NAME;
  private final String USERROLE_COLUMN_NAME;
  PgConnectOptions connectOptions;
  PoolOptions poolOptions;
  PgPool pool;
  private final QueryBuilder queryBuilder = new QueryBuilder();
  private JsonObject query = new JsonObject();
  private String databaseIp;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int databasePoolSize;
  private String databaseTableName;
  private ResponseBuilder responseBuilder;
  public static DataBrokerService rmqService;

  public AuditingServiceImpl(JsonObject propObj, Vertx vertxInstance) {
    if (propObj != null && !propObj.isEmpty()) {
      databaseIp = propObj.getString("auditingDatabaseIP");
      databasePort = propObj.getInteger("auditingDatabasePort");
      databaseName = propObj.getString("auditingDatabaseName");
      databaseUserName = propObj.getString("auditingDatabaseUserName");
      databasePassword = propObj.getString("auditingDatabasePassword");
      databaseTableName = propObj.getString("auditingDatabaseTableName");
      databasePoolSize = propObj.getInteger("auditingPoolSize");
    }

    this.connectOptions =
        new PgConnectOptions()
            .setPort(databasePort)
            .setHost(databaseIp)
            .setDatabase(databaseName)
            .setUser(databaseUserName)
            .setPassword(databasePassword)
            .setReconnectAttempts(2)
            .setReconnectInterval(1000);

    this.poolOptions = new PoolOptions().setMaxSize(databasePoolSize);
    this.pool = PgPool.pool(vertxInstance, connectOptions, poolOptions);
    this.rmqService = DataBrokerService.createProxy(vertxInstance, BROKER_SERVICE_ADDRESS);

    METHOD_COLUMN_NAME =
        _METHOD_COLUMN_NAME
            .insert(0, "(" + databaseName + "." + databaseTableName + ".")
            .toString();
    TIME_COLUMN_NAME =
        _TIME_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    USERID_COLUMN_NAME =
        _USERID_COLUMN_NAME
            .insert(0, "(" + databaseName + "." + databaseTableName + ".")
            .toString();
    BODY_COLUMN_NAME =
        _BODY_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();

    ENDPOINT_COLUMN_NAME =
        _ENDPOINT_COLUMN_NAME
            .insert(0, "(" + databaseName + "." + databaseTableName + ".")
            .toString();

    API_COLUMN_NAME =
        _API_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    IID_COLUMN_NAME =
        _IID_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    IUDX_COLUMN_NAME =
        _IUDX_COLUMN_NAME.insert(0, "(" + databaseName + "." + databaseTableName + ".").toString();
    USERROLE_COLUMN_NAME =
        _USERROLE_COLUMN_NAME
            .insert(0, "(" + databaseName + "." + databaseTableName + ".")
            .toString();
  }

  @Override
  public AuditingService insertAuditngValuesInRmq(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    request.put(DATABASE_TABLE_NAME, databaseTableName);
    JsonObject rmqMessage = new JsonObject();

    rmqMessage = queryBuilder.buildMessageForRmq(request);

    LOGGER.debug("audit rmq Message body: " + rmqMessage);
    rmqService.publishMessage(
        rmqMessage,
        EXCHANGE_NAME,
        ROUTING_KEY,
        rmqHandler -> {
          if (rmqHandler.succeeded()) {
            handler.handle(Future.succeededFuture());
            LOGGER.info("inserted into rmq");
          } else {
            LOGGER.debug("failed to insert into rmq");
            LOGGER.error(rmqHandler.cause());
            handler.handle(Future.failedFuture(rmqHandler.cause().getMessage()));
          }
        });
    return this;
  }
}
