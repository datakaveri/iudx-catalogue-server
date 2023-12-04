package iudx.catalogue.server.apiserver.integrationtests.ratingapis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.consumerToken;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfiguration.class)
public class DeleteRatingIT {
    @Test
    @DisplayName("Delete Rating Success Response Test-200")
    public void deleteRatingSuccessTest() {
        basePath="";
        given()
                .queryParam("id","9fb2d1b5-0db7-40b7-8efc-4bb283ee1301")
                .header("Content-Type", "application/json")
                .header("token", consumerToken)
                .when()
                .delete("/consumer/ratings")
                .then()
                .statusCode(200)
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Delete Rating With Invalid Token Test-401")
    public void deleteRatingWithInvalidTokenTest() {
        basePath="";
        given()
                .queryParam("id","9fb2d1b5-0db7-40b7-8efc-4bb283ee1301")
                .header("Content-Type", "application/json")
                .header("token", "abc")
                .when()
                .delete("/consumer/ratings")
                .then()
                .statusCode(401)
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));
    }

}
