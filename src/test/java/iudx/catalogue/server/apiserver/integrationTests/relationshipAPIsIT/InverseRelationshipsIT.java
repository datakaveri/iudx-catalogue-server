package iudx.catalogue.server.apiserver.integrationTests.relationshipAPIsIT;

import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the Inverse Relationships APIs in the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class InverseRelationshipsIT {
    @Test
    @DisplayName("testing get resources for resource group - 200 Success")
    void GetResourcesForRG() {
        given()
                .queryParam("id","e63b756a-1c26-3a0c-8bbf-aeed53e423db")
                .queryParam("rel","resource")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get Resource for provider - 200 Success")
    void GetResourcesForProvider() {
        given()
                .queryParam("id","dec308e5-bc50-3671-af18-7f89ec33564b")
                .queryParam("rel","resource")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get RGs for provider - 200 Success")
    void GetRGsForProvide() {
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
    @DisplayName("testing get resources for resource server - 200 Success")
    void GetResourceForRS() {
        given()
                .queryParam("id","dec308e5-bc50-3671-af18-7f89ec33564b")
                .queryParam("rel","resource")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get RGs for resource server - 200 Success")
    void GetRGsForRS() {
        given()
                .queryParam("id","f3061e6c-9639-321d-a532-829c7bb870aa")
                .queryParam("rel","resourceGroup")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get providers for resource server - 200 Success")
    void GetProvidersForRS() {
        given()
                .queryParam("id","f3061e6c-9639-321d-a532-829c7bb870aa")
                .queryParam("rel","provider")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get resources for cos - 200 Success")
    void GetResourcesForCos() {
        given()
                .queryParam("id","637e32b6-9a6c-396f-914c-9db5d1a222b0")
                .queryParam("rel","resource")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get RGs for cos - 200 Success")
    void GetRGsForCos() {
        given()
                .queryParam("id","637e32b6-9a6c-396f-914c-9db5d1a222b0")
                .queryParam("rel","resourceGroup")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get providers for cos - 200 Success")
    void GetProvidersForCos() {
        given()
                .queryParam("id","637e32b6-9a6c-396f-914c-9db5d1a222b0")
                .queryParam("rel","provider")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get RSs for cos - 200 Success")
    void GetRSsForCos() {
        given()
                .queryParam("id","637e32b6-9a6c-396f-914c-9db5d1a222b0")
                .queryParam("rel","resourceServer")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
}
