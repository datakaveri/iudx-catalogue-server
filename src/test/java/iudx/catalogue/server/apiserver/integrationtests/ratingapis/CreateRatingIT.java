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
public class CreateRatingIT {
    String ratingId="9fb2d1b5-0db7-40b7-8efc-4bb283ee1301";
    @Test
    @DisplayName("Create Rating Success Response-201")
    public void createRatingSuccessTest() {
        basePath="";
        // Request Body
        JsonObject requestBody = new JsonObject()
                .put("rating", 4.8)
                .put("comment", "good resource");

        given()
                .queryParam("id",ratingId)
                .header("Content-Type", "application/json")
                .header("token", consumerToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/consumer/ratings")
                .then()
                .statusCode(201)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("results[0].id", notNullValue());
    }
    @Test
    @DisplayName("Create rating with invalid token test- 401")
    public void createRatingWithInvalidTokenTest() {
        basePath="";
        // Request Body
        JsonObject requestBody = new JsonObject()
                .put("rating", 4.8)
                .put("comment", "v.good resource");

        given()
                .queryParam("id",ratingId)
                .header("Content-Type", "application/json")
                .header("token", "abc")
                .body(requestBody.encodePrettily())
                .when()
                .post("/consumer/ratings")
                .then()
                .statusCode(401)// Expecting a 401 Unauthorized response status
                .log().body();

    }
    @Test
    @DisplayName("Create rating with invalid schema test- 400")
    public void createRatingWithInvalidSchemaTest(){
        basePath="";
        //Request Body
        JsonObject requestBody = new JsonObject()
                .put("ratingzzz", 4.8);
        given()
                .queryParam("id",ratingId)
                .header("Content-Type","application/json")
                .header("token",consumerToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/consumer/ratings")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));
    }
    @Test
    @DisplayName("Create rating with invalid access count test-400")
    public void createRatingWithInvalidAccessCount(){
        basePath="";
        //request body
        JsonObject requestBody = new JsonObject()
                .put("rating", 4.8)
                .put("comment", "good resource");
        given()
                .queryParam("id",ratingId)
                .header("Content-Type","application/json")
                .header("token",consumerToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/consumer/ratings")
                .then()
                .statusCode(400)
                .log().body();
    }
}
