package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.HEADER_CONTENT_TYPE;
import static iudx.catalogue.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.catalogue.server.apiserver.util.Constants.MIME_APPLICATION_JSON;
import static iudx.catalogue.server.util.Constants.KEYSTORE_PASSWORD;
import static iudx.catalogue.server.util.Constants.KEYSTORE_PATH;
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
 * 
 * Tests & onboard resourceServer, provide and resourceGroup items.
 *
 */
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiServerVerticlePreprareTest {

  /* LOGGER instance */
  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticlePreprareTest.class);
  private static String HOST = "";
  private static int PORT;
  private static final String BASE_URL = "/iudx/cat/v1/";

  /** Token for crud apis */
  private static String TOKEN = "";
  private static String ADMIN_TOKEN = "";

  private static WebClient client;
  private static FileSystem fileSystem;
  
  @BeforeAll
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

    WebClientOptions clientOptions = new WebClientOptions()
                                            .setSsl(true)
                                            .setVerifyHost(false)
                                            .setTrustAll(true)
                                            .setTrustStoreOptions(options);
    client = WebClient.create(vertx, clientOptions);

    testContext.completeNow();
  }

  /**
   * Creates the resourceServer and provider items.
   * 
   * @param testContext
   * @throws InterruptedException
   */
  @Test
  @Order(1)
  @DisplayName("Preprare onboarder items[Status:201, Endpoint: /item]")
  public void createResSvrPvrdItem(VertxTestContext testContext) throws InterruptedException {

    var wrapper = new Object() {
      int count = 0;
    };

    fileSystem.readFile("src/test/resources/resourceSvrProvider.json", fileRes -> {
      if (fileRes.succeeded()) {

        JsonArray resources = fileRes.result().toJsonArray();
        int numItems = resources.size();
        LOGGER.info("Total items = " + String.valueOf(resources.size()));
        Iterator<Object> objectIterator = resources.iterator();


        while (objectIterator.hasNext()) {
          // Send the file to the server using POST
          client.post(PORT, HOST, BASE_URL.concat("item")).putHeader(HEADER_TOKEN, TOKEN)
              .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              // .putHeader("instance", "pune")
              .sendJson(objectIterator.next(), serverResponse -> {
                if (serverResponse.succeeded()) {
                  if (serverResponse.result().statusCode() == 201) {
                    wrapper.count++;
                    testContext.completeNow();
                  }
                  if (wrapper.count == numItems - 1) {
                    testContext.completeNow();
                  }
                  assertEquals(201, serverResponse.result().statusCode());
                } else if (serverResponse.failed()) {
                  testContext.failed();
                }
              });
        }
      } else if (fileRes.failed()) {
        LOGGER.error("Problem in reading test data file: ".concat(fileRes.cause().toString()));
      }
    });
  }

  /**
   * creates the resourceGroup item.
   * 
   * @param testContext
   * @throws InterruptedException
   */
  @Test
  @Order(2)
  @DisplayName("Preprare onboarder items[Status:201, Endpoint: /item]")
  public void createResourceGrpItem(VertxTestContext testContext) throws InterruptedException {

    Thread.sleep(5000);
    var wrapper = new Object() {
      int count = 0;
    };

    fileSystem.readFile("src/test/resources/resourceGroup.json", fileRes -> {
      if (fileRes.succeeded()) {

        JsonArray resources = fileRes.result().toJsonArray();
        int numItems = resources.size();
        LOGGER.info("Total items = " + String.valueOf(resources.size()));
        Iterator<Object> objectIterator = resources.iterator();


        while (objectIterator.hasNext()) {
          // Send the file to the server using POST
          client.post(PORT, HOST, BASE_URL.concat("item")).putHeader(HEADER_TOKEN, TOKEN)
              .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              // .putHeader("instance", "pune")
              .sendJson(objectIterator.next(), serverResponse -> {
                if (serverResponse.succeeded()) {
                  if (serverResponse.result().statusCode() == 201) {
                    wrapper.count++;
                    testContext.completeNow();
                  }
                  if (wrapper.count == numItems - 1) {
                    testContext.completeNow();
                  }
                  assertEquals(201, serverResponse.result().statusCode());
                } else if (serverResponse.failed()) {
                  testContext.failed();
                }
              });
        }
      } else if (fileRes.failed()) {
        LOGGER.error("Problem in reading test data file: ".concat(fileRes.cause().toString()));
      }
    });
  }
}
