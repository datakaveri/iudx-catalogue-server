package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerdomain;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.io.FileWriter;
import java.io.IOException;
import static io.restassured.RestAssured.given;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.cosAdminToken;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class CreateMlayerDomainIT {
   private static String domainId;
    @Test
    @DisplayName("Create Mlayer Domain Success Test-201")
    public void createMlayerDomainTest(){
        JsonObject requestBody = new JsonObject()
                .put("description", "Data Models that pertain to civic domain")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/civic.png")
                .put("label", "Civic")
                .put("name", "civic");

      Response resp= given()
                .header("Content-Type", "application/json")
                .header("token", cosAdminToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/domain");
        JsonObject respJson = new JsonObject(resp.body().asString());
        JsonObject firstResult = respJson.getJsonArray("results").getJsonObject(0);
        domainId = firstResult.getString("id");
        resp.then()
                .statusCode(201)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
        // Write domainId to a JSON file
        JsonObject json = new JsonObject().put("domainId", domainId);
        try (FileWriter file = new FileWriter("configDomain.json")) {
            file.write(json.encodePrettily());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Test
    @DisplayName("Create Mlayer Domain with Invalid Schema Test-400")

    public void createMlayerDomainWithInvalidSchemaTest(){
       // String domainId="6aeed588-a28e-4c17-9694-edfa24e7d227";
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
        //String domainId="6aeed588-a28e-4c17-9694-edfa24e7d227";
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
