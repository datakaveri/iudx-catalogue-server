package iudx.catalogue.server.apiserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.vertx.core.Future;
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
import iudx.catalogue.server.starter.CatalogueServerStarter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test class for ApiServerVerticle api handlers
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

    CatalogueServerStarter starter = new CatalogueServerStarter();
    Future<JsonObject> result = starter.startServer();

    result.onComplete(resultHandler -> {
      if (resultHandler.succeeded()) {
        vertx.setTimer(15000, id -> {
          logger.info("!!!!!!!!\n\n!!!!!!!!!");
          testContext.completeNow();
        });
      }
    });
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

    fileSytem.readFile("src/test/resources/request_body.json", fileRes -> {
      if (fileRes.succeeded()) {

        JsonObject fileStream = fileRes.result().toJsonObject();

        // Send the file to the server using POST
        client.post(PORT, HOST, BASE_URL.concat("item")).putHeader("token", "abc")
            .putHeader("Content-Type", "application/json").sendJson(fileStream, serverResponse -> {
              if (serverResponse.succeeded()) {
                assertEquals(201, serverResponse.result().statusCode());
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
   * Tests the createItem of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(2)
  @DisplayName("Create Item[Status:400, Endpoint: /item]")
  public void createItem400(VertxTestContext testContext) {

    /* open and read the entire test file mentioned in the path */
    fileSytem.readFile("src/test/resources/request_body.json", fileRes -> {
      if (fileRes.succeeded()) {

        /* mapping the file as JsonObject */
        JsonObject jsonBody = fileRes.result().toJsonObject();

        /* with itemType null or empty */
        jsonBody.putNull("itemType");

        /* Send the file to the server using POST */
        client.post(PORT, HOST, BASE_URL.concat("item")).putHeader("token", "abc")
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
   * Tests the updateItem of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(3)
  @DisplayName("Update Item[Status:200, Endpoint: /item]")
  void updateItem200(VertxTestContext testContext) {

    String itemId = "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs"
        + ".varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live";

    /* open and read the entire test file mentioned in the path */
    fileSytem.readFile("src/test/resources/request_body.json", fileRes -> {
      if (fileRes.succeeded()) {

        /* mapping the file as JsonObject */
        JsonObject jsonBody = fileRes.result().toJsonObject();

        /* Send the file to the server using PUT */
        client.put(PORT, HOST, BASE_URL.concat("item/").concat(itemId)).putHeader("token", "abc")
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

    String itemId = "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs"
        + ".varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live";

    /* open and read the entire test file mentioned in the path */
    fileSytem.readFile("src/test/resources/request_body.json", fileRes -> {
      if (fileRes.succeeded()) {

        /* mapping the file as JsonObject */
        JsonObject jsonBody = fileRes.result().toJsonObject();

        /* with itemType null or empty */
        jsonBody.putNull("itemType");

        /* Send the file to the server using PUT */
        client.put(PORT, HOST, BASE_URL.concat("item/").concat(itemId)).putHeader("token", "abc")
            .putHeader("Content-Type", "application/json").sendJson(jsonBody, serverResponse -> {
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

    String itemId = "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs"
        + ".varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live";

    /* Send the file to the server using DELETE */
    client.delete(PORT, HOST, BASE_URL.concat("item/").concat(itemId)).putHeader("token", "abc")
        .putHeader("Content-Type", "application/json").send(serverResponse -> {
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
  @Order(6)
  @DisplayName("Search Item[Geometry:Point, Status:200, Endpoint: /search]")
  void searchItemCircle200(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "near").addQueryParam("maxDistance", "500")
        .addQueryParam("geometry", "Point")
        .addQueryParam("coordinates", "[73.85534405708313,18.52008289032131]")
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
  @Order(7)
  @DisplayName("Search Item[Geometry:Point, Status:400 invalidValue, Endpoint: /search]")
  void searchItemCircle400_1(VertxTestContext testContext) {

    /* Send the file to the server using GET with query parameters */
    client.get(PORT, HOST, BASE_URL.concat("search/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "near").addQueryParam("maxDistance", "500")
        .addQueryParam("geometry", "Point")
        .addQueryParam("coordinates", "[73.85534405708313,abc123]").send(serverResponse -> {
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

    logger.info("starting searchItemPolygon200");
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

    String itemId = "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs"
        + ".varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live";

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("items/").concat(itemId)).send(serverResponse -> {
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
    client.get(PORT, HOST, BASE_URL.concat("tags/")).send(serverResponse -> {
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
    client.get(PORT, HOST, BASE_URL.concat("providers")).send(serverResponse -> {
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
   * Tests the listResourceGroups api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(22)
  @DisplayName("List ResourceGroups[Status:200, Endpoint: /resourcegroups]")
  void listResourceGroups200(VertxTestContext testContext) {

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat("resourcegroups")).send(serverResponse -> {
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
   * Tests the listResourceRelationship api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(23)
  @DisplayName("Search Relationship[Status:200, Endpoint: /<resourceGroupID>/resource]")
  void listResourceRelationship200(VertxTestContext testContext) {

    String resourceGroupID = "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531"
        + "/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01";

    /* Send the file to the server using GET */
    client.get(PORT, HOST, BASE_URL.concat(resourceGroupID).concat("/resource"))
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

  /**
   * Tests the listResourceGroupRelationship api handler of ApiServerVerticle.
   *
   * @param testContext of asynchronous operations
   */
  @Test
  @Order(24)
  @DisplayName("Search Relationship[Status:200, Endpoint: /<resourceID>/resourceGroup]")
  void listResourceGroupRelationship200(VertxTestContext testContext) {

    String resourceID = "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531"
        + "/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01";

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
    String apiURL =
        "search?property=[id]&value=[[rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01]]";
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
  @Order(26)
  @DisplayName("Single Attribute multiple value")
  void singleAttributeMultiValueTest(VertxTestContext testContext) {
    String apiURL =
        "search?property=[id]&value=[[rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01,rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02]]";
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

  // @Test
  // @Order(27)
  // @DisplayName("non-existing value")
  // void nonExistingValueTest(VertxTestContext testContext) {
  // // TODO: This test case will not pass because of assert failure
  // String apiURL = "search?property=[id]&value=[[non-existing-id]]";
  // logger.info("Url is " + BASE_URL + apiURL);
  // client
  // .get(PORT, HOST, BASE_URL.concat(apiURL))
  // .send(
  // ar -> {
  // if (ar.succeeded()) {
  // /* Due to stub code in DBservice, the query succeeds and 200 code is obtained
  // which causes the test to fail */
  // assertEquals(400, ar.result().statusCode());
  // testContext.completeNow();
  // } else if (ar.failed()) {
  // logger.info("status code received : " + ar.result().statusCode());
  // logger.info(ar.cause());
  // testContext.failed();
  // }
  // });
  // }

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
        "search?property=[prop1,prop2]&value=[[prop1-value],[prop2-value1,prop2-value2]]";
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
    String apiURL = "search?property=[provider.name]&value=[[value1]]";
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
        "search?geoproperty=location&georel=within&geometry=bbox&coordinates=[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]";
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
    String apiURL =
        "search?geoproperty=location&georel=intersects&geometry=LineString&coordinates=[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]";
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
        "search?geoproperty=location&georel=within&geometry=abc123&coordinates=[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]";
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
        "search?geoproperty=location&georel=abc123&geometry=LineString&coordinates=[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]";
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
        "search?geoproperty=location&georel=within&geometry=bbox&coordinates=[[[73.69697570800781,18.592236436157137],[73.69697570800781,abc123]]]";
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
        "search?invalidsyntax=location&abc123=within&geometry=bbox&coordinates=[[[73.69697570800781,18.592236436157137],[73.69697570800781,abc123]]]";
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
    String apiURL = "search?q=\"text%20to%20search\"&limit=50&offset=100";
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
    String apiURL = "search?q=\"text%20to%20search*\"&limit=50&offset=100";
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
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01/provider";
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
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01/resourceServer";
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
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01/type";
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
  @Order(44)
  @DisplayName("Get City Config")
  void getCityConfigTest(VertxTestContext testContext) {
    String apiURL = "ui/cities";
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
  @Order(45)
  @DisplayName("Set City Config")
  void setCityConfigTest(VertxTestContext testContext) {
    JsonObject body = new JsonObject();
    JsonObject json = new JsonObject();
    json.put("smart_city_name", "PSCDCL").put("map_default_view_lat_lng",
        new JsonArray().add(18.5644).add(73.7858));
    body.put("configurations", json);
    String apiURL = "ui/cities";
    logger.info("Url is " + BASE_URL + apiURL);
    client.post(PORT, HOST, BASE_URL.concat(apiURL)).sendJsonObject(body, ar -> {
      if (ar.succeeded()) {
        assertEquals(201, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(46)
  @DisplayName("Update City Config")
  void updateCityConfigTest(VertxTestContext testContext) {
    JsonObject body = new JsonObject();
    JsonObject json = new JsonObject();
    json.put("smart_city_name", "PSCDCL").put("map_default_view_lat_lng",
        new JsonArray().add(18.5644).add(73.7858));
    body.put("configurations", json);
    String apiURL = "ui/cities";
    logger.info("Url is " + BASE_URL + apiURL);
    client.put(PORT, HOST, BASE_URL.concat(apiURL)).sendJsonObject(body, ar -> {
      if (ar.succeeded()) {
        assertEquals(201, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(47)
  @DisplayName("Get Config")
  void getConfigTest(VertxTestContext testContext) {
    String apiURL = "ui/config";
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
  @Order(48)
  @DisplayName("Set Config")
  void setConfigTest(VertxTestContext testContext) {
    JsonObject body = new JsonObject();
    JsonObject json = new JsonObject();
    json.put("smart_city_name", "PSCDCL").put("map_default_view_lat_lng",
        new JsonArray().add(18.5644).add(73.7858));
    body.put("configurations", json);
    String apiURL = "ui/config";
    logger.info("Url is " + BASE_URL + apiURL);
    client.post(PORT, HOST, BASE_URL.concat(apiURL)).sendJsonObject(body, ar -> {
      if (ar.succeeded()) {
        assertEquals(201, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(49)
  @DisplayName("Update Config")
  void updateConfigTest(VertxTestContext testContext) {
    JsonObject body = new JsonObject();
    JsonObject json = new JsonObject();
    json.put("smart_city_name", "PSCDCL").put("map_default_view_lat_lng",
        new JsonArray().add(18.5644).add(73.7858));
    body.put("configurations", json);
    String apiURL = "ui/config";
    logger.info("Url is " + BASE_URL + apiURL);
    client.put(PORT, HOST, BASE_URL.concat(apiURL)).sendJsonObject(body, ar -> {
      if (ar.succeeded()) {
        assertEquals(201, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        logger.info("status code received : " + ar.result().statusCode());
        logger.info(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(50)
  @DisplayName("Delete Config")
  void deleteConfigTest(VertxTestContext testContext) {
    String apiURL = "ui/config";
    logger.info("Url is " + BASE_URL + apiURL);
    client.delete(PORT, HOST, BASE_URL.concat(apiURL)).send(ar -> {
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
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("geoproperty", "location")
        .addQueryParam("georel", "near").addQueryParam("maxDistance", "500")
        .addQueryParam("geometry", "Point")
        .addQueryParam("coordinates", "[73.85534405708313,18.52008289032131]")
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

    String id = "[[rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi."
        + "iudx.org.in/varanasi-aqm/EM_01_0103_01]]";
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

    String id = "[[rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi."
        + "iudx.org.in/varanasi-aqm/EM_01_0103_01,rbccps.org/aa9d66a000d94a78895de8d4c0"
        + "b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02]]";

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
    client.get(PORT, HOST, BASE_URL.concat("count")).addQueryParam("property", "[prop1,prop2]")
        .addQueryParam("value", "[[prop1-value],[prop2-value1,prop2-value2]]")
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
    client.get(PORT, HOST, BASE_URL.concat("count")).addQueryParam("property", "[provider.name]")
        .addQueryParam("value", "[[value1]]").send(serverResponse -> {
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
            "[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]")
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
            "[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]")
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
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("q", "\"text to count\"")
        .addQueryParam("limit", "50").addQueryParam("offset", "100").send(serverResponse -> {
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
    client.get(PORT, HOST, BASE_URL.concat("count/")).addQueryParam("q", "\"text to count*\"")
        .addQueryParam("limit", "50").addQueryParam("offset", "100").send(serverResponse -> {
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
}
