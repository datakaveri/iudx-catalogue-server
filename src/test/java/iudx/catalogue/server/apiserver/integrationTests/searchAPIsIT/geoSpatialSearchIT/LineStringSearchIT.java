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
 * Integration tests for the search APIs by line string in the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class LineStringSearchIT {
    private static final Logger LOGGER = LogManager.getLogger(LineStringSearchIT.class);
    @Test
    @DisplayName("testing search based on line string - 200 Success")
    void GetSearchLineString() {
        Response response = given()
                .param("geoproperty","location")
                .param("georel","intersects")
                .param("geometry","LineString")
                .param("coordinates","[[73.874537,18.528311],[73.82772,18.488375],[73.876484,18.525007]]")
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
    @DisplayName("testing search based on line string - 400 Invalid value 1")
    void GetSearchLineStringInvVal1() {
        given()
                .param("geoproperty","abclocation")
                .param("georel","intersects")
                .param("geometry","LineString")
                .param("coordinates","[[73.874537,18.528311],[73.82772,18.488375],[73.876484,18.525007]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidGeoValue"));
    }
    @Test
    @DisplayName("testing search based on line string - 400 Invalid value 2")
    void GetSearchLineStringInvVal2() {
        given()
                .param("geoproperty","location")
                .param("georel","abcintersects")
                .param("geometry","LineString")
                .param("coordinates","[[73.874537,18.528311],[73.82772,18.488375],[73.876484,18.525007]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidGeoValue"));
    }
    @Test
    @DisplayName("testing search based on line string - 400 Invalid value 3")
    void GetSearchLineStringInvVal3() {
        given()
                .param("geoproperty","location")
                .param("georel","intersects")
                .param("geometry","LineString")
                .param("coordinates","[[abc73.874537,18.528311],[73.82772,18.488375],[73.876484,18.525007]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on line string - 400 Invalid syntax 1")
    void GetSearchLineStringInvSynt1() {
        given()
                .param("abcgeoproperty","location")
                .param("georel","intersects")
                .param("geometry","LineString")
                .param("coordinates","[[73.874537,18.528311],[73.82772,18.488375],[73.876484,18.525007]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on line string - 400 Invalid syntax 2")
    void GetSearchLineStringInvSynt2() {
        given()
                .param("geoproperty","location")
                .param("abcgeorel","intersects")
                .param("geometry","LineString")
                .param("coordinates","[[73.874537,18.528311],[73.82772,18.488375],[73.876484,18.525007]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on line string - 400 Invalid syntax 3")
    void GetSearchLineStringInvSynt3() {
        given()
                .param("geoproperty","location")
                .param("georel","intersects")
                .param("geometry","LineString")
                .param("abccoordinates","[[73.874537,18.528311],[73.82772,18.488375],[73.876484,18.525007]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing search based on line string - 400 Invalid Request")
    void GetSearchLineStringPrecisionValidation() {
        given()
                .param("geoproperty","location")
                .param("georel","intersects")
                .param("geometry","LineString")
                .param("coordinates","[[73.8745374,18.528311],[73.836808,18.572797],[73.876484,18.525007]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing search based on line string - 400 Invalid Request")
    void GetSearchLineStringCoordinatePair() {
        given()
                .param("geoproperty","location")
                .param("georel","intersects")
                .param("geometry","LineString")
                .param("coordinates","[[[75.9,14.5],[72,13],[73,20],[76.9,14.5],[76.9,14.5],[72,13],[73,20],[71.9,14.5],[79.9,14.5],[72,13],[73,20],[76.9,14.5],[71.9,14.5],[72,13],[73,20],[75.9,14.5]]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing search based on line string - 400 Invalid Request")
    void GetSearchLineStringCoordinateInfCheck() {
        given()
                .param("geoproperty","location")
                .param("georel","intersects")
                .param("geometry","LineString")
                .param("coordinates","[[732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123732321414141312354253636345241312373232141414131235425363634524131237323214141413123542536363452413123.927285,18.528311],[73.836808,18.572797],[73.876484,18.525007]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
}
