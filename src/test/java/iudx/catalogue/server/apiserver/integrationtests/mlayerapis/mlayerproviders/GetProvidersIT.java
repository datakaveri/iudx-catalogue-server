package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerproviders;

import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/*Rest Assured Integration tests for listing all the providers in
Catalogue Middle layer specific APIs*/

@ExtendWith(RestAssuredConfiguration.class)
public class GetProvidersIT {
    @Test
    @DisplayName("Get Mlayer Providers Success Test-200")
    public void getProvidersTest(){
        given()
                .when()
                .get("/internal/ui/providers")
                .then()
                .statusCode(200)
                .log().body()
                .body("type",equalTo("urn:dx:cat:Success"));
    }
}
