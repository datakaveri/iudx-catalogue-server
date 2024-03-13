package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerproviders;

import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/*Rest Assured Integration tests for listing all the providers in
Catalogue Middle layer specific APIs*/

@ExtendWith(RestAssuredConfiguration.class)
public class GetProvidersIT {
    @Test
    @Order(1)
    @DisplayName("Get Mlayer Providers Success Test-200")
    public void getProvidersTest(){
        given()
                .when()
                .get("/internal/ui/providers")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type",equalTo("urn:dx:cat:Success"));
    }

    @Test
    @Order(2)
    @DisplayName("Get Mlayer Provider by Limit and Offset Test-200")
    public void getMlayerProviderByLimitAndOffset() {
        given()
                .param("limit",10)
                .param("offset", 0)
                .when()
                .get("/internal/ui/domain")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @Order(3)
    @DisplayName("Invalid limit value Test-400")
    public void getMlayerProviderWithInvalidLimit() {
        given()
                .param("limit",-1)
                .param("offset", 0)
                .when()
                .get("/internal/ui/domain")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidParamValue"))
                .body("detail", equalTo("Invalid limit parameter"));
    }

    @Test
    @Order(4)
    @DisplayName("Invalid Offset value Test-400")
    public void getMlayerProviderWithInvalidOffset() {
        given()
                .param("limit",10000)
                .param("offset", -1)
                .when()
                .get("/internal/ui/domain")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidParamValue"))
                .body("detail", equalTo("Invalid offset parameter"));
    }

    @Test
    @Order(5)
    @DisplayName("Limit and offset > 10K Test-400")
    public void getMlayerProviderWithLimitOrOffsetGreaterThan10K() {
        given()
                .param("limit",10001)
                .param("offset", 1)
                .when()
                .get("/internal/ui/domain")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidParamValue"))
                .body("detail", equalTo("Invalid limit parameter"));
    }
}

