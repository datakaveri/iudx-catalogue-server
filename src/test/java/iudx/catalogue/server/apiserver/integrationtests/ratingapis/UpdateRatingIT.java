package iudx.catalogue.server.apiserver.integrationtests.ratingapis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.*;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.consumerToken;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfiguration.class)
public class UpdateRatingIT {
    @Test
    @DisplayName("Update rating success response test- 200")
    public void updateRatingSuccessTest(){
        basePath="";
        //request body
        String requestBody = "{\"rating\": 4.8,\"comment\": \"very good resource\"}";
        given()
               .queryParam("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .header("Content-Type","application/json")
                .header("token",consumerToken)
                .body(requestBody)
                .when()
                .put("/consumer/ratings")
                .then()
                .statusCode(200)
                .body("type", equalTo("urn:dx:cat:Success"));

    }
    @Test
    @DisplayName("Update rating with Invalid Token Test- 401")
    public void updateRatingWithInvalidTokenTest(){
        basePath="";
        //request body
        String requestBody = "{\"rating\": 4.8,\"comment\": \"very good resource\"}";
        given()
                .queryParam("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .header("Content-Type","application/json")
                .header("token","abc")
                .body(requestBody)
                .when()
                .put("/consumer/ratings")
                .then()
                .statusCode(401)
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));

    }
    @Test
    @DisplayName("Update rating with Invalid Schema Test- 400")
    public void updateRatingWithInvalidSchemaTest(){
        basePath="";
        //request body
        String requestBody ="{\"ratingzzz\": 4.8,\"comment\": \"v.good resource\"}";
        given()
                .queryParam("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .header("Content-Type","application/json")
                .header("token",consumerToken)
                .body(requestBody)
                .when()
                .put("/consumer/ratings")
                .then()
                .statusCode(400)
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));

    }


}
