package iudx.catalogue.server.geocoding;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jdk.jfr.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GeoSummarizerTest {
    private static final Logger LOGGER = LogManager.getLogger(GeoSummarizerTest.class);
    JsonObject doc = new JsonObject("{\"tags\": [\"a\",\"b\",\"c\"], \"description\": \"some description,with characters\", \"name\": \"iudx\", \"label\": \"thisisiudx\", \"descriptor\": {\"co2\": \"high\", \"no2\": [\"low\", \"medium\"]}, \"location\": {\"type\": \"Place\",\"address\": \"ABD area, Pune\"}}");

    @Test
    @DisplayName("Summarize test")
    void summarize(VertxTestContext testContext) {
        LOGGER.info(GeoSummarizer.summarize(doc));
        testContext.completeNow();
    }
}