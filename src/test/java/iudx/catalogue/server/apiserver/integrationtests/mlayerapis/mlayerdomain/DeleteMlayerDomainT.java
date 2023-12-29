package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerdomain;
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
public class DeleteMlayerDomainT {
    private static String domainId;
    private static Path configFile = Paths.get("configDomain.json");
    @BeforeAll
    public static void setUp() {
        // Check if the file exists, and if not, create it
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
    @DisplayName("Delete Mlayer Domain Success Test-200")
    public void deleteMlayerDomainTest(){
        given()
                .queryParam("id",domainId)
                .header("token",cosAdminToken)
                .when()
                .delete("/internal/ui/domain")
                .then()
                .statusCode(200)
                .log().body()
                .body("type",equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Delete Mlayer Domain With Invalid Token Test-401")
    public void deleteMlayerDomainWithInvalidTokenTest(){
        given()
                .queryParam("id",domainId)
                .header("token","abc")
                .when()
                .delete("/internal/ui/domain")
                .then()
                .statusCode(401)
                .log().body()
                .body("type",equalTo("urn:dx:cat:InvalidAuthorizationToken"));
    }
    @AfterAll
    public static void tearDown() {
        // Remove instanceId from the JSON file after the tests
        try {
            JsonObject json = new JsonObject(new String(Files.readAllBytes(configFile)));
            json.remove("domainId");
            Files.write(configFile, json.encode().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
