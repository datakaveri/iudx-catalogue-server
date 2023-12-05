package iudx.catalogue.server.apiserver.integrationTests.searchAPIsIT;

import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the search APIs by complex search in the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class ComplexSearchIT {
    @Test
    @DisplayName("testing Complex Search with geo filter - 200 Success")
    void ComplexSearchGeoFilter() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry", "Polygon")
                .param("coordinates","[[[73.696,18.592],[73.69079,18.391],[73.96,18.3643],[74.09,18.526],[73.8947,18.689830],[73.696,18.592]]]")
                .param("filter","[id, description]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing Complex Search with geo attribute - 200 Success")
    void ComplexSearchGeoAttr() {
        given()
                .param("property","[location.address]")
                .param("value","[[pune,delhi]]")
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry", "Polygon")
                .param("coordinates","[[[73.696,18.592],[73.69079,18.391],[73.96,18.3643],[74.09,18.526],[73.8947,18.689830],[73.696,18.592]]]")
                .param("filter","[id, description]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing Complex Search with geo text - 200 Success")
    void ComplexSearchGeoText() {
        given()
                .param("q","flood sensors")
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry", "Polygon")
                .param("coordinates","[[[73.696,18.592],[73.69079,18.391],[73.96,18.3643],[74.09,18.526],[73.8947,18.689830],[73.696,18.592]]]")
                .param("filter","[id, description]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing Complex Search with geo attribute filter - 200 Success")
    void ComplexSearchGeoAttrFilter() {
        given()
                .param("property","[location.address]")
                .param("value","[[pune,delhi]]")
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry", "Polygon")
                .param("coordinates","[[[73.696,18.592],[73.69079,18.391],[73.96,18.3643],[74.09,18.526],[73.8947,18.689830],[73.696,18.592]]]")
                .param("filter","[id, description]")
                .param("filter", "[tags,id,location.geometry.coordinates]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing Complex Search with geo text filter - 200 Success")
    void ComplexSearchGeoTextFilter() {
        given()
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry", "Polygon")
                .param("coordinates","[[[73.696,18.592],[73.69079,18.391],[73.96,18.3643],[74.09,18.526],[73.8947,18.689830],[73.696,18.592]]]")
                .param("q","flood sensors")
                .param("filter","[tags,id,location.geometry.coordinates]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing Complex Search with attribute filter - 200 Success")
    void ComplexSearchAttributeFilter() {
        given()
                .param("property","[location.address]")
                .param("value","[[pune,delhi]]")
                .param("filter","[tags,id,location.geometry.coordinates]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing Complex Search with attribute text - 200 Success")
    void ComplexSearchAttributeText() {
        given()
                .param("property","[location.address]")
                .param("value","[[pune,delhi]]")
                .param("q","flood sensors")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing Complex Search with attribute text filter - 200 Success")
    void ComplexSearchAttributeTextFilter() {
        given()
                .param("property","[location.address]")
                .param("value","[[pune,delhi]]")
                .param("q","flood sensors")
                .param("filter","[tags,id,location.geometry.coordinates]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing Complex Search with attribute tags filter - 200 Success")
    void ComplexSearchAttributeTagsFilter() {
        given()
                .param("property","[tags]")
                .param("value","[[parking]]")
                .param("filter","[tags,id,location.geometry.coordinates]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing Complex Search with attribute text filter - 200 Success")
    void ComplexSearchAttributeTextFilter2() {
        given()
                .param("q","paid parking")
                .param("filter","[tags,id,location.geometry.coordinates]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing Complex Search with geo text attribute - 200 Success")
    void ComplexSearchGeoTextAttribute() {
        given()
                .param("property","[location.address]")
                .param("value","[[pune,delhi]]")
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry","Polygon")
                .param("coordinates","[[[73.696,18.592],[73.69079,18.391],[73.96,18.3643],[74.09,18.526],[73.8947,18.689830],[73.696,18.592]]]")
                .param("q","sens data")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing Complex Search with geo text attribute filter - 200 Success")
    void ComplexSearchGeoTextAttributeFilter() {
        given()
                .param("property","[location.address]")
                .param("value","[[pune,delhi]]")
                .param("geoproperty","location")
                .param("georel","within")
                .param("geometry","Polygon")
                .param("coordinates","[[[73.696,18.592],[73.69079,18.391],[73.96,18.3643],[74.09,18.526],[73.8947,18.689830],[73.696,18.592]]]")
                .param("q","sens data")
                .param("filter","[tags,id,location.geometry.coordinates]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing complex search Response filter- 400 Invalid request")
    void ComplexSearchResponseFilter(){
        given()
                .param("geoproperty", "location")
                .param("georel","within")
                .param("geometry", "Polygon")
                .param("coordinates","[[[75.9,14.5],[72,13],[73,20],[75.9,14.5]]]")
                .param("filter","[id,name,tags,deviceId,resourceServer,provider,location,address,type,itemStatus,authServerInfo]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing complex search Exceed limit value - 400 Invalid request")
    void ComplexSearchExceedLimitVal(){
        given()
                .param("geoproperty", "location")
                .param("georel","within")
                .param("geometry", "Polygon")
                .param("coordinates","[[[75.9,14.5],[72,13],[73,20],[75.9,14.5]]]")
                .param("filter","[id,name]")
                .param("limit",10001)
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing complex search Exceed offset value - 400 Invalid request")
    void ComplexSearchExceedOffsetVal(){
        given()
                .param("geoproperty", "location")
                .param("georel","within")
                .param("geometry", "Polygon")
                .param("coordinates","[[[75.9,14.5],[72,13],[73,20],[75.9,14.5]]]")
                .param("filter","[id,name]")
                .param("limit",101)
                .param("offset",1010101)
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
}
