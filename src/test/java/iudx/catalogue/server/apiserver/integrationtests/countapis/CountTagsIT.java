package iudx.catalogue.server.apiserver.integrationtests.countapis;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

/*Rest Assured Integration tests to get the total number of hits for the given API request
containing the provided query parameters in Catalogue Server APIs.
*/

@ExtendWith(RestAssuredConfiguration.class)
public class CountTagsIT {
    @Test
    @DisplayName("Get Count Tags success response test- 200")
    public void getCountTagTest() {
        given()
                .queryParam("value", "[[pollution]]")
                .queryParam("property", "[tags]")
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());

    }

    @Test
    @DisplayName("Get Count Tag With Invalid Single Attribute Value test- 200")
    public void getCountTagWithInvalidSingleAttributeValueTest() {
        given()
                .queryParam("value", "[[abc123]]")
                .queryParam("property", "[tags]")
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());

    }

    @Test
    @DisplayName("Get Count Tag With Multi Attribute Value test- 200")
    public void getCountTagWithMultiAttributeValueTest() {
        given()
                .queryParam("value", "[[pollution, flood]]")
                .queryParam("property", "[tags]")
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());

    }

    @Test
    @DisplayName("Get Count Tag With  Single Attribute Invalid Multi Value test- 200")
    public void getCountTagWithSingleAttributeInvalidMultiValueTest() {
        given()
                .queryParam("value", "[[abc, abc123]]")
                .queryParam("property", "[tags]")
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());

    }

    @Test
    @DisplayName("Get Count Tag With Invalid Property Invalid Value test- 200")
    public void getCountTagWithInvalidPropertyInvalidValueTest() {
        given()
                .queryParam("value", "[[abc123]]")
                .queryParam("property", "[abc]")
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());

    }

    @Test
    @DisplayName("Get Count Attributes test- 200")
    public void getCountAttributesTest() {
        given()
                .queryParam("value", "[[b58da193-23d9-43eb-b98a-a103d4b6103c]]")
                .queryParam("property", "[id]")
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());

    }

    @Test
    @DisplayName("Get Count Attribute SingleProperty, MultiValue- 200")
    public void getCountAttributeSinglePropertyMultiValue() {
        given()
                .queryParam("value", "[[b58da193-23d9-43eb-b98a-a103d4b6103c,af19727d-0ab8-4002-b745-ec183cb0ce60]]")
                .queryParam("property", "[id]")
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());

    }

    @Test
    @DisplayName("Get Count Attribute SingleProperty ,InvalidValue- 200")
    public void getCountAttributeSinglePropertyInvalidValue() {
        String non_existing_id = "[[a2b3c23b-ce73-41ac-a321-82206b319a51]]";
        given()
                .queryParam("value", non_existing_id)
                .queryParam("property", "[id]")
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());

    }

    @Test
    @DisplayName("Get Count Attribute SingleProperty MultiValue with InvalidValue- 200")
    public void getCountAttributeSinglePropertyMultiValuewithInvalidValue() {
        given()
                .queryParam("value", "[[af19727d-0ab8-4002-b745-ec183cb0ce90,non-existing-id]]")
                .queryParam("property", "[id]")
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());

    }

    @Test
    @DisplayName("Get Count Attribute Nested-1- 200")
    public void getCountAttributeNested1() {
        given()
                .queryParam("value", "[[aqm],[8cff12b2-b8be-1230-c5f6-ca96b4e4e441,climo]]")
                .queryParam("property", "[tags,deviceId.keyword]")
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());

    }

    @Test
    @DisplayName("Get Count Attribute Nested-2- 200")
    public void getCountAttributeNested2() {
        given()
                .queryParam("value", "[[aqm, flood], [pune,delhi]]")
                .queryParam("property", "[tags,location.address]")
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());

    }

    @Test
    @DisplayName("Count Attribute InvalidProperty (totalHits=0) - 200")
    public void getCountAttributeInvalidPropertytotalHitsEqualToZero() {
        given()
                .queryParam("value", "[[af19727d-0ab8-4002-b745-ec183cb0ce60]]")
                .queryParam("property", "[non-existing property]")
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());

    }

    @Test
    @DisplayName("Count based on Text - 200")
    public void getCountbasedOnText() {
        String text = "Bosch Climo";
        given()
                .queryParam("q", text)
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Count Text using SpecialChar[*] - 200")
    public void getCountTextusingSpecialCharStar() {
        String textWithSpecialChar = "Bosch*";
        given()
                .queryParam("q", textWithSpecialChar)
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Count Text InvalidSyntax-1 - 400")
    public void getCountTextInvalidSyntax1() {
        String invalidText=".\"dss$%fdd&\"";
        given()
                .queryParam("q", invalidText)
                .queryParam("limit",50)
                .queryParam("offset",100)
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("Count Text InvalidSyntax-2 - 400")
    public void getCountTextInvalidSyntax2() {
        String invalidText="text to search";
        given()
                .queryParam("abc123", invalidText)
                .queryParam("limit",50)
                .queryParam("offset",100)
                .header("Content-Type", "application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("Get Count Circle Point- 200")
    public void getCountCirclePoint() {
        given()
                .queryParam("geoproperty", "location")
                .queryParam("georel", "within")
                .queryParam("maxDistance", 5000)
                .queryParam("geometry", "Point")
                .queryParam("coordinates", "[73.9,18.6]")
                .header("Content-Type","application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());

    }
    @Test
    @DisplayName("Get Count Circle Point With Invalid Value- 400")
    public void getCountCirclePointWithInvalidValue() {
        given()
                .queryParam("geoproperty", "location")
                .queryParam("georel", "abc")
                .queryParam("maxDistance", 5000)
                .queryParam("geometry", "Point")
                .queryParam("coordinates", "[73.927285,18.502712]")
                .header("Content-Type","application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidGeoValue"));
    }
    @Test
    @DisplayName("Get Count Circle Point With Invalid Syntax- 400")
    public void getCountCirclePointWithInvalidSyntax() {
        given()
                .queryParam("abcgeoproperty", "location")
                .queryParam("georel", "within")
                .queryParam("maxDistance", 5000)
                .queryParam("geometry", "Point")
                .queryParam("coordinates", "[73.927285,18.502712]")
                .header("Content-Type","application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("Get Count Polygons- 200")
    public void getCountPolygons() {
        given()
                .queryParam("geoproperty", "location")
                .queryParam("georel", "within")
                .queryParam("geometry", "Polygon")
                .queryParam("coordinates","[[[73.696,18.592],[73.69079,18.391],[73.96,18.3643],[74.09,18.526],[73.8947,18.689830],[73.696,18.592]]]")
                .header("Content-Type","application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("totalHits", notNullValue());
    }
    @Test
    @DisplayName("Get Count Polygons With Invalid Value- 400")
    public void getCountPolygonsWithInvalidValue() {
        given()
                .queryParam("geoproperty", "abclocation")
                .queryParam("georel", "within")
                .queryParam("geometry", "Polygon")
                .queryParam("coordinates","[[[73.696,18.592],[73.69079,18.391],[73.96,18.3643],[74.09,18.526],[73.8947,18.689830],[73.696,18.592]]]")
                .header("Content-Type","application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidGeoValue"));
    }
    @Test
    @DisplayName("Get Count Polygons With Invalid Syntax- 400")
    public void getCountPolygonsWithInvalidSyntax() {
        given()
                .queryParam("abcgeoproperty", "location")
                .queryParam("georel", "within")
                .queryParam("geometry", "Polygon")
                .queryParam("coordinates","[[[73.696,18.592],[73.69079,18.391],[73.96,18.3643],[74.09,18.526],[73.8947,18.689830],[73.696,18.592]]]")
                .header("Content-Type","application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("Get Count Line String- 200")
    public void getCountLineString() {
        given()
                .queryParam("geoproperty", "location")
                .queryParam("georel", "intersects")
                .queryParam("geometry", "LineString")
                .queryParam("coordinates","[[73.874537,18.528311],[73.836808,18.572797],[73.876484,18.525007]]")
                .header("Content-Type","application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Get Count Line String With Invalid Value- 400")
    public void getCountLineStringWithInvalidValue() {
        given()
                .queryParam("geoproperty", "abclocation")
                .queryParam("georel", "intersects")
                .queryParam("geometry", "LineString")
                .queryParam("coordinates","[[73.874537,18.528311],[73.836808,18.572797],[73.876484,18.525007]]")
                .header("Content-Type","application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidGeoValue"));
    }
    @Test
    @DisplayName("Get Count Line String With Invalid Syntax- 400")
    public void getCountLineStringWithInvalidSyntax() {
        given()
                .queryParam("abcgeoproperty", "location")
                .queryParam("georel", "intersects")
                .queryParam("geometry", "LineString")
                .queryParam("coordinates","[[73.874537,18.528311],[73.836808,18.572797],[73.876484,18.525007]]")
                .header("Content-Type","application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("Get Count bbox- 200")
    public void getCountBbox() {
        given()
                .queryParam("geoproperty", "location")
                .queryParam("georel", "intersects")
                .queryParam("geometry", "bbox")
                .queryParam("coordinates","[[73,20],[75,18]]")
                .header("Content-Type","application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Get bbox With Invalid Value- 400")
    public void getCountBboxWithInvalidValue() {
        given()
                .queryParam("geoproperty", "location1")
                .queryParam("georel", "intersects")
                .queryParam("geometry", "bbox")
                .queryParam("coordinates","[[73,20],[75,18]]")
                .header("Content-Type","application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidGeoValue"));
    }
    @Test
    @DisplayName("Get bbox With Invalid Syntax- 400")
    public void getCountBboxWithInvalidSyntax() {
        given()
                .queryParam("geopropertyabc", "location")
                .queryParam("georel", "intersects")
                .queryParam("geometry", "bbox")
                .queryParam("coordinates","[[73,20],[75,18]]")
                .header("Content-Type","application/json")
                .when()
                .get("/count")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSyntax"));
    }

}

