package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerdomain;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.cosAdminToken;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class DeleteMlayerDomainT {
    String domainId="6aeed588-a28e-4c17-9694-edfa24e7d227"; //auto generated
    @Test
    @DisplayName("Delete Mlayer Domain Success Test-200")
    public void deleteMlayerDomainTest(){
        given()
                .queryParam("id",domainId)
                .header("token",cosAdminToken)
                .when()
                .delete("/internal/ui/domain")
                .then()
                .statusCode(200)
                .log().body()
                .body("type",equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Delete Mlayer Domain With Invalid Token Test-401")
    public void deleteMlayerDomainWithInvalidTokenTest(){
        given()
                .queryParam("id",domainId)
                .header("token","abc")
                .when()
                .delete("/internal/ui/domain")
                .then()
                .statusCode(401)
                .log().body()
                .body("type",equalTo("urn:dx:cat:InvalidAuthorizationToken"));
    }

}
