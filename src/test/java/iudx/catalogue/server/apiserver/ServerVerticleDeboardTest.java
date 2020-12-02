package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.HEADER_CONTENT_TYPE;
import static iudx.catalogue.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.catalogue.server.apiserver.util.Constants.MIME_APPLICATION_JSON;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.INSTANCE;
import static iudx.catalogue.server.util.Constants.KEYSTORE_PASSWORD;
import static iudx.catalogue.server.util.Constants.KEYSTORE_PATH;
import static iudx.catalogue.server.util.Constants.STATUS;
import static iudx.catalogue.server.util.Constants.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;
import io.vertx.reactivex.ext.web.client.WebClient;
import iudx.catalogue.server.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Iterator;

/**
 * Test class for ApiServerVerticle api handlers.
 * 
 * @see {@link ApiServerVerticle}
 */
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServerVerticleDeboardTest {

  /* LOGGER instance */
  private static final Logger LOGGER = LogManager.getLogger(ServerVerticleDeboardTest.class);
  private static String HOST = "";
  private static int PORT;
  private static final String BASE_URL = "/iudx/cat/v1/";

  /** Token for crud apis */
  private static String TOKEN = "";
  private static String ADMIN_TOKEN = "";

  private static WebClient client;
  private static FileSystem fileSystem;


  /**
   * Starting the Catalogue-Server in clustered mode, before the execution of delete tests
   * 
   * @param testContext of asynchronous operations
   * @param vertx initializing the core vertx apis
   * @throws InterruptedException generated when a thread is interrupted
   */
  @BeforeAll
  @DisplayName("Deploy a apiserver deboarder")
  static void startVertx(VertxTestContext testContext, Vertx vertx) throws InterruptedException {

    fileSystem = vertx.fileSystem();

    /* configuration setup */
    JsonObject apiVerticleConfig = Configuration.getConfiguration("./configs/config-test.json", 3);

    String keyStore = apiVerticleConfig.getString(KEYSTORE_PATH);
    String keyStorePassword = apiVerticleConfig.getString(KEYSTORE_PASSWORD);
    HOST = apiVerticleConfig.getString("ip");
    PORT = apiVerticleConfig.getInteger("port");
    TOKEN = apiVerticleConfig.getString(HEADER_TOKEN);
    ADMIN_TOKEN = apiVerticleConfig.getString("admin_token");


    /* Options for the web client connections */
    JksOptions options = new JksOptions().setPath(keyStore).setPassword(keyStorePassword);

    WebClientOptions clientOptions = new WebClientOptions().setSsl(true).setVerifyHost(false)
        .setTrustAll(true).setTrustStoreOptions(options);
    client = WebClient.create(vertx, clientOptions);

    testContext.completeNow();

  }



  /**
   * Tests the deleteItem of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(1)
  @DisplayName("Delete Item[Status:200, Endpoint: /item]")
  void deleteItem200(VertxTestContext testContext) {

    var wrapper = new Object() {
      int count = 0;
    };

    fileSystem.readFile("src/test/resources/resourcesToDelete.json", fileRes -> {
      if (fileRes.succeeded()) {
        JsonArray resources = fileRes.result().toJsonArray();
        Iterator<Object> objectIterator = resources.iterator();
        int numItems = resources.size();

        while (objectIterator.hasNext()) {

          JsonObject item = (JsonObject) objectIterator.next();
          /* Send the file to the server using DELETE */
          LOGGER.info("Deleting " + item.getString(ID));
          client.delete(PORT, HOST, BASE_URL.concat("item/")).addQueryParam(ID, item.getString(ID))
              .putHeader(HEADER_TOKEN, TOKEN).putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .send(serverResponse -> {
                if (serverResponse.succeeded()) {

                  /* comparing the response */
                  assertEquals(200, serverResponse.result().statusCode());
                  testContext.completeNow();
                  if (serverResponse.result().statusCode() == 200) {
                    wrapper.count++;
                  }
                  if (wrapper.count == numItems - 1) {
                    testContext.completeNow();
                  }
                } else if (serverResponse.failed()) {
                  testContext.failed();
                }
              });
        }
        testContext.completed();
      }
    });
  }


  /**
   * Tests the deletion of instance api
   * 
   * @param testContext of asynchronous operations
   * @throws InterruptedException
   */
  @Test
  @Order(2)
  @DisplayName("delete Instance, Status:200, Endpoint: /instance]")
  void deleteInstance200(VertxTestContext testContext) throws InterruptedException {

    /* Send the file to the server using GET with query parameters */
    /* Should give only one item */
    client.delete(PORT, HOST, BASE_URL.concat(INSTANCE)).addQueryParam(ID, "pune")
        .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON).putHeader(HEADER_TOKEN, ADMIN_TOKEN)
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {
            LOGGER.info(serverResponse.result().bodyAsString());
            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(SUCCESS, serverResponse.result().bodyAsJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  /**
   * Deletes all the prepare items- resourceServer, provider and resourceGroup items.
   * 
   * @param testContext
   */
  @Test
  @Order(3)
  void cleanPrepareItems(VertxTestContext testContext) {

    var wrapper = new Object() {
      int count = 0;
    };

    fileSystem.readFile("src/test/resources/prepareDeleteItems.json", fileRes -> {
      if (fileRes.succeeded()) {
        JsonArray resources = fileRes.result().toJsonArray();
        Iterator<Object> objectIterator = resources.iterator();
        int numItems = resources.size();

        while (objectIterator.hasNext()) {

          JsonObject item = (JsonObject) objectIterator.next();
          /* Send the file to the server using DELETE */
          LOGGER.info("Deleting " + item.getString(ID));
          client.delete(PORT, HOST, BASE_URL.concat("item/")).addQueryParam(ID, item.getString(ID))
              .putHeader(HEADER_TOKEN, TOKEN).putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .send(serverResponse -> {
                if (serverResponse.succeeded()) {

                  /* comparing the response */
                  assertEquals(200, serverResponse.result().statusCode());
                  testContext.completeNow();
                  if (serverResponse.result().statusCode() == 200) {
                    wrapper.count++;
                  }
                  if (wrapper.count == numItems - 1) {
                    testContext.completeNow();
                  }
                } else if (serverResponse.failed()) {
                  testContext.failed();
                }
              });
        }
        testContext.completed();
      }
    });
  }
}
