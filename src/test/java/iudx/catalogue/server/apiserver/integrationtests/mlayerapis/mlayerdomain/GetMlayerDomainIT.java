package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerdomain;

import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class GetMlayerDomainIT {

  private static String domainId;

  @BeforeAll
  public static void setUp() {
    // Check if the file exists, and if not, create it
    Path configFile = Paths.get("configDomain.json");
    if (!Files.exists(configFile)) {
      try {
        // Create an empty JSON object if the file doesn't exist
        Files.write(configFile, "{}".getBytes());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Read domainId from the JSON file
    JsonObject json;
    try {
      json = new JsonObject(new String(Files.readAllBytes(configFile)));
      domainId = json.getString("domainId");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  @DisplayName("Get All Mlayer Domains Success Test-200")
  public void getAllMlayerDomainsTest() {
    given()
            .when()
            .get("/internal/ui/domain")
            .then()
            .statusCode(200)
            .log().body()
            .body("type", equalTo("urn:dx:cat:Success"));
  }

  @Test
  @DisplayName("Get Mlayer Domain by Id Success Test-200")
  public void getMlayerDomainByIdTest() {
    System.out.println("domain id:" + domainId);
    given()
            .param("id", domainId)
            .when()
            .get("/internal/ui/domain")
            .then()
            .statusCode(200)
            .log().body()
            .body("type", equalTo("urn:dx:cat:Success"));
  }
}
