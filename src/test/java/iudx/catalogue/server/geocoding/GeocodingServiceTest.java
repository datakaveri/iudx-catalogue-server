package iudx.catalogue.server.geocoding;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import jdk.jfr.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.catalogue.server.Configuration;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class GeocodingServiceTest {
    private static GeocodingService geoService;
    private static JsonObject config;
    private static Vertx vertxObj;
    
    private static final Logger LOGGER = LogManager.getLogger(GeocodingServiceTest.class);

    JsonObject doc = new JsonObject("{\"tags\": [\"a\",\"b\",\"c\"], \"description\": \"some description,with characters\", \"name\": \"iudx\", \"label\": \"thisisiudx\", \"descriptor\": {\"co2\": \"high\", \"no2\": [\"low\", \"medium\"]}, \"location\": {\"type\": \"Place\",\"address\": \"Pune\",\"geometry\": {\"type\": \"Point\", \"coordinates\": [\"77.570423\",\"13.013945\"]}}}");

    @BeforeAll
    static void startVertx(Vertx vertx, VertxTestContext testContext) {
      vertxObj = vertx;

      config = Configuration.getConfiguration("./configs/config-test.json", 1);
      
      WebClientOptions webClientOptions = new WebClientOptions();
      webClientOptions.setTrustAll(true).setVerifyHost(false);
      WebClient client =  WebClient.create(vertx, webClientOptions);
      // WebClient client = GeocodingVerticle.createWebClient(vertxObj, config, true);
      geoService = new GeocodingServiceImpl(client, "pelias_api");
      
      LOGGER.info("Geocoding Service setup complete");
      testContext.completeNow();
    }

    @AfterEach
    public void finish(VertxTestContext testContext) {
      LOGGER.info("Finishing....");
      vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
    }

    @Test
    @DisplayName("Summarize test")
    void summarize(VertxTestContext testContext) {
      geoService.geoSummarize(doc, ar-> {
        LOGGER.info("Result: " + ar.result().toString());
        testContext.completeNow();
      });
    }
}
