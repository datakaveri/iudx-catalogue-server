package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerdomain;
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
public class UpdateMlayerDomainIT  {
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
    @DisplayName("Update Mlayer Domain Success Test-200")
    public void updateMlayerDomainTest(){
        //System.out.println("Domain ID to update: " + domainId);
        JsonObject requestBody = new JsonObject()
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/civic.jpeg")
                .put("name", "civic")
                .put("description", "Data Models that pertain to civic domain")
                .put("label", "Civic");
        given()
                .queryParam("id",domainId)
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/internal/ui/domain")
                .then()
                .statusCode(200)
                .log().body()
                .body("type",equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Update Mlayer Domain With Invalid Schema Test-400")
    public void updateMlayerDomainWithInvalidSchemaTest(){
        JsonObject requestBody = new JsonObject()
                .put("descriiiptionn", "Data Models that pertain to civic domain")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/civic.png")
                .put("label", "Civic")
                .put("name", "civic");

        given()
                .queryParam("id",domainId)
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/internal/ui/domain")
                .then()
                .statusCode(400)
                .log().body()
                .body("type",equalTo("urn:dx:cat:InvalidSchema"));
    }
    @Test
    @DisplayName("Update Mlayer Domain With Invalid Token Test-401")
    public void updateMlayerDomainWithInvalidTokenTest(){
        JsonObject requestBody = new JsonObject()
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/civic.jpeg")
                .put("name", "civic")
                .put("description", "Data Models that pertain to civic domain")
                .put("label", "Civic");
        given()
                .queryParam("id",domainId)
                .header("Content-Type","application/json")
                .header("token","abc")
                .body(requestBody.encodePrettily())
                .when()
                .put("/internal/ui/domain")
                .then()
                .statusCode(401)
                .log().body()
                .body("type",equalTo("urn:dx:cat:InvalidAuthorizationToken"));
    }



}
