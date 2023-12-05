package iudx.catalogue.server.apiserver.integrationTests.searchAPIsIT;

import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the search APIs by tags in the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class TagSearchIT {
    @Test
    @DisplayName("testing Tag Search (filter,limit,offset) - 200 Success")
    void GetTagSearch() {
        given()
                .param("property","[tags]")
                .param("value","[[flood]]")
                .param("filter","[id,tags]")
                .param("limit",100)
                .param("offset",0)
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing Tag Search multi value - 200 Success")
    void GetTagSearchMulVal() {
        given()
                .param("property","[tags]")
                .param("value","[[parking, flood]]")
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing Tag Search 1 - 400 Invalid syntax")
    void GetTagSearchInvSyntax1() {
        given()
                .param("property","[abc]")
                .param("value","[[abc123*]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing Tag Search 2 - 400 Invalid syntax")
    void GetTagSearchInvSyntax2() {
        given()
                .param("property","[tag$]")
                .param("value","[[abc, abc123]]")
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing Tag Search response filter - 400 Invalid Request")
    void GetTagSearchResponseFilter() {
        given()
                .param("property","[tags]")
                .param("value","[[pollution]]")
                .param("filter","[id,name,tags,deviceId,resourceServer,provider,location,address,type,itemStatus,authServerInfo]")
                .param("limit",100)
                .param("offset",0)
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing Tag Search Exceed limit value - 400 Invalid Request")
    void GetTagSearchExceedLimitVal() {
        given()
                .param("property","[tags]")
                .param("value","[[pollution]]")
                .param("filter","[id,name,tags,deviceId]")
                .param("limit",1000000)
                .param("offset",0)
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing Tag Search Exceed offset value - 400 Invalid Request")
    void GetTagSearchExceedOffsetVal() {
        given()
                .param("property","[tags]")
                .param("value","[[pollution]]")
                .param("filter","[id,name,tags,deviceId]")
                .param("limit",100)
                .param("offset",1000000)
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
}
