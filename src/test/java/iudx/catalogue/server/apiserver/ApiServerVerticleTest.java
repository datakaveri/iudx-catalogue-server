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
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

/**
 * Test class for ApiServerVerticle api handlers.
 * 
 * @see {@link ApiServerVerticle}
 */
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiServerVerticleTest {

  /* LOGGER instance */
  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticleTest.class);
  private static String HOST = "";
  private static int PORT;
  private static final String BASE_URL = "/iudx/cat/v1/";

  /** Token for crud apis */
  private static String TOKEN = "";
  private static String ADMIN_TOKEN ="";

  private static WebClient client;
  private static FileSystem fileSystem;

  ApiServerVerticleTest() {}

  /**
   * Starting the Catalogue-Server in clustered mode, before the execution of tests
   * 
   * @param testContext of asynchronous operations
   * @param vertx initializing the core vertx apis
   * @throws InterruptedException generated when a thread is interrupted
   */
  @BeforeAll
  @DisplayName("Deploy a apiserver onboarder")
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

    /**
     * Comment this block if you want to use an already running instance/hotswapped instance
     **/


    // CatalogueServerStarter starter = new CatalogueServerStarter();
    // Future<JsonObject> result = starter.startServer();
    // result.onComplete(resultHandler -> {
    // if (resultHandler.succeeded()) {
    // vertx.setTimer(15000, id -> {
    // LOGGER.info("!!!!!!!!\n\n!!!!!!!!!");
    // testContext.completeNow();
    // });
    // }
    // });


    /**
     * End
     **/

  }


  /**
   * Tests the createItem of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   * @throws InterruptedException
   */
  @Test
  @Order(1)
  @DisplayName("Create Item[Status:201, Endpoint: /item]")
  public void createItem201(VertxTestContext testContext) throws InterruptedException {

    var wrapper = new Object() {
      int count = 0;
    };

    fileSystem.readFile("src/test/resources/resources.json", fileRes -> {
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
   * Tests the createItem of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(2)
  @DisplayName("Create Item[Status:400, Endpoint: /item]")
  public void createItem400(VertxTestContext testContext) {

    /* open and read the entire test file mentioned in the path */
    fileSystem.readFile("src/test/resources/resourceBadBody.json", fileRes -> {
      if (fileRes.succeeded()) {

        /* mapping the file as JsonObject */
        JsonObject jsonBody = fileRes.result().toJsonObject();

        /* Send the file to the server using POST */
        client.post(PORT, HOST, BASE_URL.concat("item")).putHeader(HEADER_TOKEN, TOKEN)
            .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .sendJson(jsonBody, serverResponse -> {
              if (serverResponse.succeeded()) {

                /* comparing the response */
                assertEquals(400, serverResponse.result().statusCode());
                testContext.completeNow();
              } else if (serverResponse.failed()) {
                testContext.failed();
              }
            });
      } else if (fileRes.failed()) {
        LOGGER.error("Problem in reading test data file: ".concat(fileRes.cause().toString()));
      }
    });
  }


  /**
   * tests the updateitem of apiserververticle.
   * 
   * @param testcontext of asynchronous operations
   */
  @Test
  @Order(3)
  @DisplayName("Update Item[Status:200, Endpoint: /item]")
  void updateItem200(VertxTestContext testContext) {

    /* open and read the entire test file mentioned in the path */
    fileSystem.readFile("src/test/resources/resourceToUpdate.json", fileRes -> {
      if (fileRes.succeeded()) {

        /* mapping the file as JsonObject */
        JsonObject jsonBody = fileRes.result().toJsonObject();

        /* Send the file to the server using PUT */
        client.put(PORT, HOST, BASE_URL.concat("item")).putHeader(HEADER_TOKEN, TOKEN)
            .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .sendJson(jsonBody, serverResponse -> {
              if (serverResponse.succeeded()) {

                /* comparing the response */
                assertEquals(200, serverResponse.result().statusCode());
                assertEquals(MIME_APPLICATION_JSON,
                    serverResponse.result().getHeader("content-type"));

                testContext.completeNow();
              } else if (serverResponse.failed()) {
                testContext.failed();
              }
            });
      } else if (fileRes.failed()) {
        LOGGER.error("Problem in reading test data file: ".concat(fileRes.cause().toString()));
      }
    });
  }


  /**
   * Tests the updateItem of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(4)
  @DisplayName("Update Item[Status:400, Endpoint: /item]")
  void updateItem400(VertxTestContext testContext) {

    /* open and read the entire test file mentioned in the path */
    fileSystem.readFile("src/test/resources/resourceBadBody.json", fileRes -> {
      if (fileRes.succeeded()) {

        /* mapping the file as JsonObject */
        JsonObject jsonBody = fileRes.result().toJsonObject();

        /* Send the file to the server using PUT */
        client.put(PORT, HOST, BASE_URL.concat("item/"))
            .putHeader(HEADER_TOKEN, TOKEN).putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .sendJson(jsonBody, serverResponse -> {
              if (serverResponse.succeeded()) {

                /* comparing the response */
                assertEquals(400, serverResponse.result().statusCode());
                assertEquals(MIME_APPLICATION_JSON,
                    serverResponse.result().getHeader("content-type"));
                testContext.completeNow();

              } else if (serverResponse.failed()) {
                testContext.failed();
              }
            });
      } else if (fileRes.failed()) {
        LOGGER.error("Problem in reading test data file: ".concat(fileRes.cause().toString()));
      }
    });
  }



  /**
   * Tests the search api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   * @throws InterruptedException
   */
  @Test
  @Order(5)
  @DisplayName("Search Item[Geometry:Point, Status:200, Endpoint: /search]")
  void searchItemCircle200(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    /* Should give only one item */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam(GEORELATION, GEOREL_WITHIN).addQueryParam(MAX_DISTANCE, "5000")
        .addQueryParam(GEOMETRY, "Point").addQueryParam(COORDINATES, "[77.567829,13.091794]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            JsonObject resp = serverResponse.result().bodyAsJsonObject();
            // assertEquals(2, resp.getInteger(TOTAL_HITS));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey(TOTAL_HITS));
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }

  /**
   * Tests the create instance api
   * 
   * @param testContext of asynchronous operations
   * @throws InterruptedException
   */
  @Test
  @Order(6)
  @DisplayName("create Instance, Status:200, Endpoint: /instance]")
  void createInstance201(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    /* Should give only one item */
    client.post(PORT, HOST,
                BASE_URL.concat(INSTANCE))
        .addQueryParam(ID, "pune")
        .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON).putHeader(HEADER_TOKEN, ADMIN_TOKEN)
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {
            LOGGER.info(serverResponse.result().bodyAsString());
            /* comparing the response */
            assertEquals(201, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }

  /**
   * Tests the list instance api
   * 
   * @param testContext of asynchronous operations
   * @throws InterruptedException
   */
  @Test
  @Order(7)
  @DisplayName("list Instance, Status:200, Endpoint: /instance]")
  void listInstance200(VertxTestContext testContext) throws InterruptedException {

    Thread.sleep(5000);
    /* Send the file to the server using GET with query parameters */
    /* Should give only one item */
    client.get(PORT, HOST,
                BASE_URL.concat("list/instance"))
        .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {
            LOGGER.info(serverResponse.result().bodyAsString());
            /* comparing the response */
            JsonObject resp = serverResponse.result().bodyAsJsonObject();
            assertEquals(1, resp.getInteger(TOTAL_HITS));
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  /**
   * Tests the search api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(8)
  @DisplayName("Search Item[Geometry:Point, Status:400 invalidValue, Endpoint: /search]")
  void searchItemCircle400_1(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam(GEORELATION, GEOREL_NEAR).addQueryParam(MAX_DISTANCE, "5000")
        .addQueryParam(GEOMETRY, "Point").addQueryParam(COORDINATES, "[75.9,abc123]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(INVALID_SYNTAX,
                serverResponse.result().body().toJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  /**
   * Tests the search api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(9)
  @DisplayName("Search Item[Geometry:Point, Status:400 invalidSyntax, Endpoint: /search]")
  void searchItemCircle400_2(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(INVALID_SYNTAX, LOCATION)
        .addQueryParam(GEORELATION, GEOREL_NEAR).addQueryParam(MAX_DISTANCE, "500")
        .addQueryParam(GEOMETRY, "Point").addQueryParam(COORDINATES, "[73.85534405708313,abc123]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(INVALID_SYNTAX,
                serverResponse.result().body().toJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  /**
   * Tests the search api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(10)
  @DisplayName("Search Item[Geometry:Point, Status:400 invalidSyntax, Endpoint: /search]")
  void searchItemCircle400_3(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam("abc123", "near").addQueryParam(MAX_DISTANCE, "500")
        .addQueryParam(GEOMETRY, "Point")
        .addQueryParam(COORDINATES, "[73.85534405708313,18.52008289032131]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(INVALID_SYNTAX,
                serverResponse.result().body().toJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  /**
   * Tests the search api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(11)
  @DisplayName("Search Item[Geometry:Polygon, Status:200, Endpoint: /search]")
  void searchItemPolygon200(VertxTestContext testContext) {

    LOGGER.info("starting searchItemPolygon200");
    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam(GEORELATION, GEOREL_WITHIN).addQueryParam(MAX_DISTANCE, "5000")
        .addQueryParam(GEOMETRY, "Polygon").addQueryParam(COORDINATES,
            "[ [ [ 77.51, 12.85 ], [ 77.70, 12.95 ], [ 77.58, 13.07 ], [ 77.44, 13.01 ], [ 77.51, 12.85 ] ] ]") 
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            JsonObject resp = serverResponse.result().bodyAsJsonObject();
            assertEquals(1, resp.getInteger(TOTAL_HITS));
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  /**
   * Tests the search api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(12)
  @DisplayName("Search Item[Geometry:Polygon, Status:400 invalidValue, Endpoint: /search]")
  void searchItemPolygon400_1(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam(GEORELATION, GEOREL_WITHIN).addQueryParam(MAX_DISTANCE, "500")
        .addQueryParam(GEOMETRY, "Polygon").addQueryParam(COORDINATES,
            "[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066]"
                + ",[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],"
                + "[73.89472961425781,18.689830007518434],[73.69697570800781,abc123]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(INVALID_SYNTAX,
                serverResponse.result().body().toJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  /**
   * Tests the search api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(13)
  @DisplayName("Search Item[Geometry:Polygon, Status:400 invalidValue, Endpoint: /search]")
  void searchItemPolygon400_2(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam(GEORELATION, GEOREL_WITHIN).addQueryParam(MAX_DISTANCE, "500")
        .addQueryParam(GEOMETRY, "abc123").addQueryParam(COORDINATES,
            "[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066]"
                + ",[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],"
                + "[73.89472961425781,18.689830007518434],[73.69697570800781,18.592236436157137]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(INVALID_VALUE,
                serverResponse.result().body().toJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  /**
   * Tests the search api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(14)
  @DisplayName("Search Item[Geometry:Polygon, Status:400 invalidSyntax, Endpoint: /search]")
  void searchItemPolygon400_3(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam("invalidsyntax", LOCATION)
        .addQueryParam("abc123", GEOREL_WITHIN).addQueryParam(MAX_DISTANCE, "500")
        .addQueryParam(GEOMETRY, "Polygon").addQueryParam(COORDINATES,
            "[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066]"
                + ",[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],"
                + "[73.89472961425781,18.689830007518434],[73.69697570800781,abc123]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(INVALID_SYNTAX,
                serverResponse.result().body().toJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  /**
   * Tests the search api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(15)
  @DisplayName("Search Item[Geometry:Polygon, Status:400 invalidSyntax, Endpoint: /search]")
  void searchItemPolygon400_4(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam("abc123", GEOREL_WITHIN).addQueryParam(MAX_DISTANCE, "500")
        .addQueryParam(GEOMETRY, "Polygon").addQueryParam(COORDINATES,
            "[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066]"
                + ",[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],"
                + "[73.89472961425781,18.689830007518434],[73.69697570800781,18.592236436157137]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(INVALID_SYNTAX,
                serverResponse.result().body().toJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  /**
   * Tests the listItem api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(16)
  @DisplayName("List Item[Status:200, Endpoint: /items{itemID}]")
  void listItem200(VertxTestContext testContext) {

    String itemId =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo/aqm_test_3";

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("item/"))
        .addQueryParam(ID, itemId)
          .send(serverResponse -> {
      if (serverResponse.succeeded()) {

        LOGGER.info("Received response");
        LOGGER.info(serverResponse.result().bodyAsString());
        /* comparing the response */
        JsonObject resp = serverResponse.result().bodyAsJsonObject();
            assertEquals(1, resp.getInteger(TOTAL_HITS));
        assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(SUCCESS, serverResponse.result().body().toJsonObject().getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        testContext.failed();
      }
    });
  }


  /**
   * Tests the listItem api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(17)
  @DisplayName("List Item[Status:404, Endpoint: /items{itemID}]")
  void listItem400(VertxTestContext testContext) {

    String itemId = "abc123";

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("item/")).addQueryParam(ID, itemId)
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            JsonObject resp = serverResponse.result().bodyAsJsonObject();
            assertEquals(0, resp.getInteger(TOTAL_HITS));
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(ERROR, resp.getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  /**
   * Tests the listTags api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(18)
  @DisplayName("List tags[Status:200, Endpoint: /tags]")
  void listTags200(VertxTestContext testContext) {

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("/list/tags")).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        LOGGER.info(serverResponse.result().bodyAsString());
        /* comparing the response */
        JsonObject resp = serverResponse.result().bodyAsJsonObject();
        if (resp.getInteger(TOTAL_HITS) == 0) {
          testContext.failed();
        }
        assertEquals(200, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
        assertEquals(SUCCESS, serverResponse.result().body().toJsonObject().getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        testContext.failed();
      }
    });
  }

  /**
   * Tests the listResourceServers api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(19)
  @DisplayName("List ResourceServers[Status:200, Endpoint: /resourceservers]")
  void listResourceServers200(VertxTestContext testContext) {

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("list/resourceServer")).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        /* comparing the response */
        JsonObject resp = serverResponse.result().bodyAsJsonObject();
        assertEquals(200, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
        assertEquals(SUCCESS, resp.getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        testContext.failed();
      }
    });
  }


  /**
   * Tests the listProviders api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(20)
  @DisplayName("List Providers[Status:200, Endpoint: /providers]")
  void listProviders200(VertxTestContext testContext) {

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("/list/provider")).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        LOGGER.info(serverResponse.result().bodyAsString());
        /* comparing the response */
        JsonObject resp = serverResponse.result().bodyAsJsonObject();
        assertEquals(200, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
        assertEquals(SUCCESS, resp.getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        testContext.failed();
      }
    });
  }


  /**
   * Tests the listResourceGroups api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(21)
  @DisplayName("List ResourceGroups[Status:200, Endpoint: /resourcegroups]")
  void listResourceGroups200(VertxTestContext testContext) {

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("/list/resourceGroup")).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        LOGGER.info(serverResponse.result().bodyAsString());
        /* comparing the response */
        JsonObject resp = serverResponse.result().bodyAsJsonObject();
        assertEquals(200, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
        assertEquals(SUCCESS, resp.getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        testContext.failed();
      }
    });
  }


  /**
   * Tests the listResourceRelationship api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(22)
  @DisplayName("Search Relationship[Status:200, Endpoint: relationship/id=<resourceGroupID>&rel=resource]")
  void listResourceRelationship200(VertxTestContext testContext) {

    String resourceGroupID =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo";

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("/relationship")).addQueryParam(ID, resourceGroupID)
        .addQueryParam(REL_KEY, RESOURCE)
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            LOGGER.info(serverResponse.result().bodyAsString());
            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(SUCCESS, serverResponse.result().body().toJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  /**
   * Tests the listResourceGroupRelationship api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(23)
  @DisplayName("Search Relationship[Status:200, Endpoint: relationship?id=<resourceID>&rel=resourceGroup]")
  void listResourceGroupRelationship200(VertxTestContext testContext) {

    String resourceID =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo/Pune Railway Station_test";

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("/relationship")).addQueryParam(ID, resourceID)
        .addQueryParam(REL_KEY, RESOURCE_GRP)
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(SUCCESS, serverResponse.result().body().toJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  @Test
  @Order(24)
  @DisplayName("Single Attribute search")
  void singleAttributeSearchTest(VertxTestContext testContext) {

    LOGGER.info("singleAttributeSearchTest");

    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(PROPERTY, "[id]")
        .addQueryParam(VALUE, "[[datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/"
            + "rs.iudx.io/aqm-bosch-climo/aqm_test_4]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            JsonObject resp = serverResponse.result().bodyAsJsonObject();
            assertEquals(1, resp.getInteger(TOTAL_HITS));
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(SUCCESS, resp.getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });

  }

  @Test
  @Order(25)
  @DisplayName("Single Attribute multiple value")
  void singleAttributeMultiValueTest(VertxTestContext testContext) {

    LOGGER.info("singleAttributeMultiValueTest");

    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(PROPERTY, "[id]")
        .addQueryParam(VALUE,
            "[[datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.i"
                + "udx.io/aqm-bosch-climo/aqm_test_2,datakaveri.org/f7e044e"
                + "ee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo/aqm_test_3]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            JsonObject resp = serverResponse.result().bodyAsJsonObject();
            assertEquals(2, resp.getInteger(TOTAL_HITS));
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(SUCCESS, resp.getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  @Test
  @Order(26)
  @DisplayName("Attribute Search Invalid Syntax")
  void attributeSearchInvalidSyntax(VertxTestContext testContext) {
    String apiURL = "search?prop=[id]&val=[[existing-value]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        JsonObject resp = serverResponse.result().bodyAsJsonObject();
        assertEquals(400, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
        assertEquals(INVALID_SYNTAX, resp.getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(27)
  @DisplayName("rel search")
  void relsearch(VertxTestContext testContext) {
    String apiURL =
        "relsearch?relationship=[resourceGroup.authServerInfo.authType]&value=[[iudx-auth]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {
        
        assertEquals(200, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
        assertEquals(SUCCESS, serverResponse.result().bodyAsJsonObject().getString(STATUS));
        
        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info("status code received : " + serverResponse.result().statusCode());
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(28)
  @DisplayName("Multi Attribute search")
  void multiAttributeSearchtest(VertxTestContext testContext) {
    String apiURL =
        "search?property=[tags,deviceId]&value=[[aqm],[db3d6ea0-a84a-b3d6-7ec9-71ae66736273,climo]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        JsonObject resp = serverResponse.result().bodyAsJsonObject();
        assertEquals(1, resp.getInteger(TOTAL_HITS));
        assertEquals(200, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
        assertEquals(SUCCESS, resp.getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info("status code received : " + serverResponse.result().statusCode());
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(29)
  @DisplayName("Nested Attribute search")
  void nestedAttributeSearchtest(VertxTestContext testContext) {
    String apiURL = "search?property=[deviceModel.modelName]&value=[[Bosch-Climo]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        assertEquals(200, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
        assertEquals(SUCCESS, serverResponse.result().bodyAsJsonObject().getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info("status code received : " + serverResponse.result().statusCode());
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(30)
  @DisplayName("bbox search")
  void bboxSearchtest(VertxTestContext testContext) {
    String apiURL =
        "search?geoproperty=location&georel=within&geometry=bbox&coordinates=[[77.567829,18.528311],[73.874537,18.528311]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        assertEquals(200, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
        assertEquals(SUCCESS, serverResponse.result().bodyAsJsonObject().getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info("status code received : " + serverResponse.result().statusCode());
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(31)
  @DisplayName("LineString search")
  void LineStringSearchtest(VertxTestContext testContext) {
    String apiURL = "search?geoproperty=location&georel=intersects&geometry=LineString&coordinates"
        + "=[[73.874537,18.528311],[73.836808,18.572797],[73.876484,18.525007]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        assertEquals(200, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
        assertEquals(SUCCESS, serverResponse.result().bodyAsJsonObject().getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info("status code received : " + serverResponse.result().statusCode());
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(32)
  @DisplayName("Invalid Geometry search")
  void invalidGeometrySearchtest(VertxTestContext testContext) {
    String apiURL =
        "search?geoproperty=location&georel=within&geometry=abc123&coordinates=[[[73.696975"
            + "70800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        assertEquals(400, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
        assertEquals(INVALID_VALUE, serverResponse.result().bodyAsJsonObject().getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info("status code received : " + serverResponse.result().statusCode());
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(33)
  @DisplayName("Invalid Georel search")
  void invalidGeorelSearchtest(VertxTestContext testContext) {
    String apiURL =
        "search?geoproperty=location&georel=abc123&geometry=LineString&coordinates=[[[73.696975"
            + "70800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        assertEquals(400, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
        assertEquals(INVALID_VALUE, serverResponse.result().bodyAsJsonObject().getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info("status code received : " + serverResponse.result().statusCode());
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(34)
  @DisplayName("Invalid coordinate search")
  void invalidCoordinateSearchtest(VertxTestContext testContext) {
    String apiURL =
        "search?geoproperty=location&georel=within&geometry=bbox&coordinates=[[[73.69697570"
            + "800781,18.592236436157137],[73.69697570800781,abc123]]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        assertEquals(400, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
        assertEquals(INVALID_SYNTAX, serverResponse.result().bodyAsJsonObject().getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info("status code received : " + serverResponse.result().statusCode());
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(35)
  @DisplayName("Geo Spatial Search Invalid Syntax")
  void geoSpatialInvalidSyntax(VertxTestContext testContext) {
    String apiURL =
        "search?invalidsyntax=location&abc123=within&geometry=bbox&coordinates=[[[73.6969757"
            + "0800781,18.592236436157137],[73.69697570800781,abc123]]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        assertEquals(400, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
        assertEquals(INVALID_SYNTAX, serverResponse.result().bodyAsJsonObject().getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info("status code received : " + serverResponse.result().statusCode());
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(36)
  @DisplayName("Text Search")
  void textSearchTest(VertxTestContext testContext) {
    /* Encoded whitespaces to get appropriate response */
    String apiURL = "search?q=\"climo\"";

    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        assertEquals(200, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
        assertEquals(SUCCESS, serverResponse.result().bodyAsJsonObject().getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info("status code received : " + serverResponse.result().statusCode());
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(37)
  @DisplayName("Text Search with *")
  void textSearchAcceptableSpecialCharTest(VertxTestContext testContext) {
    /* Encoded whitespaces to get appropriate response */

    String apiURL = "search?q=\"climo*\"";

    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        assertEquals(200, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
        assertEquals(SUCCESS, serverResponse.result().bodyAsJsonObject().getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info("status code received : " + serverResponse.result().statusCode());
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(38)
  @DisplayName("Special Characters Text Search")
  void specialCharactersTextSearchTest(VertxTestContext testContext) {
    /* Encoded characters to get appropriate response */
    String apiURL = "search?q=\"@!$%432\"&limit=50&offset=100";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        assertEquals(400, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
        assertEquals(INVALID_SYNTAX, serverResponse.result().bodyAsJsonObject().getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info("status code received : " + serverResponse.result().statusCode());
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(39)
  @DisplayName("Text Search Invalid Syntax")
  void textSearchInvalidSyntaxTest(VertxTestContext testContext) {
    /* Encoded whitespaces to get appropriate response */
    String apiURL = "search?abc123=\"text%20to%20search\"&limit=50&offset=100";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        assertEquals(400, serverResponse.result().statusCode());
        assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
        assertEquals(INVALID_SYNTAX, serverResponse.result().bodyAsJsonObject().getString(STATUS));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        LOGGER.info("status code received : " + serverResponse.result().statusCode());
        LOGGER.info(serverResponse.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(40)
  @DisplayName("Get Provider")
  void getProviderTest(VertxTestContext testContext) {
    String apiURL =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo/aqm_test_2";

    client.get(PORT, HOST, BASE_URL.concat(RELATIONSHIP)).addQueryParam(ID, apiURL)
        .addQueryParam(REL_KEY, PROVIDER).send(serverResponse -> {
          if (serverResponse.succeeded()) {

            JsonObject resp = serverResponse.result().bodyAsJsonObject();
            assertEquals(1, resp.getInteger(TOTAL_HITS));
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(SUCCESS, resp.getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info("status code received : " + serverResponse.result().statusCode());
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  @Test
  @Order(41)
  @DisplayName("Get resourceServer")
  void getResourceServerTest(VertxTestContext testContext) {
    String apiURL =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo/aqm_test_2";

    client.get(PORT, HOST, BASE_URL.concat(RELATIONSHIP)).addQueryParam(ID, apiURL)
        .addQueryParam(REL_KEY, RESOURCE_SVR).send(serverResponse -> {
          if (serverResponse.succeeded()) {

            JsonObject resp = serverResponse.result().bodyAsJsonObject();
            assertEquals(1, resp.getInteger(TOTAL_HITS));
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(SUCCESS, resp.getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info("status code received : " + serverResponse.result().statusCode());
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  @Test
  @Order(42)
  @DisplayName("Get data model [type]")
  void getDataModelTest(VertxTestContext testContext) {
    String apiURL =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo/aqm_test_2";

    client.get(PORT, HOST, BASE_URL.concat(RELATIONSHIP)).addQueryParam(ID, apiURL)
        .addQueryParam(REL_KEY, TYPE).send(serverResponse -> {
          if (serverResponse.succeeded()) {

            JsonObject resp = serverResponse.result().bodyAsJsonObject();
            assertEquals(1, resp.getInteger(TOTAL_HITS));
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(SUCCESS, resp.getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info("status code received : " + serverResponse.result().statusCode());
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(43)
  @DisplayName("Count Item[Geometry:Point, Status:200, Endpoint: /count]")
  void countItemCircle200(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    /* Should give only one item */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam(GEORELATION, INTERSECTS).addQueryParam(MAX_DISTANCE, "5000")
        .addQueryParam(GEOMETRY, "Point").addQueryParam(COORDINATES, "[77.567829,13.091794]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            JsonObject resp = serverResponse.result().bodyAsJsonObject();
            assertTrue(serverResponse.result().body().toJsonObject().containsKey(TOTAL_HITS));
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(SUCCESS, resp.getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(44)
  @DisplayName("count Item[Geometry:Polygon, Status:200, Endpoint: /count]")
  void countItemPolygon200(VertxTestContext testContext) {

    LOGGER.info("starting countItemPolygon200");
    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam(GEORELATION, GEOREL_WITHIN).addQueryParam(MAX_DISTANCE, "5000")
        .addQueryParam(GEOMETRY, "Polygon").addQueryParam(COORDINATES,
            "[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066]"
                + ",[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],"
                + "[73.89472961425781,18.689830007518434],[73.69697570800781,18.592236436157137]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            JsonObject resp = serverResponse.result().bodyAsJsonObject();
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(SUCCESS, resp.getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(45)
  @DisplayName("Count Attribute[Status:200, Endpoint: /count]")
  void countAttribute200(VertxTestContext testContext) {

    LOGGER.info("starting countAttribute200");

    String id =
        "[[datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo/aqm_test_5]]";
    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count")).addQueryParam(PROPERTY, "[id]")
        .addQueryParam(VALUE, id).send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            JsonObject resp = serverResponse.result().bodyAsJsonObject();
            assertEquals(1, resp.getInteger(TOTAL_HITS));
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(SUCCESS, resp.getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(46)
  @DisplayName("Count SingleAttr multiValue[Status:200, Endpoint: /count]")
  void countAttributeMultiValue200(VertxTestContext testContext) {

    LOGGER.info("starting countAttributeMultiValue200");

    String id1 =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo/aqm_test_4";
    String id2 =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo/aqm_test_3";

    String id = "[[" + id1 + "," + id2 + "]]";

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count")).addQueryParam(PROPERTY, "[id]")
        .addQueryParam(VALUE, id).send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            JsonObject resp = serverResponse.result().bodyAsJsonObject();
            assertEquals(2, resp.getInteger(TOTAL_HITS));
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertEquals(SUCCESS, resp.getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(47)
  @DisplayName("Count Attribute[Status:400 invalidSyntax, Endpoint: /count]")
  void countAttribute400(VertxTestContext testContext) {

    LOGGER.info("starting countAttribute400");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count")).addQueryParam("prop", "[id]")
        .addQueryParam(VALUE, "[[existing-value]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
            assertEquals(INVALID_SYNTAX, serverResponse.result().bodyAsJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(48)
  @DisplayName("Count multiAttr[Status:200, Endpoint: /count]")
  void countMultiAttribute200(VertxTestContext testContext) {

    LOGGER.info("starting countMultiAttribute200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count"))
        .addQueryParam(PROPERTY, "[itemStatus,deviceId]")
        .addQueryParam(VALUE, "[[ACTIVE, INACTIVE],[b3ec32ff-fa7d-64fa-c0af-272e25d314e9test_2]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey(TOTAL_HITS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(49)
  @DisplayName("Count nestedAttr[Status:200, Endpoint: /count]")
  void countNestedAttribute200(VertxTestContext testContext) {

    LOGGER.info("starting countNestedAttribute200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count"))
        .addQueryParam(PROPERTY, "[deviceModelInfo.name]").addQueryParam(VALUE, "[[Bosch-Climo]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey(TOTAL_HITS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(50)
  @DisplayName("Count bbox[Status:200, Endpoint: /count]")
  void countBbox200(VertxTestContext testContext) {

    LOGGER.info("starting countBbox200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam(GEORELATION, GEOREL_WITHIN).addQueryParam(MAX_DISTANCE, "5000")
        .addQueryParam(GEOMETRY, BBOX).addQueryParam(COORDINATES,
            "[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey(TOTAL_HITS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(51)
  @DisplayName("Count LineString[Status:200, Endpoint: /count]")
  void countLineString200(VertxTestContext testContext) {

    LOGGER.info("starting countLineString200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam(GEORELATION, INTERSECTS).addQueryParam(GEOMETRY, LINE_STRING)
        .addQueryParam(COORDINATES,
            "[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137],[73.876484,18.525007]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey(TOTAL_HITS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(52)
  @DisplayName("count geometry[Status:400 invalidValue, Endpoint: /count]")
  void countGeometry400(VertxTestContext testContext) {

    LOGGER.info("starting countGeometry400");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam(GEORELATION, GEOREL_WITHIN).addQueryParam(GEOMETRY, "abc123")
        .addQueryParam(COORDINATES,
            "[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON,
                serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
            assertEquals(INVALID_VALUE,
                serverResponse.result().bodyAsJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info("status code received : " + serverResponse.result().statusCode());
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(53)
  @DisplayName("Count georel[Status:400 invalidValue, Endpoint: /count]")
  void countGeorel400(VertxTestContext testContext) {

    LOGGER.info("starting countGeorel400");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam(GEORELATION, "abc123").addQueryParam(GEOMETRY, LINE_STRING)
        .addQueryParam(COORDINATES,
            "[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON,
                serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
            assertEquals(INVALID_VALUE,
                serverResponse.result().bodyAsJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(54)
  @DisplayName("Count geoSpatial[Status:400 invalidSyntax, Endpoint: /count]")
  void countGeoSpatial400(VertxTestContext testContext) {

    LOGGER.info("starting countGeoSpatial400");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("invalidsyntax", LOCATION)
        .addQueryParam(GEORELATION, GEOREL_WITHIN).addQueryParam(GEOMETRY, BBOX)
        .addQueryParam(COORDINATES,
            "[[[73.69697570800781,18.592236436157137],[73.69697570800781,abc123]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON,
                serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
            assertEquals(INVALID_SYNTAX,
                serverResponse.result().bodyAsJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(55)
  @DisplayName("Count text[Status:200, Endpoint: /count]")
  void countText200(VertxTestContext testContext) {

    LOGGER.info("starting countText200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam(Q_VALUE, "\"climo\"")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey(TOTAL_HITS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info("status code received : " + serverResponse.result().statusCode());
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(56)
  @DisplayName("Count text*[Status:200, Endpoint: /count]")
  void countTextAcceptableSpecialChar200(VertxTestContext testContext) {

    LOGGER.info("starting countTextAcceptableSpecialChar200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam(Q_VALUE, "\"climo*\"")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey(TOTAL_HITS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(57)
  @DisplayName("Count text SpecialChar[Status:400, Endpoint: /count]")
  void countSpecialCharactersText400(VertxTestContext testContext) {

    LOGGER.info("starting countSpecialCharactersText400");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("q", "\"@!$%432\"")
        .addQueryParam(LIMIT, "50").addQueryParam(OFFSET, "100").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON,
                serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
            assertEquals(INVALID_SYNTAX,
                serverResponse.result().bodyAsJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(58)
  @DisplayName("count text[Status:400 invalidSyntax, Endpoint: /count]")
  void countText400(VertxTestContext testContext) {

    LOGGER.info("starting countText400 invalidSyntax");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("abc123", "\"text to count\"")
        .addQueryParam(LIMIT, "50").addQueryParam(OFFSET, "100").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON,
                serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
            assertEquals(INVALID_SYNTAX,
                serverResponse.result().bodyAsJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(59)
  @DisplayName("Count Item[Geometry:Polygon, Status:400 invalidSyntax, Endpoint: /count]")
  void countItemPolygon400_1(VertxTestContext testContext) {

    LOGGER.info("starting countItemPolygon400 invalidSyntax");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam(GEORELATION, GEOREL_WITHIN).addQueryParam(MAX_DISTANCE, "500")
        .addQueryParam(GEOMETRY, "Polygon").addQueryParam(COORDINATES,
            "[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066]"
                + ",[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],"
                + "[73.89472961425781,18.689830007518434],[73.69697570800781,abc123]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON,
                serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
            assertEquals(INVALID_SYNTAX,
                serverResponse.result().bodyAsJsonObject().getString(STATUS));


            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }

  /**
   * Tests the count api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(60)
  @DisplayName("Count coordinates[Status:400 invalidSyntax, Endpoint: /count]")
  void countCoordinate400(VertxTestContext testContext) {

    LOGGER.info("starting countGeorel400");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam(GEOPROPERTY, LOCATION)
        .addQueryParam(GEORELATION, GEOREL_WITHIN).addQueryParam(GEOMETRY, LINE_STRING)
        .addQueryParam(COORDINATES,
            "[[[73.69697570800781,18.592236436157137],[73.69697570800781,abc123]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON,
                serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
            assertEquals(INVALID_SYNTAX,
                serverResponse.result().bodyAsJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the search handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(61)
  @DisplayName("Tag search[Status:200, Endpoint: /search]")
  void singleTagSearch200(VertxTestContext testContext) {

    LOGGER.info("starting singleTagSearch200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(PROPERTY, "[tags]")
        .addQueryParam(VALUE, "[[pollution]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey(TOTAL_HITS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }


  /**
   * Tests the search handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(62)
  @DisplayName("Tag search[Status:200, Endpoint: /search]")
  void singleTagSearchWithFilter200(VertxTestContext testContext) {

    LOGGER.info("starting singleTagSearchWithFilter200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(PROPERTY, "[tags]")
        .addQueryParam(VALUE, "[[pollution]]").addQueryParam(FILTER, "[id,tags]")
        .addQueryParam(OFFSET, "0").addQueryParam(LIMIT, "1").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey(TOTAL_HITS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the search handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(63)
  @DisplayName("Tag search[Status:400, Endpoint: /search]")
  void singleTagSearch400_1(VertxTestContext testContext) {

    LOGGER.info("starting singleTagSearch400_1");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(PROPERTY, "[tags]")
        .addQueryParam(VALUE, "[[abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(0, serverResponse.result().bodyAsJsonObject().getInteger(TOTAL_HITS));
            assertEquals(MIME_APPLICATION_JSON,
                serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
            assertEquals(SUCCESS,
                serverResponse.result().bodyAsJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the search handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(64)
  @DisplayName("Tag search[Status:400, Endpoint: /search]")
  void singleTagSearch400_2(VertxTestContext testContext) {

    LOGGER.info("starting singleTagSearch400_2");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(PROPERTY, "[abc123]")
        .addQueryParam(VALUE, "[[abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(0, serverResponse.result().bodyAsJsonObject().getInteger(TOTAL_HITS));
            assertEquals(MIME_APPLICATION_JSON,
                serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
            assertEquals(SUCCESS, serverResponse.result().bodyAsJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the search handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(65)
  @DisplayName("Tag search[Status:400, Endpoint: /search]")
  void singleTagSearch400_3(VertxTestContext testContext) {

    LOGGER.info("starting singleTagSearch400_3");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam("invalidProperty", "[abc123]")
        .addQueryParam(VALUE, "[[abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON,
                serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
            assertEquals(INVALID_SYNTAX,
                serverResponse.result().bodyAsJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the search handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(66)
  @DisplayName("Tag MultiSearch[Status:200, Endpoint: /search]")
  void multiTagSearch200(VertxTestContext testContext) {

    LOGGER.info("starting multiTagSearch200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(PROPERTY, "[tags]")
        .addQueryParam(VALUE, "[[pollution,flood]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey(TOTAL_HITS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the search handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(67)
  @DisplayName("Tag MultiSearch[Status:400, Endpoint: /search]")
  void multiTagSearch200_2(VertxTestContext testContext) {

    LOGGER.info("starting multiTagSearch400_1");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(PROPERTY, "[tags]")
        .addQueryParam(VALUE, "[[abc, abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(0, serverResponse.result().bodyAsJsonObject().getInteger(TOTAL_HITS));
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the search handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(68)
  @DisplayName("Tag MultiSearch[Status:400, Endpoint: /search]")
  void multiTagSearch200_3(VertxTestContext testContext) {

    LOGGER.info("starting multiTagSearch400_2");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(PROPERTY, "[abc123]")
        .addQueryParam(VALUE, "[[abc, abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(0, serverResponse.result().bodyAsJsonObject().getInteger(TOTAL_HITS));
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the search handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(69)
  @DisplayName("Tag MultiSearch[Status:400, Endpoint: /search]")
  void multiTagSearch400_3(VertxTestContext testContext) {

    LOGGER.info("starting singleTagSearch400_3");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam("invalidProperty", "[abc123]")
        .addQueryParam(VALUE, "[[abc, abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON,
                serverResponse.result().getHeader(HEADER_CONTENT_TYPE));
            assertEquals(INVALID_SYNTAX,
                serverResponse.result().bodyAsJsonObject().getString(STATUS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }

  /**
   * Tests the search handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(70)
  @DisplayName("Tag MultiSearch[Status:200, Endpoint: /search]")
  void multiTagSearchPartial200(VertxTestContext testContext) {

    LOGGER.info("starting multiTagSearch200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam(PROPERTY, "[tags]")
        .addQueryParam(VALUE, "[[pollution,abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals(MIME_APPLICATION_JSON, serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey(TOTAL_HITS));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            LOGGER.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }
}
