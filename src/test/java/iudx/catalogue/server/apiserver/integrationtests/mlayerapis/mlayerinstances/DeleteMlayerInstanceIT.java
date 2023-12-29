package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerinstances;

import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.cosAdminToken;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class DeleteMlayerInstanceIT {
    private static String instanceId;
  private static Path configFile = Paths.get("configInstances.json");

    @BeforeAll
    public static void setUp() {
        // Read instanceId from the JSON file
        try {
            JsonObject json = new JsonObject(new String(Files.readAllBytes(configFile)));
            instanceId = json.getString("instanceId");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Delete Mlayer Instance success response test- 200")
    public void deleteMlayerInstanceSuccessTest() {
        given()
                .queryParam("id", instanceId)
                .header("Content-Type", "application/json")
                .header("token", cosAdminToken)
                .when()
                .delete("/internal/ui/instance")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }

    @Test
    @DisplayName("Delete Mlayer Instance with Invalid Token response test- 401")
    public void deleteMlayerInstanceWithInvalidTokenTest() {
        given()
                .queryParam("id", instanceId)
                .header("Content-Type", "application/json")
                .header("token", "abc")
                .when()
                .delete("/internal/ui/instance")
                .then()
                .statusCode(401)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));
    }
    @AfterAll
    public static void tearDown() {
        // Remove instanceId from the JSON file after the tests
        try {
            JsonObject json = new JsonObject(new String(Files.readAllBytes(configFile)));
            json.remove("instanceId");
            Files.write(configFile, json.encode().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
