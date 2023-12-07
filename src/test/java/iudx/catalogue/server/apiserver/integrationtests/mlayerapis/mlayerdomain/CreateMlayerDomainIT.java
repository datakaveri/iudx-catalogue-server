package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerdomain;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.cosAdminToken;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class CreateMlayerDomainIT {
    @Test
    @DisplayName("Create Mlayer Domain Success Test-201")
    public void createMlayerDomainTest(){
        JsonObject requestBody = new JsonObject()
                .put("description", "Data Models that pertain to civic domain")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/civic.png")
                .put("label", "Civic")
                .put("name", "civic");

        given()
                .header("Content-Type", "application/json")
                .header("token", cosAdminToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/domain")
                .then()
                .statusCode(201)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Create Mlayer Domain with Invalid Schema Test-400")

    public void createMlayerDomainWithInvalidSchemaTest(){
        String domainId="6aeed588-a28e-4c17-9694-edfa24e7d227";
        JsonObject requestBody = new JsonObject()
                .put("descriiiption", "Data Models that pertain to civic domain")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/civic.png")
                .put("label", "Civic")
                .put("name", "civic");

        given()
                .queryParam("id",domainId)
                .header("Content-Type", "application/json")
                .header("token", cosAdminToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/domain")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));
    }
    @Test
    @DisplayName("Create Mlayer Domain With Invalid Token Test-401")
    public void createMlayerDomainWithInvalidTokenTest(){
        String domainId="6aeed588-a28e-4c17-9694-edfa24e7d227";
        JsonObject requestBody = new JsonObject()
                .put("description", "Data Models that pertain to civic domain")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/civic.png")
                .put("label", "Civic")
                .put("name", "civic");
        given()
                .queryParam("id",domainId)
                .header("Content-Type", "application/json")
                .header("token", "abc")
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/domain")
                .then()
                .statusCode(401)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));
    }


}
