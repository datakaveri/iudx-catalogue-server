package iudx.catalogue.server.apiserver.integrationtests.ratingapis;

import iudx.catalogue.server.apiserver.integrationtests.ratingapis.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.consumerToken;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfiguration.class)
public class CreateRatingIT {

    @Test
    @DisplayName("Create Rating Success Response-201")
    public void createRatingSuccessTest() {
        basePath="";
        // Request Body
        String requestBody = "{\"rating\": 4.8,\"comment\": \"good resource\"}";

        given()
                .queryParam("id","9fb2d1b5-0db7-40b7-8efc-4bb283ee1301")
                .header("Content-Type", "application/json")
                .header("token", consumerToken)
                .body(requestBody)
                .when()
                .post("/consumer/ratings")
                .then()
                .statusCode(201)
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("results[0].id", notNullValue());
    }
    @Test
    @DisplayName("Create rating with invalid token test- 401")
    public void createRatingWithInvalidTokenTest() {
        basePath="";
        // Request Body
        String requestBody = "{\"rating\": 4.8,\"comment\": \"v.good resource\"}";

        given()
                .queryParam("id","bec385a2-470e-4a57-91f2-7b839a58a4c5")
                .header("Content-Type", "application/json")
                .header("token", "abc")
                .body(requestBody)
                .when()
                .post("/consumer/ratings")
                .then()
                .statusCode(401); // Expecting a 401 Unauthorized response status

    }
    @Test
    @DisplayName("Create rating with invalid schema test- 400")
    public void createRatingWithInvalidSchemaTest(){
        basePath="";
        //Request Body
        String requestBody ="{\"ratingzzz\": 4.8}";
        given()
                .queryParam("id","aaeffb80-9226-4400-814e-1784bc2061f1")
                .header("Content-Type","application/json")
                .header("token",consumerToken)
                .body(requestBody)
                .when()
                .post("/consumer/ratings")
                .then()
                .statusCode(400)
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));
    }
    @Test
    @DisplayName("Create rating with invalid access count test-400")
    public void createRatingWithInvalidAccessCount(){
        basePath="";
        //request body
        String requestBody = "{\"rating\": 4.8, \"comment\": \"good resource\"}";
        given()
                .queryParam("id","ec454309-3211-4d9e-b502-1879dca673a1")
                .header("Content-Type","application/json")
                .header("token",consumerToken)
                .body(requestBody)
                .when()
                .post("/consumer/ratings")
                .then()
                .statusCode(400);
    }
}
