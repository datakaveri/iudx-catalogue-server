package iudx.catalogue.server.apiserver.integrationTests.createItem;

import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.is;

@ExtendWith(RestAssuredConfiguration.class)
public class CrudApisIT {

  private static final Logger LOGGER = LogManager.getLogger(CrudApisIT.class);

  @Test
  @DisplayName("testing get item - 400")
  void getItem() {

    LOGGER.debug(basePath);
    LOGGER.debug(baseURI);
    LOGGER.debug(port);

    given()
        .param("id", "bbeacb12-5e54-339d-92e0-d8e063b551a8")
        .when()
        .get("/item")
        .then()
        .body("type", is("urn:dx:cat:Success"));

    given()
        .param("id", "bbeacb12-5e54-339d-92e0-d8e063b551a8")
        .when()
        .get("/item")
        .then()
        .statusCode(200);

    given()
        .param("id", "bbeacb12-5e54-339d-92e0-d8e063b551a8")
        .when()
        .get("/item")
        .then()
        .header("Content-Type", "application/json");

  }

}
