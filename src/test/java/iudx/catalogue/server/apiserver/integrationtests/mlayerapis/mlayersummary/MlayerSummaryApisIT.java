package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayersummary;

import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfiguration.class)
public class MlayerSummaryApisIT {
    @Test
    @DisplayName("Get totalhit Success Test-200")
    public void getTotalHit(){
        given()
                .when()
                .get("/internal/ui/summary")
                .then()
                .statusCode(200)
                .body("type",equalTo("urn:dx:cat:Success"))
                .body("results",notNullValue());
    }

    @Test
    @DisplayName("Get monthly size and hit Success Test-200")
    public void getMonthlyHitAndSize(){
        given()
                .when()
                .get("/internal/ui/realtimedataset")
                .then()
                .statusCode(200)
                .body("type",equalTo("urn:dx:cat:Success"))
                .body("results",notNullValue());
    }
}
