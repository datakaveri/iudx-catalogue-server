package iudx.catalogue.server.apiserver.integrationtests.geocodingapis;

import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/* Rest Assured Integration tests to list the location of the dataset ids'
given in the request body of a particular instance in Catalogue Server APIs
*/

@ExtendWith(RestAssuredConfiguration.class)
public class GetGeoLocationsIT {
    String geoValue="malleswaram";
    String inValidgeoValue="++";
    @Test
    @DisplayName("Get All Geo Locations Test-200")
    public void getAllGeoLocationsTest(){
        given()
                .queryParam("q",geoValue)
                .header("Content-Type","application/json")
                .when()
                .get("/geo")
                .then()
                .statusCode(200)
                .log().body()
                .body("type",equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Location Not Found Test-400")
    public void locationNotFound(){
        given()
                .queryParam("q","")
                .header("Content-Type","application/json")
                .when()
                .get("/geo")
                .then()
                .statusCode(400)
                .log().body()
                .body("type",equalTo("urn:dx:cat:ItemNotFound"));
    }
    @Test
    @DisplayName("Get Geo Location with invalid query parameter Test-400")
    public void getGeoLocationWithInvalidQueryParameterTest(){
        given()
                .queryParam("p",geoValue)
                .header("Content-Type","application/json")
                .when()
                .get("/geo")
                .then()
                .statusCode(400)
                .log().body()
                .body("type",equalTo("urn:dx:cat:InvalidParamValue"));
    }
    @Test
    @DisplayName("Get Geo Location with invalid geo value Test-400")
    public void getGeoLocationWithInvalidGeoValueTest(){
        given()
                .queryParam("q",inValidgeoValue)
                .header("Content-Type","application/json")
                .when()
                .get("/geo")
                .then()
                .statusCode(400)
                .log().body()
                .body("type",equalTo("urn:dx:cat:InvalidGeoValue"));
    }

}
