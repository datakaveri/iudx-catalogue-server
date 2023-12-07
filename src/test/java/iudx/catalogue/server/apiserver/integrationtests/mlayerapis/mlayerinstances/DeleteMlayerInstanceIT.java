package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerinstances;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.cosAdminToken;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(RestAssuredConfiguration.class)
public class DeleteMlayerInstanceIT {
    String mlayerInstanceId="a9157b13-908e-4f4e-af04-fc532cf84184";
    @Test
    @DisplayName("Delete Mlayer Instance success response test- 200")
    public void deleteMlayerInstanceSuccessTest(){
        given()
                .queryParam("id",mlayerInstanceId)
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .when()
                .delete("/internal/ui/instance")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:cat:Success"));

    }
    @Test
    @DisplayName("Delete Mlayer Instance with Invalid Token response test- 401")
    public void deleteMlayerInstanceWithInvalidTokenTest(){
        given()
                .queryParam("id",mlayerInstanceId)
                .header("Content-Type","application/json")
                .header("token","abc")
                .when()
                .delete("/internal/ui/instance")
                .then()
                .statusCode(401)
                .log().body()
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));

    }

}
