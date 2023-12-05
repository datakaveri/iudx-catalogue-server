package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerinstances;
import iudx.catalogue.server.apiserver.integrationtests.mlayerapis.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.given;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.cosAdminToken;
import static org.hamcrest.Matchers.equalTo;
@ExtendWith(RestAssuredConfiguration.class)
public class UpdateMlayerInstanceIT {
    @Test
    @DisplayName("Update Mlayer Instance success response test- 200")
    public void updateMlayerInstanceSuccessTest(){
        String requestBody = "{\n" +
                "   \"name\": \"divyaIUDX\",\n" +
                "   \"cover\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/punay.jpeg\",\n" +
                "   \"icon\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/punay.jpeg\",\n" +
                "   \"logo\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/punay.jpeg\",\n" +
                "   \"coordinates\":[]\n" +
                "}";
        given()
                .queryParam("id","a9157b13-908e-4f4e-af04-fc532cf84184")
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .body(requestBody)
                .when()
                .put("/internal/ui/instance")
                .then()
                .statusCode(200)
                .body("type", equalTo("urn:dx:cat:Success"));

    }
    @Test
    @DisplayName("Update Mlayer Instance with invalid token test- 401")
    public void updateMlayerInstanceWithInvalidTokenTest(){
        String requestBody = "{\n" +
                "   \"name\": \"divyaIUDX\",\n" +
                "   \"cover\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/punay.jpeg\",\n" +
                "   \"icon\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/punay.jpeg\",\n" +
                "   \"logo\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/punay.jpeg\",\n" +
                "   \"coordinates\":[]\n" +
                "}";
        given()
                .queryParam("id","a9157b13-908e-4f4e-af04-fc532cf84184")
                .header("Content-Type","application/json")
                .header("token","abc")
                .body(requestBody)
                .when()
                .put("/internal/ui/instance")
                .then()
                .statusCode(401)
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));

    }
    @Test
    @DisplayName("Update Mlayer Instance with invalid schema test- 400")
    public void updateMlayerInstanceWithInvalidSchemaTest(){
        String requestBody = "{\n" +
                "   \"name\": \"divyaIUDX\",\n" +
                "   \"cover\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/punay.jpeg\",\n" +
                "   \"icon\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/punay.jpeg\",\n" +
                "   \"logo\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/punay.jpeg\",\n" +
                "}";
        given()
                .queryParam("id","a9157b13-908e-4f4e-af04-fc532cf84184")
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .body(requestBody)
                .when()
                .put("/internal/ui/instance")
                .then()
                .statusCode(400)
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));

    }

}
