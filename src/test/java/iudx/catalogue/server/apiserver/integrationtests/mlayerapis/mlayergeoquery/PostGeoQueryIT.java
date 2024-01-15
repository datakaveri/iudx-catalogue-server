package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayergeoquery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/* Rest Assured Integration tests to get location of all
resource groups whose id(s) are provided in Catalogue Middle layer specific APIs. */

@ExtendWith(RestAssuredConfiguration.class)
public class PostGeoQueryIT {
    @Test
    @DisplayName("Post Mlayer Geo Query Success Test-200")
    public void postMlayerGeoQueryTest(){
        JsonObject requestBody = new JsonObject()
                .put("id", new JsonArray().add("8b95ab80-2aaf-4636-a65e-7f2563d0d371").add("83c2e5c2-3574-4e11-9530-2b1fbdfce832"))
                .put("instance", "surat");
        given()
                .header("Content-Type","application/json")
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/geoquery")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Post Mlayer Geo Query With Invalid Schema Test-400")
    public void postMlayerGeoQueryWithInvalidSchemaTest(){
        JsonObject requestBody = new JsonObject()
                .put("id", new JsonArray().add("8b95ab80-2aaf-4636-a65e-7f2563d0d371").add("8b95ab80-2aaf-4636-a65e-7f2563d0d371").add("8b95ab80-2aaf-4636-a65e0d371"))
                .put("instance", "surat");

        given()
                .header("Content-Type","application/json")
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/geoquery")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));
    }

}
