package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerdomain;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class GetMlayerDomainIT {
    String domainId="6aeed588-a28e-4c17-9694-edfa24e7d227"; //auto generated
    @Test
    @DisplayName("Get All Mlayer Domains Success Test-200")
    public void getAllMlayerDomainsTest(){
        given()
                .when()
                .get("/internal/ui/domain")
                .then()
                .statusCode(200)
                .log().body()
                .body("type",equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Get Mlayer Domain by Id Success Test-200")
    public void getMlayerDomainByIdTest(){
        given()
                .queryParam("id",domainId)
                .when()
                .get("/internal/ui/domain")
                .then()
                .statusCode(200)
                .log().body()
                .body("type",equalTo("urn:dx:cat:Success"));
    }

}
