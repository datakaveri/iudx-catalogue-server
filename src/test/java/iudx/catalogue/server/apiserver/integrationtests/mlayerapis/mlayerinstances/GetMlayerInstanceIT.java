package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerinstances;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
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
public class GetMlayerInstanceIT {
    private static String instanceId;
    @BeforeAll
    public static void setUp() {
        // Check if the file exists, and if not, create it
        Path configFile = Paths.get("configInstances.json");
        if (!Files.exists(configFile)) {
            try {
                // Create an empty JSON object if the file doesn't exist
                Files.write(configFile, "{}".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Read instanceId from the JSON file
        JsonObject json;
        try {
            json = new JsonObject(new String(Files.readAllBytes(configFile)));
            instanceId = json.getString("instanceId");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Test
    @DisplayName("Get All Mlayer Instance success response test- 200")
    public void getAllMlayerInstanceSuccessTest(){
        given()
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .when()
                .get("/internal/ui/instance")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));

    }
    @Test
    @DisplayName("Get Mlayer Instance success response by Id test- 200")
    public void getMlayerInstanceByIdSuccessTest(){
        given()
                .queryParam("id",instanceId)
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .when()
                .get("/internal/ui/instance")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));

    }
}
