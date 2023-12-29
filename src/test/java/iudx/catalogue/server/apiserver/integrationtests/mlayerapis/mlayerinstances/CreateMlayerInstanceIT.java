package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerinstances;
import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileWriter;
import java.io.IOException;

import static io.restassured.RestAssured.given;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.cosAdminToken;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(RestAssuredConfiguration.class)
public class CreateMlayerInstanceIT {
    private static String instanceId;
    @Test
    @DisplayName("Create Mlayer Instance Success Test-201")
    public void createMlayerInstanceTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", "bhavya")
                .put("cover", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/bhavya.jpg")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/bhavya.jpg")
                .put("logo", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/bhavya.jpg")
                .put("coordinates", new JsonArray());

        Response resp= given()
                .header("Content-Type", "application/json")
                .header("token", cosAdminToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/instance");
        JsonObject respJson = new JsonObject(resp.body().asString());
        JsonObject firstResult = respJson.getJsonArray("results").getJsonObject(0);
        instanceId = firstResult.getString("id");
                resp.then()
                .statusCode(201)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("results[0].id", notNullValue());
        // Write domainId to a JSON file
        JsonObject json = new JsonObject().put("instanceId", instanceId);
        try (FileWriter file = new FileWriter("configInstances.json")) {
            file.write(json.encodePrettily());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Test
    @DisplayName("Create Mlayer Instance With Invalid Token Test-401")
    public void createMlayerInstanceWithInvalidTokenTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", "divyaIUDX")
                .put("cover", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/punay.jpg")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/punay.jpg")
                .put("logo", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/punay.jpg")
                .put("coordinates", new JsonArray());
        given()
                .header("Content-Type", "application/json")
                .header("token", "abc")
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/instance")
                .then()
                .statusCode(401)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));
    }
    @Test
    @DisplayName("Create Mlayer Instance With Invalid Schema Test-400")
    public void createMlayerInstanceWithInvalidSchemaTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", "punay")
                .put("coveeer", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/punay.jpeg")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/punay.jpeg")
                .put("logo", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/punay.jpeg");

        given()
                .header("Content-Type", "application/json")
                .header("token", cosAdminToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/instance")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));
    }

}
