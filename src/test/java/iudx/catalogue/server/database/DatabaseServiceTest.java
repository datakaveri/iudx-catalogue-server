package iudx.catalogue.server.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseServiceTest {
  private static Logger logger = LoggerFactory.getLogger(DatabaseServiceTest.class);
  private static DatabaseService dbService;
  private static Vertx vertxObj;
  private static RestClient client;
  private static Properties properties;
  private static InputStream inputstream;
  private static String databaseIP;
  private static int databasePort;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    vertxObj = vertx;

    /* Read the configuration and set the rabbitMQ server properties. */
    properties = new Properties();
    inputstream = null;

    try {

      inputstream = new FileInputStream("config.properties");
      properties.load(inputstream);

      databaseIP = properties.getProperty("databaseIP");
      databasePort = Integer.parseInt(properties.getProperty("databasePort"));

    } catch (Exception ex) {

      logger.info(ex.toString());

    }

    // TODO : Need to enable TLS using xpack security
    client = RestClient.builder(new HttpHost(databaseIP, databasePort, "http")).build();
    vertx.deployVerticle(new DatabaseVerticle(), testContext.succeeding(id -> {
      logger.info("Successfully deployed Verticle for Test" + id);
      dbService = new DatabaseServiceImpl(client);
      testContext.completeNow();
    }));
  }

  @AfterEach
  public void finish(VertxTestContext testContext) {
    logger.info("Finishing....");
    vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  }

  @Test
  @DisplayName("Testing Geo-circle query")
  void searchGeoCircle(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("searchType", "geoSearch_").put("coordinates", new JsonArray().add(75.9)
            .add(14.5)).put("maxDistance", 5000).put("geometry","point").put("georel","intersects")
            .put("geoproperty","location.geometry");

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(75.92, response.getJsonArray("results").getJsonObject(0)
          .getJsonObject("location").getJsonObject("geometry").getJsonArray("coordinates")
          .getDouble(0));
      testContext.completeNow();
    })));
  }

