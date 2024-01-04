package iudx.catalogue.server.apiserver.integrationtests.searchAPIsIT.geoSpatialSearchIT;

import io.restassured.response.Response;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the search APIs by point search in the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class PointSearchIT {
    private static final Logger LOGGER = LogManager.getLogger(PointSearchIT.class);
    @Test
    @DisplayName("testing search based on circle - 200 Success")
    void GetSearchCircle() {
        Response response = given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("maxDistance","5000")
                .param("geometry","Point")
                .param("coordinates","[73.827285,18.482712]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }
    @Test
    @DisplayName("testing search based on circle invalid value 1 - 400 Invalid request")
    void GetSearchCircleInvVal1() {
        given()
                .param("geoproperty","location")
                .param("georel","abc")
                .param("maxDistance","5000")
                .param("geometry","Point")
                .param("coordinates","[73.827285,18.482712]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidGeoValue"));
    }
    @Test
    @DisplayName("testing search based on circle invalid value 2 - 400 Invalid request")
    void GetSearchCircleInvVal2() {
        given()
                .param("geoproperty","location")
                .param("georel","intersects")
                .param("maxDistance","5000")
                .param("geometry","Point")
                .param("coordinates","[73.827285,18.482712ab]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on circle invalid value 3 - 400 Invalid Request")
    void GetSearchCircleInvVal3() {
        given()
                .param("geoproperty","abc")
                .param("georel","intersects")
                .param("maxDistance","5000")
                .param("geometry","Point")
                .param("coordinates","[73.827285,18.482712]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidGeoValue"));
    }
    @Test
    @DisplayName("testing search based on circle invalid syntax 1 - 400 Invalid Request")
    void GetSearchCircleInvSyn1() {
        given()
                .param("abcgeoproperty","location")
                .param("georel","intersects")
                .param("maxDistance","5000")
                .param("geometry","Point")
                .param("coordinates","[73.827285,18.482712]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on circle invalid syntax 2 - 400 Invalid Request")
    void GetSearchCircleInvSyn2() {
        given()
                .param("geoproperty","location")
                .param("abcgeorel","intersects")
                .param("maxDistance","5000")
                .param("geometry","Point")
                .param("coordinates","[73.827285,18.482712]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on circle invalid syntax 3 - 400 Invalid Request")
    void GetSearchCircleInvSyn3() {
        given()
                .param("geoproperty","location")
                .param("georel","intersects")
                .param("maxDistance","5000")
                .param("geometryabc","Point")
                .param("coordinates","[73.827285,18.482712]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on circle [Precision Validation] - 400 Invalid Request")
    void GetSearchCirclePrecisionValidation() {
        given()
                .param("geoproperty","location")
                .param("georel","intersects")
                .param("maxDistance","5000")
                .param("geometry","Point")
                .param("coordinates","[73.9272851,18.5027121]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing search based on circle [Coordinator pair] - 400 Invalid Request")
    void GetSearchCircleCoordinatorPair() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("maxDistance","100")
                .param("geometry","Point")
                .param("coordinates","[[[75.9,14.5],[72,13],[73,20],[76.9,14.5],[76.9,14.5],[72,13],[73,20],[71.9,14.5],[79.9,14.5],[72,13],[73,20],[76.9,14.5],[71.9,14.5],[72,13],[73,20],[75.9,14.5]]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing search based on circle [Max Distance Limit] - 400 Invalid Request")
    void GetSearchCircleMaxDist() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("maxDistance","50000")
                .param("geometry","Point")
                .param("coordinates","[73.927285,18.502712]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing search based on circle [Negative Max Distance Limit] - 400 Invalid Request")
    void GetSearchCircleNegMaxDist() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("maxDistance","-5000")
                .param("geometry","Point")
                .param("coordinates","[73.927285,18.502712]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing search based on circle [Coordinate infinity check] - 400 Invalid Request")
    void GetSearchCircleCoordInfCheck() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("maxDistance","5000")
                .param("geometry","Point")
                .param("coordinates","[732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123.927285,18.502712]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
}
