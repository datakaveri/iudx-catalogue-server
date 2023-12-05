package iudx.catalogue.server.apiserver.integrationTests.relationshipAPIsIT;

import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the Forward Relationships APIs in the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class ForwardRelationshipsIT {
    @Test
    @DisplayName("testing get cos item for resource - 200 Success")
    void GetCosForResource() {
        given()
                .queryParam("id","3897a41c-83f7-37e7-9194-374d5278dff5")
                .queryParam("rel","cos")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get RS item for resource - 200 Success")
    void GetRSForResource() {
        given()
                .queryParam("id","3897a41c-83f7-37e7-9194-374d5278dff5")
                .queryParam("rel","resourceServer")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get provider item for resource - 200 Success")
    void GetProviderForResource() {
        given()
                .queryParam("id","3897a41c-83f7-37e7-9194-374d5278dff5")
                .queryParam("rel","provider")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get RG item for resource - 200 Success")
    void GetRGForResource() {
        given()
                .queryParam("id","3897a41c-83f7-37e7-9194-374d5278dff5")
                .queryParam("rel","resourceGroup")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get cos item for resource group - 200 Success")
    void GetCosForResourceGroup() {
        given()
                .queryParam("id","e63b756a-1c26-3a0c-8bbf-aeed53e423db")
                .queryParam("rel","cos")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get RS item for resource group - 200 Success")
    void GetRSForResourceGroup() {
        given()
                .queryParam("id","e63b756a-1c26-3a0c-8bbf-aeed53e423db")
                .queryParam("rel","resourceServer")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get provider item for resource group - 200 Success")
    void GetProviderForResourceGroup() {
        given()
                .queryParam("id","e63b756a-1c26-3a0c-8bbf-aeed53e423db")
                .queryParam("rel","provider")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get cos item for provider - 200 Success")
    void GetCosForProvider() {
        given()
                .queryParam("id","dec308e5-bc50-3671-af18-7f89ec33564b")
                .queryParam("rel","cos")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get resource item for provider - 200 Success")
    void GetResourceItemForProvider() {
        given()
                .queryParam("id","dec308e5-bc50-3671-af18-7f89ec33564b")
                .queryParam("rel","resourceServer")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get cos item for resource server - 200 Success")
    void GetCosForRS() {
        given()
                .queryParam("id","f3061e6c-9639-321d-a532-829c7bb870aa")
                .queryParam("rel","cos")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
}
