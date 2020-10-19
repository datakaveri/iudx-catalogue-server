package iudx.catalogue.server.apiserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.DeploymentOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;
import io.vertx.reactivex.ext.web.client.WebClient;
import iudx.catalogue.server.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.ThrowingConsumer;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Iterator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;


import static iudx.catalogue.server.Constants.*;
import static iudx.catalogue.server.apiserver.util.Constants.*;
import iudx.catalogue.server.deploy.ReDeployerDev;



/**
 * Test class for ApiServerVerticle api handlers.
 * 
 * @see {@link ApiServerVerticle}
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IntegrationTest {


  /** Configs and test vectors */
  /* Cat main config */
  private static final String SERVER_CONFIG_PATH =  "./configs/config-test.json";


  /* test vectors */
  private static final String PROVIDER_TEST_VECTOR_PATH =  "./src/test/resources/providerVector.json";
  private static final String CRUD_TEST_VECTOR_PATH =  "./src/test/resources/crudVector.json";


  private static JsonObject providerVector;

  /* LOGGER instance */
  private static final Logger LOGGER = LogManager.getLogger(IntegrationTest.class);
  private static String HOST = "";
  private static int PORT;
  private static final String BASE_URL = "http://127.0.0.1:8443/iudx/cat/v1/";
  private String URL = "";

  /** Token for crud apis */
  private static String TOKEN = "";
  private static String ADMIN_TOKEN ="";

  private static WebClient client;
  private static FileSystem fileSystem;
  private static Configuration config;

  IntegrationTest() {}

  /**
   * Starting the Catalogue-Server in clustered mode, before the execution of tests
   * 
   * @param testContext of asynchronous operations
   * @param vertx initializing the core vertx apis
   * @throws InterruptedException generated when a thread is interrupted
   */
  @BeforeAll
  @DisplayName("Deploy a apiserver")
  static void bringUp() throws InterruptedException {

    /* configuration setup */
    JsonObject catConfig = Configuration.getConfiguration(SERVER_CONFIG_PATH);
    JsonObject apiVerticleConfig = Configuration.getConfiguration(SERVER_CONFIG_PATH, 3);

    /* testVector */
    providerVector = Configuration.getConfiguration(PROVIDER_TEST_VECTOR_PATH);

    String keyStore = apiVerticleConfig.getString(KEYSTORE_PATH);
    String keyStorePassword = apiVerticleConfig.getString(KEYSTORE_PASSWORD);
    HOST = apiVerticleConfig.getString("ip");
    PORT = apiVerticleConfig.getInteger("port");
    TOKEN = apiVerticleConfig.getString(HEADER_TOKEN);
    ADMIN_TOKEN = apiVerticleConfig.getString("admin_token");
   


    // ReDeployerDev dep = new ReDeployerDev();
    // Promise<String> catPromise = Promise.promise();

    // new Thread(() -> {
    //   vertx.deployVerticle("iudx.catalogue.server.deploy.ReDeployerDev",
    //               new DeploymentOptions()
    //                   .setConfig(catConfig), catPromise);
    // }).start();
    // catPromise.future().onSuccess( res -> {
    //   vertx.setTimer(1000, id -> {
    //     LOGGER.info("Info: Proceeding with tests");
    //     testContext.completeNow();
    //   });
    // });

  }

  private boolean createItem(JsonObject obj)
      throws URISyntaxException, IOException, InterruptedException {

    LOGGER.info("Info: Testing " + obj.getString("name"));

    HttpClient client = HttpClient.newBuilder().build();
    HttpRequest request = HttpRequest.newBuilder(new URI(BASE_URL + obj.getString("path")))
                                      .POST(BodyPublishers
                                            .ofString(obj.getJsonObject("data").toString()))
                                      .headers("content-type", "application/json")
                                      .headers("token", TOKEN)
                                      .build();
    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
    boolean assertionStatus = true;
    for ( Object a : obj.getJsonArray("assertion")) {
      JsonObject assertion = (JsonObject) a;
      if (assertion.containsKey("status") &&
            assertion.getInteger("status") != response.statusCode()){
        assertionStatus = false;
      }
    }
    return assertionStatus;

  }

  /**
   * Tests the createItem of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   * @throws InterruptedException
   */
  @Order(1)
  @DisplayName("Provider Tests")
  @TestFactory
  public Iterator<DynamicTest> providerTests() {
    ArrayList<DynamicTest> tests = new ArrayList<DynamicTest>(); 

    for (Object t : providerVector.getJsonArray("tests")) {
      JsonObject test = (JsonObject) t;
      tests.add(dynamicTest(
            test.getString("name"),
            () -> assertTrue(createItem(test))
            ));
    }
    return tests.iterator();
  }


}
