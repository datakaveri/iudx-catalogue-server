package iudx.catalogue.server.apiserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import iudx.catalogue.server.deploy.helper.CatalogueServerDeployer;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiServerTests {
  // Logger
  private static final Logger LOGGER = LoggerFactory.getLogger(ApiServerTests.class);
  private static final String BASE_URL = "127.0.0.1";

  private static WebClient client;

  // Start API Server
  @BeforeAll
  @DisplayName("Deploy a apiserver")
  static void startVertx(VertxTestContext testContext, Vertx vertx) {

    WebClientOptions clientOptions =
        new WebClientOptions().setSsl(true).setVerifyHost(false).setTrustAll(true);
    client = WebClient.create(vertx, clientOptions);

    new Thread(
            () -> {
              CatalogueServerDeployer.main(new String[] {"test"});
            })
        .start();

    vertx.setTimer(
        15000,
        id -> {
          testContext.completeNow();
        });
  }

  @Test
  @Order(1)
  @DisplayName("Single Attribute search")
  void singleAttributeSearchTest(VertxTestContext testContext) {
    String apiURL =
        "/iudx/cat/v1/search?property=[id]&value=[[rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(2)
  @DisplayName("Single Attribute multiple value")
  void singleAttributeMultiValueTest(VertxTestContext testContext) {
    String apiURL =
        "/iudx/cat/v1/search?property=[id]&value=[[rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01,rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(3)
  @DisplayName("non-existing value")
  void nonExistingValueTest(VertxTestContext testContext) {
    String apiURL = "/iudx/cat/v1/search?property=[id]&value=[[non-existing-id]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                /* Due to stub code in DBservice, the query succeeds and 200 code is obtained which causes the test to fail */
                assertEquals(400, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(4)
  @DisplayName("Attribute Search Invalid Syntax")
  void attributeSearchInvalidSyntax(VertxTestContext testContext) {
    String apiURL = "/iudx/cat/v1/search?prop=[id]&val=[[existing-value]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(400, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(5)
  @DisplayName("Multi Attribute search")
  void multiAttributeSearchtest(VertxTestContext testContext) {
    String apiURL =
        "/iudx/cat/v1/search?property=[prop1,prop2]&value=[[prop1-value],[prop2-value1,prop2-value2]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(6)
  @DisplayName("Nested Attribute search")
  void nestedAttributeSearchtest(VertxTestContext testContext) {
    String apiURL = "/iudx/cat/v1/search?property=[provider.name]&value=[[value1]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(7)
  @DisplayName("bbox search")
  void bboxSearchtest(VertxTestContext testContext) {
    String apiURL =
        "/iudx/cat/v1/search?geoproperty=location&georel=within&geometry=bbox&coordinates=[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(8)
  @DisplayName("LineString search")
  void LineStringSearchtest(VertxTestContext testContext) {
    String apiURL =
        "/iudx/cat/v1/search?geoproperty=location&georel=intersects&geometry=LineString&coordinates=[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(9)
  @DisplayName("Invalid Geometry search")
  void invalidGeometrySearchtest(VertxTestContext testContext) {
    String apiURL =
        "/iudx/cat/v1/search?geoproperty=location&georel=within&geometry=abc123&coordinates=[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(400, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(10)
  @DisplayName("Invalid Georel search")
  void invalidGeorelSearchtest(VertxTestContext testContext) {
    String apiURL =
        "/iudx/cat/v1/search?geoproperty=location&georel=abc123&geometry=LineString&coordinates=[[[73.69697570800781,18.592236436157137],[73.69697570800781,18.592236436157137]]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(400, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(11)
  @DisplayName("Invalid coordinate search")
  void invalidCoordinateSearchtest(VertxTestContext testContext) {
    String apiURL =
        "/iudx/cat/v1/search?geoproperty=location&georel=within&geometry=bbox&coordinates=[[[73.69697570800781,18.592236436157137],[73.69697570800781,abc123]]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(400, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(12)
  @DisplayName("Geo Spatial Search Invalid Syntax")
  void geoSpatialInvalidSyntax(VertxTestContext testContext) {
    String apiURL =
        "/iudx/cat/v1/search?invalidsyntax=location&abc123=within&geometry=bbox&coordinates=[[[73.69697570800781,18.592236436157137],[73.69697570800781,abc123]]]";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(400, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(13)
  @DisplayName("Text Search")
  void textSearchTest(VertxTestContext testContext) {
    /* Encoded whitespaces to get appropriate response */
    String apiURL = "/iudx/cat/v1/search?q=\"text%20to%20search\"&limit=50&offset=100";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(14)
  @DisplayName("Text Search with *")
  void textSearchAcceptableSpecialCharTest(VertxTestContext testContext) {
    /* Encoded whitespaces to get appropriate response */
    String apiURL = "/iudx/cat/v1/search?q=\"text%20to%20search*\"&limit=50&offset=100";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(15)
  @DisplayName("Special Characters Text Search")
  void specialCharactersTextSearchTest(VertxTestContext testContext) {
    /* Encoded characters to get appropriate response */
    String apiURL = "/iudx/cat/v1/search?q=\"@!$%432\"&limit=50&offset=100";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(400, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(16)
  @DisplayName("Text Search Invalid Syntax")
  void textSearchInvalidSyntaxTest(VertxTestContext testContext) {
    /* Encoded whitespaces to get appropriate response */
    String apiURL = "/iudx/cat/v1/search?abc123=\"text%20to%20search\"&limit=50&offset=100";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(400, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(17)
  @DisplayName("Get Provider")
  void getProviderTest(VertxTestContext testContext) {
    String apiURL =
        "/iudx/cat/v1/rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01/provider";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(18)
  @DisplayName("Get resourceServer")
  void getResourceServerTest(VertxTestContext testContext) {
    String apiURL =
        "/iudx/cat/v1/rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01/resourceServer";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(19)
  @DisplayName("Get data model [type]")
  void getDataModelTest(VertxTestContext testContext) {
    String apiURL =
        "/iudx/cat/v1/rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01/type";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(20)
  @DisplayName("Get City Config")
  void getCityConfigTest(VertxTestContext testContext) {
    String apiURL = "/iudx/cat/v1/ui/cities";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(21)
  @DisplayName("Set City Config")
  void setCityConfigTest(VertxTestContext testContext) {
    JsonObject body = new JsonObject();
    JsonObject json = new JsonObject();
    json.put("smart_city_name", "PSCDCL")
        .put("map_default_view_lat_lng", new JsonArray().add(18.5644).add(73.7858));
    body.put("configurations", json);
    String apiURL = "/iudx/cat/v1/ui/cities";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .post(8443, BASE_URL, apiURL)
        .sendJsonObject(
            body,
            ar -> {
              if (ar.succeeded()) {
                assertEquals(201, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(22)
  @DisplayName("Update City Config")
  void updateCityConfigTest(VertxTestContext testContext) {
    JsonObject body = new JsonObject();
    JsonObject json = new JsonObject();
    json.put("smart_city_name", "PSCDCL")
        .put("map_default_view_lat_lng", new JsonArray().add(18.5644).add(73.7858));
    body.put("configurations", json);
    String apiURL = "/iudx/cat/v1/ui/cities";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .put(8443, BASE_URL, apiURL)
        .sendJsonObject(
            body,
            ar -> {
              if (ar.succeeded()) {
                assertEquals(201, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(23)
  @DisplayName("Get Config")
  void getConfigTest(VertxTestContext testContext) {
    String apiURL = "/iudx/cat/v1/ui/config";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .get(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(24)
  @DisplayName("Set Config")
  void setConfigTest(VertxTestContext testContext) {
    JsonObject body = new JsonObject();
    JsonObject json = new JsonObject();
    json.put("smart_city_name", "PSCDCL")
        .put("map_default_view_lat_lng", new JsonArray().add(18.5644).add(73.7858));
    body.put("configurations", json);
    String apiURL = "/iudx/cat/v1/ui/config";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .post(8443, BASE_URL, apiURL)
        .sendJsonObject(
            body,
            ar -> {
              if (ar.succeeded()) {
                assertEquals(201, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(25)
  @DisplayName("Update Config")
  void updateConfigTest(VertxTestContext testContext) {
    JsonObject body = new JsonObject();
    JsonObject json = new JsonObject();
    json.put("smart_city_name", "PSCDCL")
        .put("map_default_view_lat_lng", new JsonArray().add(18.5644).add(73.7858));
    body.put("configurations", json);
    String apiURL = "/iudx/cat/v1/ui/config";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .put(8443, BASE_URL, apiURL)
        .sendJsonObject(
            body,
            ar -> {
              if (ar.succeeded()) {
                assertEquals(201, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }

  @Test
  @Order(26)
  @DisplayName("Delete Config")
  void deleteConfigTest(VertxTestContext testContext) {
    String apiURL = "/iudx/cat/v1/ui/config";
    LOGGER.info("Url is " + BASE_URL + apiURL);
    client
        .delete(8443, BASE_URL, apiURL)
        .send(
            ar -> {
              if (ar.succeeded()) {
                assertEquals(200, ar.result().statusCode());
                testContext.completeNow();
              } else if (ar.failed()) {
                LOGGER.info("status code received : " + ar.result().statusCode());
                LOGGER.info(ar.cause());
                testContext.failed();
              }
            });
  }
}
