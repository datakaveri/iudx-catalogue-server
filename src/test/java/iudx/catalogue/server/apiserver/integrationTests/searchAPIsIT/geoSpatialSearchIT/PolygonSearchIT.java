package iudx.catalogue.server.apiserver.integrationTests.searchAPIsIT.geoSpatialSearchIT;

import io.restassured.response.Response;
import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the search APIs by polygon search in the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class PolygonSearchIT {
    private static final Logger LOGGER = LogManager.getLogger(PolygonSearchIT.class);
    @Test
    @DisplayName("testing search based on polygon - 200 Success")
    void GetSearchPolygon() {
        Response response = given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry","Polygon")
                .param("coordinates","[[[73.696,18.592],[73.69079,18.391],[73.96,18.3643],[74.09,18.526],[73.8947,18.689830],[73.696,18.592]]]")
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
    @DisplayName("testing search based on polygon - 400 Invalid value 1")
    void GetSearchPolygonInvVal1() {
        given()
                .param("geoproperty","abclocation")
                .param("georel","within")
                .param("geometry","Polygon")
                .param("coordinates","[[[73.696,18.592],[73.69079,18.391],[73.96,18.3643],[74.09,18.526],[73.8947,18.689830],[73.696,18.592]]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidGeoValue"));
    }
    @Test
    @DisplayName("testing search based on polygon - 400 Invalid value 2")
    void GetSearchPolygonInvVal2() {
        given()
                .param("geoproperty","location")
                .param("georel","1within")
                .param("geometry","Polygon")
                .param("coordinates","[[[73.696,18.592],[73.69079,18.391],[73.96,18.3643],[74.09,18.526],[73.8947,18.689830],[73.696,18.592]]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidGeoValue"));
    }
    @Test
    @DisplayName("testing search based on polygon - 400 Invalid value 3")
    void GetSearchPolygonInvVal3() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry","Polygon")
                .param("coordinates","[[[abc73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066],[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],[73.89472961425781,18.689830007518434],[73.69697570800781,18.592236436157137]]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on polygon - 400 Invalid syntax 1")
    void GetSearchPolygonInvSynt1() {
        given()
                .param("abcgeoproperty","location")
                .param("georel","within")
                .param("geometry","Polygon")
                .param("coordinates","[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066],[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],[73.89472961425781,18.689830007518434],[73.69697570800781,18.592236436157137]]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on polygon - 400 Invalid syntax 2")
    void GetSearchPolygonInvSynt2() {
        given()
                .param("geoproperty","location")
                .param("abcgeorel","within")
                .param("geometry","Polygon")
                .param("coordinates","[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066],[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],[73.89472961425781,18.689830007518434],[73.69697570800781,18.592236436157137]]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on polygon - 400 Invalid syntax 3")
    void GetSearchPolygonInvSynt3() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry","Polygon")
                .param("abccoordinates","[[[73.69697570800781,18.592236436157137],[73.6907958984375,18.391017613499066],[73.96133422851562,18.364300951402384],[74.0924835205078,18.526491895773912],[73.89472961425781,18.689830007518434],[73.69697570800781,18.592236436157137]]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on polygon - 400 Invalid request")
    void GetSearchPrecisionValidation() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry","Polygon")
                .param("coordinates","[[[75.9324532,14.5],[72,13],[73,20],[75.9,14.5]]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing search based on polygon - 400 Invalid request")
    void GetSearchCoordinatesPair() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry","Polygon")
                .param("coordinates","[[[75.9,14.5],[72,13],[73,20],[76.9,14.5],[76.9,14.5],[72,13],[73,20],[71.9,14.5],[79.9,14.5],[72,13],[73,20],[76.9,14.5],[71.9,14.5],[72,13],[73,20],[75.9,14.5]]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing search based on polygon - 400 Invalid request")
    void GetSearchCoordinateInfCheck() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry","Polygon")
                .param("coordinates","[[[732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123.927285,14.5],[72,13],[73,20],[732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123.927285,14.5]]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
}
