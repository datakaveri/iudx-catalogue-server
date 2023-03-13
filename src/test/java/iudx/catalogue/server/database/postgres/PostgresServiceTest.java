package iudx.catalogue.server.database.postgres;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import iudx.catalogue.server.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static iudx.catalogue.server.mlayer.util.Constants.GET_HIGH_COUNT_DATASET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(VertxExtension.class)
public class PostgresServiceTest {
  public static final Logger LOGGER = LogManager.getLogger(PostgresServiceTest.class);
  private static PostgresServiceImpl pgService;
  private static PostgreSQLContainer<?> postgresContainer;
  // @TODO : change configs to get image version, image version and version used by IUDX should be
  // same.
  public static String CONTAINER = "postgres:12.11";
  static String table;
  private static JsonObject dbConfig;

  @BeforeAll
  static void setup(Vertx vertx, VertxTestContext testContext) {
    dbConfig = Configuration.getConfiguration("./configs/config-test.json", 10);

    dbConfig.put("databaseIp", "localhost");
    dbConfig.put("databasePort", 5432);
    dbConfig.put("databaseName", "postgres");
    dbConfig.put("databaseUserName", "postgres");
    dbConfig.put("databasePassword", "qwerty123");
    dbConfig.put("poolSize", 25);
    table = dbConfig.getString("auditingTableName");
    postgresContainer = new PostgreSQLContainer<>(CONTAINER).withInitScript("pg_test_schema.sql");

    postgresContainer.withUsername(dbConfig.getString("databaseUserName"));
    postgresContainer.withPassword(dbConfig.getString("databasePassword"));
    postgresContainer.withDatabaseName(dbConfig.getString("databaseName"));
    postgresContainer.withExposedPorts(dbConfig.getInteger("databasePort"));

    postgresContainer.start();
    if (postgresContainer.isRunning()) {
      dbConfig.put("databasePort", postgresContainer.getFirstMappedPort());

      PgConnectOptions connectOptions =
          new PgConnectOptions()
              .setPort(dbConfig.getInteger("databasePort"))
              .setHost(dbConfig.getString("databaseIp"))
              .setDatabase(dbConfig.getString("databaseName"))
              .setUser(dbConfig.getString("databaseUserName"))
              .setPassword(dbConfig.getString("databasePassword"))
              .setReconnectAttempts(2)
              .setReconnectInterval(1000);

      PoolOptions poolOptions = new PoolOptions().setMaxSize(dbConfig.getInteger("poolSize"));
      PgPool pool = PgPool.pool(vertx, connectOptions, poolOptions);

      pgService = new PostgresServiceImpl(pool);
      testContext.completeNow();
    } else {
      testContext.failNow("setup failed");
    }
  }
  @Test
  @Order(1)
  @DisplayName("Test execute query - success")
  public void testExecuteQuerySuccess(VertxTestContext testContext) {
    StringBuilder stringBuilder =
        new StringBuilder(GET_HIGH_COUNT_DATASET.replace("$1", table));

    String expected =
        "{\"type\":\"urn:dx:cat:Success\",\"title\":\"Success\",\"results\":[{\"rgid\":\"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information\",\"totalhits\":1}]}";
    pgService.executeQuery(
        stringBuilder.toString(),
        handler -> {
          if (handler.succeeded()) {
            assertEquals(expected, handler.result().toString());
            assertTrue(handler.result().containsKey("type"));
            assertTrue(handler.result().containsKey("title"));
            assertTrue(handler.result().containsKey("results"));
            assertEquals("Success", handler.result().getString("title"));
            assertEquals("urn:dx:cat:Success", handler.result().getString("type"));
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(2)
  @DisplayName("test execute query - failure")
  public void testExecuteQueryFailure(VertxTestContext testContext) {

    StringBuilder stringBuilder =
        new StringBuilder("select * from nosuchtable");

    String expected =
        "{\"type\":\"urn:dx:cat:DatabaseError\",\"title\":\"database error\",\"detail\":\"ERROR: relation \\\"nosuchtable\\\" does not exist (42P01)\"}";
    pgService.executeQuery(stringBuilder.toString(), handler -> {
      if(handler.failed()) {
        assertEquals(expected,handler.cause().getMessage());
        testContext.completeNow();
      } else {
        testContext.failNow("test execute query unexpectedly failed");
      }
    });
  }

  @Test
  @Order(3)
  @DisplayName("Test execute count query - success")
  public void testExecuteCountQuery(VertxTestContext testContext) {
    StringBuilder stringBuilder = new StringBuilder("select count(*) from " + table);

    pgService.executeCountQuery(stringBuilder.toString(), handler -> {
      if(handler.succeeded()) {
        JsonObject result = handler.result();
        int hits = result.getInteger("totalHits");
        assertEquals(1, hits);
        testContext.completeNow();
      } else {
        testContext.failNow("execute count test failed");
      }
    });
  }

  @Test
  @Order(4)
  @DisplayName("Test execute count query - failure")
  public void testExecuteCountQueryFailure(VertxTestContext testContext) {
    String query = "select count(*) from nosuchtable";

    String expected =
        "{\"type\":\"urn:dx:cat:DatabaseError\",\"title\":\"database error\",\"detail\":\"ERROR: relation \\\"nosuchtable\\\" does not exist (42P01)\"}";
    pgService.executeCountQuery(query, handler -> {
      if(handler.failed()) {
        assertEquals(expected,handler.cause().getMessage());
        testContext.completeNow();
      } else {
        testContext.failNow("execute count unexpectedly failed");
      }
    });
  }

}
