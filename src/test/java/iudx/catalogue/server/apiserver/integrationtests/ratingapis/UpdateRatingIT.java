package iudx.catalogue.server.apiserver.integrationtests.ratingapis;

import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.*;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.consumerToken;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfiguration.class)
public class UpdateRatingIT {
    String ratingId="9fb2d1b5-0db7-40b7-8efc-4bb283ee1301";
    @Test
    @DisplayName("Update rating success response test- 200")
    public void updateRatingSuccessTest(){
        basePath="";
        //request body
        JsonObject requestBody = new JsonObject()
                .put("rating", 4.8)
                .put("comment", "very good resource");
        given()
               .queryParam("id",ratingId)
                .header("Content-Type","application/json")
                .header("token",consumerToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/consumer/ratings")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));

    }
    @Test
    @DisplayName("Update rating with Invalid Token Test- 401")
    public void updateRatingWithInvalidTokenTest(){
        basePath="";
        //request body
        JsonObject requestBody = new JsonObject()
                .put("rating", 4.8)
                .put("comment", "very good resource");
        given()
                .queryParam("id",ratingId)
                .header("Content-Type","application/json")
                .header("token","abc")
                .body(requestBody.encodePrettily())
                .when()
                .put("/consumer/ratings")
                .then()
                .statusCode(401)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));

    }
    @Test
    @DisplayName("Update rating with Invalid Schema Test- 400")
    public void updateRatingWithInvalidSchemaTest(){
        basePath="";
        //request body
        JsonObject requestBody = new JsonObject()
                .put("ratingzzz", 4.8)
                .put("comment", "v.good resource");

        given()
                .queryParam("id",ratingId)
                .header("Content-Type","application/json")
                .header("token",consumerToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/consumer/ratings")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));

    }


}
