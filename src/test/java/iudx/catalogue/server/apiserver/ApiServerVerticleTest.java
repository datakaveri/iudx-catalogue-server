package iudx.catalogue.server.apiserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Iterator;

/**
 * Test class for ApiServerVerticle api handlers TODO Need to update count test cases. TODO Update
 * all the end to end test cases. TODO Use Constants file for all query strings.
 * 
 * @see {@link ApiServerVerticle}
 */
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiServerVerticleTest {

  /* logger instance */
  private static final Logger logger = LoggerFactory.getLogger(ApiServerVerticleTest.class);
  private static final String HOST = "127.0.0.1";
  private static final int PORT = 8443;
  private static final String BASE_URL = "/iudx/cat/v1/";

  /** Token for crud apis */
  private static final String TOKEN = "";

  private static WebClient client;
  private static FileSystem fileSytem;

  ApiServerVerticleTest() {}

  /**
   * Starting the Catalogue-Server in clustered mode, before the execution of tests
   * 
   * @param testContext of asynchronous operations
   * @param vertx initializing the core vertx apis
   * @throws InterruptedException generated when a thread is interrupted
   */
  @BeforeAll
  @DisplayName("Deploy a apiserver")
  static void startVertx(VertxTestContext testContext, Vertx vertx) throws InterruptedException {

    /* Options for the web client connections */

    JksOptions options = new JksOptions().setPath("keystore.jks").setPassword("password");

    WebClientOptions clientOptions = new WebClientOptions().setSsl(true).setVerifyHost(false)
        .setTrustAll(true).setTrustStoreOptions(options);
    fileSytem = vertx.fileSystem();
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
    // logger.info("!!!!!!!!\n\n!!!!!!!!!");
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

    fileSytem.readFile("src/test/resources/resources.json", fileRes -> {
      if (fileRes.succeeded()) {

        JsonArray resources = fileRes.result().toJsonArray();
        int numItems = resources.size();
        logger.info("Total items = " + String.valueOf(resources.size()));
        Iterator<Object> objectIterator = resources.iterator();


        while (objectIterator.hasNext()) {
          // Send the file to the server using POST
          client.post(PORT, HOST, BASE_URL.concat("item")).putHeader("token", TOKEN)
              .putHeader("Content-Type", "application/json")
              .sendJson(objectIterator.next(), serverResponse -> {
                if (serverResponse.succeeded()) {
                  if (serverResponse.result().statusCode() == 201) {
                    wrapper.count++;
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
        logger.error("Problem in reading test data file: ".concat(fileRes.cause().toString()));
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
    fileSytem.readFile("src/test/resources/resourceBadBody.json", fileRes -> {
      if (fileRes.succeeded()) {

        /* mapping the file as JsonObject */
        JsonObject jsonBody = fileRes.result().toJsonObject();


        /* Send the file to the server using POST */
        client.post(PORT, HOST, BASE_URL.concat("item")).putHeader("token", TOKEN)
            .putHeader("Content-Type", "application/json").sendJson(jsonBody, serverResponse -> {
              if (serverResponse.succeeded()) {

                /* comparing the response */
                assertEquals(400, serverResponse.result().statusCode());
                testContext.completeNow();
              } else if (serverResponse.failed()) {
                testContext.failed();
              }
            });
      } else if (fileRes.failed()) {
        logger.error("Problem in reading test data file: ".concat(fileRes.cause().toString()));
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
    fileSytem.readFile("src/test/resources/resourceToUpdate.json", fileRes -> {
      if (fileRes.succeeded()) {

        /* mapping the file as JsonObject */
        JsonObject jsonBody = fileRes.result().toJsonObject();

        /* Send the file to the server using PUT */
        client.put(PORT, HOST, BASE_URL.concat("item")).putHeader("token", TOKEN)
            .putHeader("Content-Type", "application/json").sendJson(jsonBody, serverResponse -> {
              if (serverResponse.succeeded()) {

                /* comparing the response */
                assertEquals(200, serverResponse.result().statusCode());
                assertEquals("application/json", serverResponse.result().getHeader("content-type"));

                testContext.completeNow();
              } else if (serverResponse.failed()) {
                testContext.failed();
              }
            });
      } else if (fileRes.failed()) {
        logger.error("Problem in reading test data file: ".concat(fileRes.cause().toString()));
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
    fileSytem.readFile("src/test/resources/resourceBadBody.json", fileRes -> {
      if (fileRes.succeeded()) {

        /* mapping the file as JsonObject */
        JsonObject jsonBody = fileRes.result().toJsonObject();

        /* Send the file to the server using PUT */
        client.put(PORT, HOST, BASE_URL.concat("item/").concat(jsonBody.getString("id")))
            .putHeader("token", TOKEN).putHeader("Content-Type", "application/json")
            .sendJson(jsonBody, serverResponse -> {
              if (serverResponse.succeeded()) {

                /* comparing the response */
                assertEquals(400, serverResponse.result().statusCode());
                assertEquals("application/json", serverResponse.result().getHeader("content-type"));

                testContext.completeNow();
              } else if (serverResponse.failed()) {
                testContext.failed();
              }
            });
      } else if (fileRes.failed()) {
        logger.error("Problem in reading test data file: ".concat(fileRes.cause().toString()));
      }
    });
  }


  /**
   * Tests the deleteItem of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(5)
  @DisplayName("Delete Item[Status:200, Endpoint: /item]")
  void deleteItem200(VertxTestContext testContext) {

    var wrapper = new Object() {
      int count = 0;
    };

    fileSytem.readFile("src/test/resources/resourcesToDelete.json", fileRes -> {
      if (fileRes.succeeded()) {
        JsonArray resources = fileRes.result().toJsonArray();
        Iterator<Object> objectIterator = resources.iterator();
        int numItems = resources.size();

        while (objectIterator.hasNext()) {

          JsonObject item = (JsonObject) objectIterator.next();
          /* Send the file to the server using DELETE */
          logger.info("Deleting " + item.getString("id"));
          client.delete(PORT, HOST, BASE_URL.concat("item/"))
              .addQueryParam("id", item.getString("id")).putHeader("token", TOKEN)
              .putHeader("Content-Type", "application/json").send(serverResponse -> {
                if (serverResponse.succeeded()) {

                  /* comparing the response */
                  assertEquals(200, serverResponse.result().statusCode());
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
      }
    });
  }


  /**
   * Tests the search api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(6)
  @DisplayName("Search Item[Geometry:Point, Status:200, Endpoint: /search]")
  void searchItemCircle200(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    /* Should give only one item */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "intersects").addQueryParam("maxDistance", "5")
        .addQueryParam("geometry", "Point").addQueryParam("coordinates", "[ 73.874537, 18.528311 ]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));

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
   */
  @Test
  @Order(6)
  @DisplayName("create Instance, Status:200, Endpoint: /instance]")
  void createInstance201(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    /* Should give only one item */
    client.post(PORT, HOST,
                BASE_URL.concat("instance"))
                        .addQueryParam("id", "someTestInstance")
                        .putHeader("Content-Type", "application/json")
                        .putHeader("token", TOKEN)
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {
            logger.info(serverResponse.result().bodyAsString());
            /* comparing the response */
            assertEquals(201, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
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
   */
  @Test
  @Order(6)
  @DisplayName("list Instance, Status:200, Endpoint: /instance]")
  void listInstance200(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    /* Should give only one item */
    client.get(PORT, HOST,
                BASE_URL.concat("list/instance"))
                        .putHeader("Content-Type", "application/json")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {
            logger.info(serverResponse.result().bodyAsString());
            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
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
   */
  @Test
  @Order(6)
  @DisplayName("delete Instance, Status:200, Endpoint: /instance]")
  void deleteInstance200(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    /* Should give only one item */
    client.delete(PORT, HOST,
                BASE_URL.concat("instance"))
                        .addQueryParam("id", "someTestInstance")
                        .putHeader("Content-Type", "application/json")
                        .putHeader("token", TOKEN)
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {
            logger.info(serverResponse.result().bodyAsString());
            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
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
  @Order(7)
  @DisplayName("Search Item[Geometry:Point, Status:400 invalidValue, Endpoint: /search]")
  void searchItemCircle400_1(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "near").addQueryParam("maxDistance", "5000")
        .addQueryParam("geometry", "Point").addQueryParam("coordinates", "[75.9,abc123]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertEquals("invalidValue",
                serverResponse.result().body().toJsonObject().getString("status"));

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
  @DisplayName("Search Item[Geometry:Point, Status:400 invalidSyntax, Endpoint: /search]")
  void searchItemCircle400_2(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("invalidsyntax", "location")
        .addQueryParam("georel", "near").addQueryParam("maxDistance", "500")
        .addQueryParam("geometry", "Point")
        .addQueryParam("coordinates", "[73.85534405708313,abc123]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertEquals("invalidSyntax",
                serverResponse.result().body().toJsonObject().getString("status"));

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
  void searchItemCircle400_3(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("geoproperty", "location")
        .addQueryParam("abc123", "near").addQueryParam("maxDistance", "500")
        .addQueryParam("geometry", "Point")
        .addQueryParam("coordinates", "[73.85534405708313,18.52008289032131]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertEquals("invalidSyntax",
                serverResponse.result().body().toJsonObject().getString("status"));

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
  @DisplayName("Search Item[Geometry:Polygon, Status:200, Endpoint: /search]")
  void searchItemPolygon200(VertxTestContext testContext) {

    logger.info(
        "starting searchItemPolygon200 !!!!!!!!@@@@@@@@##############$$$$$$$$$$$$%%%%%%%%%%%%");
    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "within").addQueryParam("maxDistance", "500")
        .addQueryParam("geometry", "Polygon")
        .addQueryParam("coordinates",
            "[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066]"
                + ",[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],"
                + "[73.89472961425781,18.689830007518434],[73.69697570800781,18.592236436157137]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));

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
  @DisplayName("Search Item[Geometry:Polygon, Status:400 invalidValue, Endpoint: /search]")
  void searchItemPolygon400_1(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "within").addQueryParam("maxDistance", "500")
        .addQueryParam("geometry", "Polygon")
        .addQueryParam("coordinates",
            "[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066]"
                + ",[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],"
                + "[73.89472961425781,18.689830007518434],[73.69697570800781,abc123]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertEquals("invalidValue",
                serverResponse.result().body().toJsonObject().getString("status"));

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
  void searchItemPolygon400_2(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "within").addQueryParam("maxDistance", "500")
        .addQueryParam("geometry", "abc123")
        .addQueryParam("coordinates",
            "[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066]"
                + ",[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],"
                + "[73.89472961425781,18.689830007518434],[73.69697570800781,18.592236436157137]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertEquals("invalidValue",
                serverResponse.result().body().toJsonObject().getString("status"));

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
  @DisplayName("Search Item[Geometry:Polygon, Status:400 invalidSyntax, Endpoint: /search]")
  void searchItemPolygon400_3(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("invalidsyntax", "location")
        .addQueryParam("abc123", "within").addQueryParam("maxDistance", "500")
        .addQueryParam("geometry", "Polygon")
        .addQueryParam("coordinates",
            "[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066]"
                + ",[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],"
                + "[73.89472961425781,18.689830007518434],[73.69697570800781,abc123]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertEquals("invalidSyntax",
                serverResponse.result().body().toJsonObject().getString("status"));

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
  void searchItemPolygon400_4(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("geoproperty", "location")
        .addQueryParam("abc123", "within").addQueryParam("maxDistance", "500")
        .addQueryParam("geometry", "Polygon")
        .addQueryParam("coordinates",
            "[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066]"
                + ",[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],"
                + "[73.89472961425781,18.689830007518434],[73.69697570800781,18.592236436157137]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertEquals("invalidSyntax",
                serverResponse.result().body().toJsonObject().getString("status"));

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
  @Order(15)
  @DisplayName("List Item[Status:200, Endpoint: /items{itemID}]")
  void listItem200(VertxTestContext testContext) {

    String itemId = "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/aqm-bosch-climo/Hadapsar_Gadital_01";

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("item/"))
          .addQueryParam("id", itemId)
          .send(serverResponse -> {
      if (serverResponse.succeeded()) {

        logger.info("Received response");
        logger.info(serverResponse.result().bodyAsString());
        /* comparing the response */
        assertEquals(200, serverResponse.result().statusCode());
        assertEquals("application/json", serverResponse.result().getHeader("content-type"));
        assertEquals("success", serverResponse.result().body().toJsonObject().getString("status"));

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
  @DisplayName("List Item[Status:404, Endpoint: /items{itemID}]")
  void listItem400(VertxTestContext testContext) {

    String itemId = "abc123";

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("items/").concat(itemId)).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        /* comparing the response */
        assertEquals(404, serverResponse.result().statusCode());

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
  @Order(17)
  @DisplayName("List tags[Status:200, Endpoint: /tags]")
  void listTags200(VertxTestContext testContext) {

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("/list/tags")).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        logger.info(serverResponse.result().bodyAsString());
        /* comparing the response */
        assertEquals(200, serverResponse.result().statusCode());
        assertEquals("application/json", serverResponse.result().getHeader("content-type"));
        assertEquals("success", serverResponse.result().body().toJsonObject().getString("status"));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        testContext.failed();
      }
    });
  }


  /**
   * Tests the listDomains api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(18)
  @DisplayName("List domains[Status:200, Endpoint: /domains]")
  void listDomains200(VertxTestContext testContext) {

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("domains")).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        /* comparing the response */
        assertEquals(200, serverResponse.result().statusCode());
        assertEquals("application/json", serverResponse.result().getHeader("content-type"));
        assertEquals("success", serverResponse.result().body().toJsonObject().getString("status"));

        testContext.completeNow();
      } else if (serverResponse.failed()) {
        testContext.failed();
      }
    });
  }


  /**
   * Tests the listCities api handler of ApiServerVerticle.
   * 
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(19)
  @DisplayName("List cities[Status:200, Endpoint: /cities]")
  void listCities200(VertxTestContext testContext) {

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("cities")).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        /* comparing the response */
        assertEquals(200, serverResponse.result().statusCode());
        assertEquals("application/json", serverResponse.result().getHeader("content-type"));
        assertEquals("success", serverResponse.result().body().toJsonObject().getString("status"));

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
  @Order(20)
  @DisplayName("List ResourceServers[Status:200, Endpoint: /resourceservers]")
  void listResourceServers200(VertxTestContext testContext) {

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("resourceservers")).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        /* comparing the response */
        assertEquals(200, serverResponse.result().statusCode());
        assertEquals("application/json", serverResponse.result().getHeader("content-type"));
        assertEquals("success", serverResponse.result().body().toJsonObject().getString("status"));

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
  @Order(21)
  @DisplayName("List Providers[Status:200, Endpoint: /providers]")
  void listProviders200(VertxTestContext testContext) {

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("/list/provider")).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        logger.info(serverResponse.result().bodyAsString());
        /* comparing the response */
        assertEquals(200, serverResponse.result().statusCode());
        assertEquals("application/json", serverResponse.result().getHeader("content-type"));
        assertEquals("success", serverResponse.result().body().toJsonObject().getString("status"));

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
  @Order(22)
  @DisplayName("List ResourceGroups[Status:200, Endpoint: /resourcegroups]")
  void listResourceGroups200(VertxTestContext testContext) {

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("/list/resourceGroup")).send(serverResponse -> {
      if (serverResponse.succeeded()) {

        logger.info(serverResponse.result().bodyAsString());
        /* comparing the response */
        assertEquals(200, serverResponse.result().statusCode());
        assertEquals("application/json", serverResponse.result().getHeader("content-type"));
        assertEquals("success", serverResponse.result().body().toJsonObject().getString("status"));

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
  @Order(23)
  @DisplayName("Search Relationship[Status:200, Endpoint: /<resourceGroupID>/resource]")
  void listResourceRelationship200(VertxTestContext testContext) {

    String resourceGroupID =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/aqm-bosch-climo";

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat(resourceGroupID).concat("/resource"))
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            logger.info(serverResponse.result().bodyAsString());
            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertEquals("success",
                serverResponse.result().body().toJsonObject().getString("status"));

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
  @Order(24)
  @DisplayName("Search Relationship[Status:200, Endpoint: /<resourceID>/resourceGroup]")
  void listResourceGroupRelationship200(VertxTestContext testContext) {

    String resourceID =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/aqm-bosch-climo/Sadhu_Wasvani_Square_24";

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat(resourceID).concat("/resourceGroup"))
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertEquals("success",
                serverResponse.result().body().toJsonObject().getString("status"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            testContext.failed();
          }
        });
  }


  @Test
  @Order(25)
  @DisplayName("Single Attribute search")
  void singleAttributeSearchTest(VertxTestContext testContext) {

    logger.info("singleAttributeSearchTest");

    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam("property", "[id]")
        .addQueryParam("value", "[[datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/"
            + "rs.iudx.org.in/aqm-bosch-climo/Ambedkar society circle_29]]")
        .send(ar -> {
          if (ar.succeeded()) {
            assertEquals(200, ar.result().statusCode());
            testContext.completeNow();
          } else if (ar.failed()) {
            logger.info(ar.cause());
            testContext.failed();
          }
        });

  }

  @Test
  @Order(26)
  @DisplayName("Single Attribute multiple value")
  void singleAttributeMultiValueTest(VertxTestContext testContext) {

    logger.info("singleAttributeMultiValueTest");

    client.get(PORT, HOST, BASE_URL.concat("search")).addQueryParam("property", "[id]")
        .addQueryParam("value", "[[datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.i"
            + "udx.org.in/aqm-bosch-climo/Ambedkar society circle_29,datakaveri.org/f7e044e"
            + "ee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/aqm-bosch-climo/Dr Baba Saheb Ambedkar Sethu Junction_3]]")
        .send(ar -> {
          if (ar.succeeded()) {
            assertEquals(200, ar.result().statusCode());
            testContext.completeNow();
          } else if (ar.failed()) {
            logger.info(ar.cause());
            testContext.failed();
          }
        });
  }

  /*
   * @Test
   * 
   * @Order(27)
   * 
   * @DisplayName("non-existing value") void nonExistingValueTest(VertxTestContext testContext) {
   * 
   * String apiURL = "search?property=[id]&value=[[rbccps.org/aa9d66a000d94a788" +
   * "95de8d4c0b3a67f3450e531/pscdcl/aqm-bosch-climo/Appa_Balwant_Square_900]]";
   * logger.info("Url is " + BASE_URL + apiURL); client.get(PORT, HOST,
   * BASE_URL.concat(apiURL)).send(ar -> { if (ar.succeeded()) {
   * 
   * Due to stub code in DBservice, the query succeeds and 200 code is obtained which causes the
   * test to fail
   * 
   * assertEquals(400, ar.result().statusCode()); testContext.completeNow(); } else if (ar.failed())
   * { logger.info(ar.cause()); testContext.failed(); } }); }
   */

  @Test
  @Order(28)
  @DisplayName("Attribute Search Invalid Syntax")
  void attributeSearchInvalidSyntax(VertxTestContext testContext) {
    String apiURL = "search?prop=[id]&val=[[existing-value]]";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(400, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(29)
  @DisplayName("rel search")
  void relsearch(VertxTestContext testContext) {
    String apiURL =
        "relsearch?relationship=[resourceGroup.authServerInfo.authType]&value=[[iudx-auth]]";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(200, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(29)
  @DisplayName("Multi Attribute search")
  void multiAttributeSearchtest(VertxTestContext testContext) {
    String apiURL =
        "search?property=[tags,deviceId]&value=[[aqm],[8cff12b2-b8be-1230-c5f6-ca96b4e4e441,climo]]";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(200, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(30)
  @DisplayName("Nested Attribute search")
  void nestedAttributeSearchtest(VertxTestContext testContext) {
    String apiURL = "search?property=[deviceModel.modelName]&value=[[Bosch-Climo]]";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(200, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(31)
  @DisplayName("bbox search")
  void bboxSearchtest(VertxTestContext testContext) {
    String apiURL =
        "search?geoproperty=location&georel=within&geometry=bbox&coordinates=[[73.874537,18.528311],[73.874537,18.528311]]";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(200, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(32)
  @DisplayName("LineString search")
  void LineStringSearchtest(VertxTestContext testContext) {
    String apiURL = "search?geoproperty=location&georel=intersects&geometry=LineString&coordinates"
        + "=[[73.874537,18.528311],[73.836808,18.572797],[73.876484,18.525007]]";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(200, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(33)
  @DisplayName("Invalid Geometry search")
  void invalidGeometrySearchtest(VertxTestContext testContext) {
    String apiURL =
        "search?geoproperty=location&georel=within&geometry=abc123&coordinates=[[[73.696975"
            + "70800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(400, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(34)
  @DisplayName("Invalid Georel search")
  void invalidGeorelSearchtest(VertxTestContext testContext) {
    String apiURL =
        "search?geoproperty=location&georel=abc123&geometry=LineString&coordinates=[[[73.696975"
            + "70800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(400, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(35)
  @DisplayName("Invalid coordinate search")
  void invalidCoordinateSearchtest(VertxTestContext testContext) {
    String apiURL =
        "search?geoproperty=location&georel=within&geometry=bbox&coordinates=[[[73.69697570"
            + "800781,18.592236436157137],[73.69697570800781,abc123]]]";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(400, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(36)
  @DisplayName("Geo Spatial Search Invalid Syntax")
  void geoSpatialInvalidSyntax(VertxTestContext testContext) {
    String apiURL =
        "search?invalidsyntax=location&abc123=within&geometry=bbox&coordinates=[[[73.6969757"
            + "0800781,18.592236436157137],[73.69697570800781,abc123]]]";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(400, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(37)
  @DisplayName("Text Search")
  void textSearchTest(VertxTestContext testContext) {
    /* Encoded whitespaces to get appropriate response */
    String apiURL = "search?q=\"climo\"";

    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(200, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(38)
  @DisplayName("Text Search with *")
  void textSearchAcceptableSpecialCharTest(VertxTestContext testContext) {
    /* Encoded whitespaces to get appropriate response */

    String apiURL = "search?q=\"climo*\"";

    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(200, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(39)
  @DisplayName("Special Characters Text Search")
  void specialCharactersTextSearchTest(VertxTestContext testContext) {
    /* Encoded characters to get appropriate response */
    String apiURL = "search?q=\"@!$%432\"&limit=50&offset=100";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(400, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(40)
  @DisplayName("Text Search Invalid Syntax")
  void textSearchInvalidSyntaxTest(VertxTestContext testContext) {
    /* Encoded whitespaces to get appropriate response */
    String apiURL = "search?abc123=\"text%20to%20search\"&limit=50&offset=100";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(400, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(41)
  @DisplayName("Get Provider")
  void getProviderTest(VertxTestContext testContext) {
    String apiURL =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/aqm-bosch-climo/Sadhu_Wasvani_Square_24/provider";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(200, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(42)
  @DisplayName("Get resourceServer")
  void getResourceServerTest(VertxTestContext testContext) {
    String apiURL =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/aqm-bosch-climo/Sadhu_Wasvani_Square_24/resourceServer";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(200, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(43)
  @DisplayName("Get data model [type]")
  void getDataModelTest(VertxTestContext testContext) {
    String apiURL =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/aqm-bosch-climo/Sadhu_Wasvani_Square_24/type";
    logger.info("Url is " + BASE_URL + apiURL);
    client.get(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
      if (ar.succeeded()) {
        assertEquals(200, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
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
  @DisplayName("Count Item[Geometry:Point, Status:200, Endpoint: /count]")
  void countItemCircle200(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    /* Should give only one item */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "intersects").addQueryParam("maxDistance", "5")
        .addQueryParam("geometry", "Point").addQueryParam("coordinates", "[ 73.874537, 18.528311 ]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey("count"));

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
  @Order(52)
  @DisplayName("count Item[Geometry:Polygon, Status:200, Endpoint: /count]")
  void countItemPolygon200(VertxTestContext testContext) {

    logger.info("starting countItemPolygon200");
    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "within").addQueryParam("maxDistance", "500")
        .addQueryParam("geometry", "Polygon")
        .addQueryParam("coordinates",
            "[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066]"
                + ",[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],"
                + "[73.89472961425781,18.689830007518434],[73.69697570800781,18.592236436157137]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));

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
  @Order(53)
  @DisplayName("Count Attribute[Status:200, Endpoint: /count]")
  void countAttribute200(VertxTestContext testContext) {

    logger.info("starting countAttribute200");

    String id =
        "[[datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/aqm-bosch-climo/Pune Railway Station_28]]";
    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count")).addQueryParam("property", "[id]")
        .addQueryParam("value", id).send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey("count"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @DisplayName("Count SingleAttr multiValue[Status:200, Endpoint: /count]")
  void countAttributeMultiValue200(VertxTestContext testContext) {

    logger.info("starting countAttributeMultiValue200");

    String id1 =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/aqm-bosch-climo/Pune Railway Station_28";
    String id2 =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/aqm-bosch-climo/BopadiSquare_65";

    String id = "[[" + id1 + "," + id2 + "]]";

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count")).addQueryParam("property", "[id]")
        .addQueryParam("value", id).send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey("count"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @DisplayName("Count Attribute[Status:400 invalidSyntax, Endpoint: /count]")
  void countAttribute400(VertxTestContext testContext) {

    logger.info("starting countAttribute400");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count")).addQueryParam("prop", "[id]")
        .addQueryParam("value", "[[existing-value]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @DisplayName("Count multiAttr[Status:200, Endpoint: /count]")
  void countMultiAttribute200(VertxTestContext testContext) {

    logger.info("starting countMultiAttribute200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count"))
        .addQueryParam("property", "[itemStatus,deviceId]")
        .addQueryParam("value", "[[ACTIVE, INACTIVE],[b3ec32ff-fa7d-64fa-c0af-272e25d314e9]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey("count"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @DisplayName("Count nestedAttr[Status:200, Endpoint: /count]")
  void countNestedAttribute200(VertxTestContext testContext) {

    logger.info("starting countNestedAttribute200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count"))
        .addQueryParam("property", "[deviceModelInfo.name]")
        .addQueryParam("value", "[[Bosch-Climo]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey("count"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @DisplayName("Count bbox[Status:200, Endpoint: /count]")
  void countBbox200(VertxTestContext testContext) {

    logger.info("starting countBbox200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "within").addQueryParam("maxDistance", "500")
        .addQueryParam("geometry", "bbox")
        .addQueryParam("coordinates",
            "[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey("count"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @DisplayName("Count LineString[Status:200, Endpoint: /count]")
  void countLineString200(VertxTestContext testContext) {

    logger.info("starting countLineString200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "intersects").addQueryParam("geometry", "LineString")
        .addQueryParam("coordinates",
            "[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137],[73.876484,18.525007]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey("count"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @DisplayName("count geometry[Status:400 invalidValue, Endpoint: /count]")
  void countGeometry400(VertxTestContext testContext) {

    logger.info("starting countGeometry400");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "within").addQueryParam("geometry", "abc123")
        .addQueryParam("coordinates",
            "[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info("status code received : " + serverResponse.result().statusCode());
            logger.info(serverResponse.cause());
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
  @Order(61)
  @DisplayName("Count georel[Status:400 invalidValue, Endpoint: /count]")
  void countGeorel400(VertxTestContext testContext) {

    logger.info("starting countGeorel400");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "abc123").addQueryParam("geometry", "LineString")
        .addQueryParam("coordinates",
            "[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @Order(62)
  @DisplayName("Count geoSpatial[Status:400 invalidSyntax, Endpoint: /count]")
  void countGeoSpatial400(VertxTestContext testContext) {

    logger.info("starting countGeoSpatial400");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("invalidsyntax", "location")
        .addQueryParam("georel", "within").addQueryParam("geometry", "bbox")
        .addQueryParam("coordinates",
            "[[[73.69697570800781,18.592236436157137],[73.69697570800781,abc123]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @Order(63)
  @DisplayName("Count text[Status:200, Endpoint: /count]")
  void countText200(VertxTestContext testContext) {

    logger.info("starting countText200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("q", "\"climo\"")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey("count"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info("status code received : " + serverResponse.result().statusCode());
            logger.info(serverResponse.cause());
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
  @Order(64)
  @DisplayName("Count text*[Status:200, Endpoint: /count]")
  void countTextAcceptableSpecialChar200(VertxTestContext testContext) {

    logger.info("starting countTextAcceptableSpecialChar200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("q", "\"climo*\"")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());
            assertEquals("application/json", serverResponse.result().getHeader("content-type"));
            assertTrue(serverResponse.result().body().toJsonObject().containsKey("count"));

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @Order(65)
  @DisplayName("Count text SpecialChar[Status:400, Endpoint: /count]")
  void countSpecialCharactersText400(VertxTestContext testContext) {

    logger.info("starting countSpecialCharactersText400");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("q", "\"@!$%432\"")
        .addQueryParam("limit", "50").addQueryParam("offset", "100").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @Order(66)
  @DisplayName("count text[Status:400 invalidSyntax, Endpoint: /count]")
  void countText400(VertxTestContext testContext) {

    logger.info("starting countText400 invalidSyntax");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("abc123", "\"text to count\"")
        .addQueryParam("limit", "50").addQueryParam("offset", "100").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @Order(67)
  @DisplayName("Count Item[Geometry:Polygon, Status:400 invalidValue, Endpoint: /count]")
  void countItemPolygon400_1(VertxTestContext testContext) {

    logger.info("starting countItemPolygon400 invalidSyntax");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "within").addQueryParam("maxDistance", "500")
        .addQueryParam("geometry", "Polygon")
        .addQueryParam("coordinates",
            "[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066]"
                + ",[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],"
                + "[73.89472961425781,18.689830007518434],[73.69697570800781,abc123]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

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
  @Order(68)
  @DisplayName("Count coordinates[Status:400 invalidValue, Endpoint: /count]")
  void countCoordinate400(VertxTestContext testContext) {

    logger.info("starting countGeorel400");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "within").addQueryParam("geometry", "LineString")
        .addQueryParam("coordinates",
            "[[[73.69697570800781,18.592236436157137],[73.69697570800781,abc123]]]")
        .send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @DisplayName("Tag search[Status:200, Endpoint: /search]")
  void singleTagSearch200(VertxTestContext testContext) {

    logger.info("starting singleTagSearch200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("property", "[tags]")
        .addQueryParam("value", "[[pollution]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @DisplayName("Tag search[Status:200, Endpoint: /search]")
  void singleTagSearchWithFilter200(VertxTestContext testContext) {

    logger.info("starting singleTagSearchWithFilter200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("property", "[tags]")
        .addQueryParam("value", "[[pollution]]").addQueryParam("filter", "[id,tags]")
        .addQueryParam("offset", "0").addQueryParam("limit", "1").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @Order(71)
  @DisplayName("Tag search[Status:400, Endpoint: /search]")
  void singleTagSearch400_1(VertxTestContext testContext) {

    logger.info("starting singleTagSearch400_1");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("property", "[tags]")
        .addQueryParam("value", "[[abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @Order(72)
  @DisplayName("Tag search[Status:400, Endpoint: /search]")
  void singleTagSearch400_2(VertxTestContext testContext) {

    logger.info("starting singleTagSearch400_2");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("property", "[abc123]")
        .addQueryParam("value", "[[abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @Order(73)
  @DisplayName("Tag search[Status:400, Endpoint: /search]")
  void singleTagSearch400_3(VertxTestContext testContext) {

    logger.info("starting singleTagSearch400_3");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("invalidProperty", "[abc123]")
        .addQueryParam("value", "[[abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @Order(74)
  @DisplayName("Tag MultiSearch[Status:200, Endpoint: /search]")
  void multiTagSearch200(VertxTestContext testContext) {

    logger.info("starting multiTagSearch200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("property", "[tags]")
        .addQueryParam("value", "[[pollution,flood]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @Order(75)
  @DisplayName("Tag MultiSearch[Status:400, Endpoint: /search]")
  void multiTagSearch400_1(VertxTestContext testContext) {

    logger.info("starting multiTagSearch400_1");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("property", "[tags]")
        .addQueryParam("value", "[[abc, abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @Order(76)
  @DisplayName("Tag MultiSearch[Status:400, Endpoint: /search]")
  void multiTagSearch400_2(VertxTestContext testContext) {

    logger.info("starting multiTagSearch400_2");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("property", "[abc123]")
        .addQueryParam("value", "[[abc, abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @Order(77)
  @DisplayName("Tag MultiSearch[Status:400, Endpoint: /search]")
  void multiTagSearch400_3(VertxTestContext testContext) {

    logger.info("starting singleTagSearch400_3");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("invalidProperty", "[abc123]")
        .addQueryParam("value", "[[abc, abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(400, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
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
  @Order(78)
  @DisplayName("Tag MultiSearch[Status:200, Endpoint: /search]")
  void multiTagSearchPartial200(VertxTestContext testContext) {

    logger.info("starting multiTagSearch200");

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("property", "[tags]")
        .addQueryParam("value", "[[pollution,abc123]]").send(serverResponse -> {
          if (serverResponse.succeeded()) {

            /* comparing the response */
            assertEquals(200, serverResponse.result().statusCode());

            testContext.completeNow();
          } else if (serverResponse.failed()) {
            logger.info(serverResponse.cause());
            testContext.failed();
          }
        });
  }
}
