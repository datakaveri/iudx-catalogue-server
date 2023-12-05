package iudx.catalogue.server.apiserver.integrationTests.relationshipAPIsIT;

import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the Total Inverse Relationships APIs in the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class TotalInverseRelationshipsIT {
    @Test
    @DisplayName("testing get total inverse relationships[Resource] - 200 Success")
    void GetTotalInvRelResources() {
        given()
                .queryParam("id","3897a41c-83f7-37e7-9194-374d5278dff5")
                .queryParam("rel","all")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get total inverse relationships[Resource Group] - 200 Success")
    void GetTotalInvRelRG() {
        given()
                .queryParam("id","e63b756a-1c26-3a0c-8bbf-aeed53e423db")
                .queryParam("rel","all")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get total inverse relationships[Provider] - 200 Success")
    void GetTotalInvRelProvider() {
        given()
                .queryParam("id","dec308e5-bc50-3671-af18-7f89ec33564b")
                .queryParam("rel","all")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get total inverse relationships[Resource Server] - 200 Success")
    void GetTotalInvRelResourceServer() {
        given()
                .queryParam("id","f3061e6c-9639-321d-a532-829c7bb870aa")
                .queryParam("rel","all")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing get total inverse relationships[COS] - 400 Invalid item type")
    void GetTotalInvRelCos() {
        given()
                .queryParam("id","637e32b6-9a6c-396f-914c-9db5d1a222b0")
                .queryParam("rel","all")
                .when()
                .get("/relationship")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidRelationSearch"));
    }
    @Test
    @DisplayName("testing get relationship resource server - 400 Invalid Resource Group")
    void GetRelRSInvalid() {
        given()
                .queryParam("id","5b7556b5-0779-4c47-9cf2-3f209779aa21")
                .queryParam("rel","resourceServer")
                .when()
                .get("/relationship")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:ItemNotFound"));
    }
    @Test
    @DisplayName("testing get relationship provider - 400 Invalid Resource Id")
    void GetRelProviderInvalid() {
        given()
                .queryParam("id","b58da193-23d9-43eb-b98a-a103d4b6103f")
                .queryParam("rel","provider")
                .when()
                .get("/relationship")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:ItemNotFound"));
    }
    @Test
    @DisplayName("testing get relationship1 RG - 400 Invalid value")
    void GetRelRGInvalid() {
        given()
                .queryParam("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .queryParam("rel","resourceGrp")
                .when()
                .get("/relationship")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidParamValue"));
    }
    @Test
    @DisplayName("testing get relationship2 Resource - 400 Invalid value")
    void GetRelResourceInvalid() {
        given()
                .queryParam("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .queryParam("rel","resrce")
                .when()
                .get("/relationship")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidParamValue"));
    }
    @Test
    @DisplayName("testing get relationship3 Resource - 400 Invalid value")
    void GetRelResourceInvalid3() {
        given()
                .queryParam("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .queryParam("rel1","resource")
                .when()
                .get("/relationship")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:MissingParams"));
    }
    @Test
    @DisplayName("testing get relationship4 RS - 400 Invalid syntax")
    void GetRelResourceInvalid4() {
        given()
                .queryParam("i1d","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .queryParam("rel","resourceServer")
                .when()
                .get("/relationship")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:MissingParams"));
    }
    @Test
    @DisplayName("testing get relationship Response filter - 400 Invalid request")
    void GetRelResponseFilter() {
        given()
                .queryParam("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .queryParam("rel","resourceGroup")
                .queryParam("filter","[id,name,tags,deviceId,resourceServer,provider,location,address,type,itemStatus,authServerInfo]")
                .when()
                .get("/relationship")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
    @Test
    @DisplayName("testing get relationship Exceed limit value - 400 Invalid request")
    void GetRelExceedLimit() {
        given()
                .queryParam("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .queryParam("rel","resourceGroup")
                .queryParam("limit","1000001")
                .when()
                .get("/relationship")
                .then()
                .statusCode(400)
                .body("title", is("failed"));
    }
}
