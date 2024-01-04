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
 * Integration tests for the search APIs by Bbox in the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class BboxSearchIT {
    private static final Logger LOGGER = LogManager.getLogger(BboxSearchIT.class);
    @Test
    @DisplayName("testing search based on Bbox - 200 Success")
    void GetSearchBbox() {
        Response response = given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry","bbox")
                .param("coordinates","[[73.8120,18.5305],[73.8665,18.4126]]")
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
    @DisplayName("testing search based on bbox- 400 Invalid value 1")
    void GetSearchBboxInvVal1() {
        given()
                .param("geoproperty","location1")
                .param("georel","within")
                .param("geometry","bbox")
                .param("coordinates","[[73.874537,18.528311],[73.874537,18.528311]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidGeoValue"));
    }
    @Test
    @DisplayName("testing search based on bbox - 400 Invalid value 2")
    void GetSearchBboxInvVal2() {
        given()
                .param("geoproperty","location")
                .param("georel","withinab")
                .param("geometry","bbox")
                .param("coordinates","[[73.874537,18.528311],[73.874537,18.528311]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidGeoValue"));
    }
    @Test
    @DisplayName("testing search based on bbox - 400 Invalid value 3")
    void GetSearchBboxInvVal3() {
        given()
                .param("geoproperty","location")
                .param("georel","intersects")
                .param("geometry","bbox")
                .param("coordinates","[[sb73.874537,18.528311],[73.874537,18.528311]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on bbox - 400 Invalid syntax 1")
    void GetSearchBboxInvSynt1() {
        given()
                .param("abcgeoproperty","location")
                .param("georel","within")
                .param("geometry","bbox")
                .param("coordinates","[[73.874537,18.528311],[73.874537,18.528311]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on bbox - 400 Invalid syntax 2")
    void GetSearchBboxInvSynt2() {
        given()
                .param("geoproperty","location")
                .param("abcgeorel","within")
                .param("geometry","bbox")
                .param("coordinates","[[73.874537,18.528311],[73.874537,18.528311]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on bbox - 400 Invalid syntax 3")
    void GetSearchBboxInvSynt3() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry","bbox")
                .param("abccoordinates","[[73.874537,18.528311],[73.874537,18.528311]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on bbox - 400 Invalid Request")
    void GetSearchBboxPrecisionValidation() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry","bbox")
                .param("coordinates","[[73.84231220,18.5305],[73.8665,18.5126]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing search based on bbox - 400 Invalid Request")
    void GetSearchBboxCoordinatePair() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry","bbox")
                .param("coordinates","[[[75.9,14.5],[72,13],[73,20],[76.9,14.5],[76.9,14.5],[72,13],[73,20],[71.9,14.5],[79.9,14.5],[72,13],[73,20],[76.9,14.5],[71.9,14.5],[72,13],[73,20],[75.9,14.5]]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing search based on bbox - 400 Invalid Request")
    void GetSearchBboxCoordinateInfCheck() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry","bbox")
                .param("coordinates","[[732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123.927285,18.5305],[73.8665,18.5126]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
}
