package iudx.catalogue.server.apiserver.integrationTests.listItemsIT;

import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the List Items APIs in the Catalog Server.
 * Note: These tests assume the availability of the required authentication tokens and valid
 * configurations for the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class ListItemsIT {
    @Test
    @DisplayName("testing list tags - 200 Success")
    void ListTags() {
        given()
                .when()
                .get("/list/tags")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing list instances - 200 Success")
    void ListInstances() {
        given()
                .when()
                .get("/list/instance")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing list Resource Group - 200 Success")
    void ListResourceGroup() {
        given()
                .when()
                .get("/list/resourceGroup")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing list Resource Server - 200 Success")
    void ListResourceServer() {
        given()
                .when()
                .get("/list/resourceServer")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing list Provider - 200 Success")
    void ListProvider() {
        given()
                .when()
                .get("/list/provider")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing list Tags - 400 Invalid item type")
    void ListInvalidTags() {
        given()
                .when()
                .get("/list/tag")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing list Instances - 400 Invalid item type")
    void ListInvalidInstance() {
        given()
                .when()
                .get("/list/instanc")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing list Resource Group - 400 Invalid item type")
    void ListInvalidResourceGrp() {
        given()
                .when()
                .get("/list/resourceGrp")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing list Resource Server - 400 Invalid item type")
    void ListInvalidResourceSvr() {
        given()
                .when()
                .get("/list/resourceSvr")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing list Provider - 400 Invalid item type")
    void ListInvalidProvider() {
        given()
                .when()
                .get("/list/rprovider")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing list t- 400 Invalid item type")
    void ListInvalidItemType() {
        given()
                .when()
                .get("/list/resource")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing Exceed limit value - 400 Invalid request")
    void ListTagsExceedLimit() {
        given()
                .queryParam("limit","13323232320")
                .when()
                .get("/list/tags")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing List COS - 200 Success")
    void ListCOS() {
        given()
                .when()
                .get("/list/cos")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing List Owner - 200 Success")
    void ListOwner() {
        given()
                .when()
                .get("/list/owner")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing List COS - 400 Invalid Item Type")
    void ListCOSInvalid() {
        given()
                .when()
                .get("/list/coss")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing List Owner - 400 Invalid Item Type")
    void ListOwnerInvalid() {
        given()
                .when()
                .get("/list/rowner")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
}
