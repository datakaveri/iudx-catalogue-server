package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerinstances;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.cosAdminToken;
import static org.hamcrest.Matchers.equalTo;


@ExtendWith(RestAssuredConfiguration.class)
public class GetMlayerInstanceIT {
    String mlayerInstanceId="243df662-82be-4983-ba13-34a408752769";
    @Test
    @DisplayName("Get All Mlayer Instance success response test- 200")
    public void getAllMlayerInstanceSuccessTest(){
        given()
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .when()
                .get("/internal/ui/instance")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));

    }
    @Test
    @DisplayName("Get Mlayer Instance success response by Id test- 200")
    public void getMlayerInstanceByIdSuccessTest(){
        given()
                .queryParam("id",mlayerInstanceId)
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .when()
                .get("/internal/ui/instance")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));

    }
}
