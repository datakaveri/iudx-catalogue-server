package iudx.catalogue.server.apiserver.integrationtests.ratingapis;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.consumerToken;
import static org.hamcrest.Matchers.*;
@ExtendWith(RestAssuredConfiguration.class)
public class GetRatingIT {

    @Test
    @DisplayName("Get Rating Success Test-200")
    public void getRatingSuccessTest(){
        String ratingId="9fb2d1b5-0db7-40b7-8efc-4bb283ee1301";
        basePath="";
        given()
                .queryParam("id",ratingId)
                .header("token",consumerToken)
                .when()
                .get("/consumer/ratings")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Get Rating With No Content Test-204")
    public void getRatingWithNoContentTest(){
        basePath="";
        given()
                .queryParam("id","b58da193-23d9-43eb-b98a-a103d4b6103d")
                .header("token",consumerToken)
                .when()
                .get("/consumer/ratings")
                .then()
                .statusCode(204)
                .log().body();
    }
    @Test
    @DisplayName("Get All Ratings Success Test-200")
    public void getAllRatingsSuccessTest(){
        basePath="";
        given()
                .queryParam("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .queryParam("type","group")
                .when()
                .get("/consumer/ratings")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Get Average Rating Success Test-200")
    public void getAverageRatingSuccessTest(){
        basePath="";
        given()
                .queryParam("id","5b7556b5-0779-4c47-9cf2-3f209779aa22")
                .queryParam("type","average")
                .when()
                .get("/consumer/ratings")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Get Average Rating With Invalid ID Test-400")
    public void getAverageRatingWithInvalidIdTest(){
        basePath="";
        given()
                .queryParam("id","5b7556b5-0779-4c47-9cf2-3f209779aa22\\")
                .queryParam("type","average")
                .when()
                .get("/consumer/ratings")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidParamValue"));

    }
    @Test
    @DisplayName("Get Rating With Invalid Token Test-401")
    public void getRatingWithInvalidTokenTest(){
        basePath="";
        given()
                .queryParam("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .header("token","abc")
                .when()
                .get("/consumer/ratings")
                .then()
                .statusCode(401)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));
    }



}
