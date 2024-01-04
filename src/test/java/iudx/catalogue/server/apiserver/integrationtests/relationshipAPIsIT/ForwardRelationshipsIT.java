package iudx.catalogue.server.apiserver.integrationtests.relationshipAPIsIT;

import io.restassured.response.Response;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final Logger LOGGER = LogManager.getLogger(ForwardRelationshipsIT.class);
    @Test
    @DisplayName("testing get cos item for resource - 200 Success")
    void GetCosForResource() {
        Response response = given()
                .queryParam("id","3897a41c-83f7-37e7-9194-374d5278dff5")
                .queryParam("rel","cos")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing get RS item for resource - 200 Success")
    void GetRSForResource() {
        Response response = given()
                .queryParam("id","3897a41c-83f7-37e7-9194-374d5278dff5")
                .queryParam("rel","resourceServer")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing get provider item for resource - 200 Success")
    void GetProviderForResource() {
        Response response = given()
                .queryParam("id","3897a41c-83f7-37e7-9194-374d5278dff5")
                .queryParam("rel","provider")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing get RG item for resource - 200 Success")
    void GetRGForResource() {
        Response response = given()
                .queryParam("id","3897a41c-83f7-37e7-9194-374d5278dff5")
                .queryParam("rel","resourceGroup")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing get cos item for resource group - 200 Success")
    void GetCosForResourceGroup() {
        Response response = given()
                .queryParam("id","e63b756a-1c26-3a0c-8bbf-aeed53e423db")
                .queryParam("rel","cos")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing get RS item for resource group - 200 Success")
    void GetRSForResourceGroup() {
        Response response = given()
                .queryParam("id","e63b756a-1c26-3a0c-8bbf-aeed53e423db")
                .queryParam("rel","resourceServer")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing get provider item for resource group - 200 Success")
    void GetProviderForResourceGroup() {
        Response response = given()
                .queryParam("id","e63b756a-1c26-3a0c-8bbf-aeed53e423db")
                .queryParam("rel","provider")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing get cos item for provider - 200 Success")
    void GetCosForProvider() {
        Response response = given()
                .queryParam("id","dec308e5-bc50-3671-af18-7f89ec33564b")
                .queryParam("rel","cos")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing get resource item for provider - 200 Success")
    void GetResourceItemForProvider() {
        Response response = given()
                .queryParam("id","dec308e5-bc50-3671-af18-7f89ec33564b")
                .queryParam("rel","resourceServer")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing get cos item for resource server - 200 Success")
    void GetCosForRS() {
        Response response = given()
                .queryParam("id","f3061e6c-9639-321d-a532-829c7bb870aa")
                .queryParam("rel","cos")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
    }
}
