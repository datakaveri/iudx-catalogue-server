package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerinstances;
import iudx.catalogue.server.apiserver.integrationtests.mlayerapis.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.cosAdminToken;
import static org.hamcrest.Matchers.equalTo;


@ExtendWith(RestAssuredConfiguration.class)
public class GetMlayerInstanceIT {
    @Test
    @DisplayName("Get Mlayer Instance success response test- 200")
    public void getMlayerInstanceSuccessTest(){
        given()
                .queryParam("id","a9157b13-908e-4f4e-af04-fc532cf84184")
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .when()
                .get("/internal/ui/instance")
                .then()
                .statusCode(200)
                .body("type", equalTo("urn:dx:cat:Success"));

    }
}
