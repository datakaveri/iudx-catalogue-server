package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerinstances;
import io.vertx.core.json.JsonArray;
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
public class UpdateMlayerInstanceIT {
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
    @DisplayName("Update Mlayer Instance success response test- 200")
    public void updateMlayerInstanceSuccessTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", "bhavya")
                .put("cover", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/bhavya.jpeg")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/bhavya.jpeg")
                .put("logo", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/bhavya.jpeg")
                .put("coordinates", new JsonArray());
        given()
                .queryParam("id",instanceId)
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/internal/ui/instance")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));

    }
    @Test
    @DisplayName("Update Mlayer Instance with invalid token test- 401")
    public void updateMlayerInstanceWithInvalidTokenTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", "bhavya")
                .put("cover", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/bhavya.jpeg")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/bhavya.jpeg")
                .put("logo", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/bhavya.jpeg")
                .put("coordinates", new JsonArray());
        given()
                .queryParam("id",instanceId)
                .header("Content-Type","application/json")
                .header("token","abc")
                .body(requestBody.encodePrettily())
                .when()
                .put("/internal/ui/instance")
                .then()
                .statusCode(401)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));

    }
    @Test
    @DisplayName("Update Mlayer Instance with invalid schema test- 400")
    public void updateMlayerInstanceWithInvalidSchemaTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", "bhavya")
                .put("coveer", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/bhavya.jpeg")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/bhavya.jpeg")
                .put("logo", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/bhavya.jpeg");

        given()
                .queryParam("id",instanceId)
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/internal/ui/instance")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));

    }

}