//  @Test
//  @DisplayName("Testing Basic Exceptions (No instanceId key)")
//  void searchWithNoResourceId(VertxTestContext testContext) {
//    JsonObject request = new JsonObject().put("searchType", "geoSearch_");
//
//    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
//      JsonObject res = new JsonObject(response.getMessage());
//      assertEquals("No instanceId found", res.getString("desc"));
//      testContext.completeNow();
//    })));
//  }

  @Test
  @DisplayName("Testing Basic Exceptions (No searchType key)")
  void searchWithSearchType(VertxTestContext testContext) {
    JsonObject request = new JsonObject();

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals("No searchType found", res.getString("desc"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo-Polygon query")
  void searchGeoPolygon(VertxTestContext testContext) {
    /**
     * coordinates should look like this
     * [[[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5],[lo1,la1]]]
     */

    JsonObject request =
        new JsonObject().put("geometry", "polygon").put("georel", "within").put("coordinates",
                new JsonArray().add(new JsonArray().add(new JsonArray().add(75.9).add(14.5))
                    .add(new JsonArray().add(72).add(13))
                    .add(new JsonArray().add(73).add(20))
                    .add(new JsonArray().add(75.9).add(14.5))))
            .put("geoproperty", "geoJsonLocation").put("searchType", "geoSearch_")
            ;

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(75.92, response.getJsonArray("results").getJsonObject(0)
          .getJsonObject("location").getJsonObject("geometry").getJsonArray("coordinates")
          .getDouble(0));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo Exceptions (Missing necessary parameters [coordinates, geoproperty, georel)")
  void searchMissingGeoParamsGeometry(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("searchType", "geoSearch_").put("geometry", "polygon");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals("Missing/Invalid geo parameters", res.getString("desc"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo Polygon Exceptions (First and Last coordinates don't match)")
  void searchPolygonFirstLastNoMatch(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject().put("geometry", "polygon").put("georel", "within").put("coordinates",
            new JsonArray().add(new JsonArray().add(new JsonArray().add(75.9).add(14.5))
                .add(new JsonArray().add(72).add(13))
                .add(new JsonArray().add(73).add(20))))
            .put("geoproperty", "geoJsonLocation").put("searchType", "geoSearch_");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals("Coordinate mismatch (Polygon)", res.getString("desc"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo-LineString query")
  void searchGeoLineString(VertxTestContext testContext) {
    /**
     * coordinates should look like this [[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5]]
     */

    JsonObject request =
        new JsonObject()
            .put("geometry", "linestring").put("georel", "intersects")
            .put("coordinates",
                new JsonArray().add(new JsonArray().add(75.9).add(14.5))
                    .add(new JsonArray().add(72).add(13))
                    .add(new JsonArray().add(73).add(20)))
            .put("geoproperty", "geoJsonLocation")
            .put("searchType", "geoSearch_");

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(75.92, response.getJsonArray("results").getJsonObject(0)
          .getJsonObject("location").getJsonObject("geometry").getJsonArray("coordinates")
          .getDouble(0));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo-BBOX query")
  void searchGeoBbox(VertxTestContext testContext) {
    /**
     * coordinates should look like this [[lo1,la1],[lo3,la3]]
     */

    JsonObject request =
        new JsonObject()
            .put("geometry", "bbox").put("georel", "within")
            .put("coordinates",
                new JsonArray().add(new JsonArray().add(73).add(20))
                    .add(new JsonArray().add(75).add(14)))
            .put("geoproperty", "geoJsonLocation")
            .put("searchType", "geoSearch_");

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.86, response.getJsonArray("results").getJsonObject(0)
          .getJsonObject("location").getJsonObject("geometry").getJsonArray("coordinates")
          .getJsonArray(0).getJsonArray(0).getDouble(0));
      testContext.completeNow();
    })));

  }

  @Test
  @DisplayName("Testing Geo-BBOX Exceptions [empty response]")
  void searchBboxEmptyResponse(VertxTestContext testContext) {
    /**
     * coordinates should look like this [[lo1,la1],[lo3,la3]]
     */

    JsonObject request =
        new JsonObject()
            .put("geometry", "bbox").put("georel", "within")
            .put("coordinates",
                new JsonArray().add(new JsonArray().add(82).add(25.33))
                    .add(new JsonArray().add(82.01).add(25.317)))
            .put("geoproperty", "geoJsonLocation")
            .put("searchType", "geoSearch_");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals("Empty response", res.getString("desc"));
      testContext.completeNow();
    })));

  }

  @Test
  @DisplayName("Testing Response Filter")
  void searchResponseFilter(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("searchType", "responseFilter_")
            .put("attrs", new JsonArray().add("id").add("tags"));
    Set<String> attrs = new HashSet<>();
    attrs.add("id");
    attrs.add("tags");
    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray("results")) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }

  @Test
  @DisplayName("Testing Response Filter Exceptions (Missing parameters [attrs]")
  void searchMissingResponseFilterParams(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("searchType", "responseFilter_");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals("Missing/Invalid responseFilter parameters", res.getString("desc"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Complex (Geo + Response Filter) Search")
  void searchComplexGeoResponse(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("searchType", "responseFilter_geoSearch_")
            .put("attrs", new JsonArray().add("id").add("tags").add("location"))
            .put("coordinates", new JsonArray().add(75.9).add(14.5)).put("maxDistance", 5000)
            .put("geometry","point").put("georel","intersects")
            .put("geoproperty","location.geometry");
    Set<String> attrs = new HashSet<>();
    attrs.add("id");
    attrs.add("tags");
    attrs.add("location");
    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray("results")) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(75.92, response.getJsonArray("results").getJsonObject(0)
            .getJsonObject("location").getJsonObject("geometry").getJsonArray("coordinates")
            .getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }

}
