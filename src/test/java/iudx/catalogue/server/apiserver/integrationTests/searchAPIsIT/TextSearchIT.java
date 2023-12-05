package iudx.catalogue.server.apiserver.integrationTests.searchAPIsIT;

import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the search APIs by text in the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class TextSearchIT {
    @Test
    @DisplayName("testing text search - 200 Success")
    void GetTextSearch() {
        given()
                .param("q","paid parking")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing text search special character - 200 Success")
    void GetTextSearchSpecialChar() {
        given()
                .param("q","flood*")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing text search special character - 400 Invalid value")
    void GetTextSearchSpecialCharInvalid() {
        given()
                .param("q",".\"dss$%fdd")
                .param("\"","")
                .param("limit",50)
                .param("offset",100)
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing text search - 400 Invalid value")
    void GetTextSearchInvalid() {
        given()
                .param("abc123","text to search")
                .param("limit",50)
                .param("offset",100)
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing text search string size - 400 Invalid request")
    void GetTextSearchInvalidSearchStrSize() {
        given()
                .param("q","Goliber Square Sivaji chowk Maharashtra near Railway station aqm pollution sensor iudx iudxadmin resource")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing text search Response filter - 400 Invalid request")
    void GetTextSearchInvalidRespFilter() {
        given()
                .param("q","Bosch Climo")
                .param("filter","[id,name,tags,deviceId,resourceServer,provider,location,address,type,itemStatus,authServerInfo]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing text search Exceed limit value - 400 Invalid request")
    void GetTextSearchExceedLimitVal() {
        given()
                .param("q","Bosch Climo")
                .param("limit",1000000)
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing text search Exceed offset value - 400 Invalid request")
    void GetTextSearchExceedOffsetVal() {
        given()
                .param("q","Bosch Climo")
                .param("limit",100)
                .param("offset",100000)
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
}
