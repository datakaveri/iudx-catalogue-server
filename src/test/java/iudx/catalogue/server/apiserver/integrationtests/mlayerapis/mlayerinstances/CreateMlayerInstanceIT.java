package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerinstances;

import iudx.catalogue.server.apiserver.integrationtests.mlayerapis.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.cosAdminToken;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(RestAssuredConfiguration.class)
public class CreateMlayerInstanceIT {
    @Test
    @DisplayName("Create Mlayer Instance Success Test-200")
    public void createMlayerInstanceTest(){
        String requestBody = "{\"name\": \"bhavya\",\n" +
                "   \"cover\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/bhavya.jpg\",\n" +
                "   \"icon\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/bhavya.jpg\",\n" +
                "   \"logo\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/bhavya.jpg\",\n" +
                "   \"coordinates\":[]\n" +
                "}";

        given()
                .queryParam("id","86c363a5-3c46-44d1-907c-a03eae729ba9")
                .header("Content-Type", "application/json")
                .header("token", cosAdminToken)
                .body(requestBody)
                .when()
                .post("/internal/ui/instance")
                .then()
                .statusCode(201)
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("results[0].id", notNullValue());
    }
    @Test
    @DisplayName("Create Mlayer Instance With Invalid Token Test-401")
    public void createMlayerInstanceWithInvalidTokenTest(){
        String requestBody = "{\"name\": \"divyaIUDX\",\n" +
                "   \"cover\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/punay.jpg\",\n" +
                "   \"icon\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/punay.jpg\",\n" +
                "   \"logo\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/punay.jpg\",\n" +
                "   \"coordinates\":[]\n" +
                "}";

        given()
                .queryParam("id","7d50628f-546d-43c9-ba26-61330206ca09")
                .header("Content-Type", "application/json")
                .header("token", "abc")
                .body(requestBody)
                .when()
                .post("/internal/ui/instance")
                .then()
                .statusCode(401)
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));
    }
    @Test
    @DisplayName("Create Mlayer Instance With Invalid Schema Test-400")
    public void createMlayerInstanceWithInvalidSchemaTest(){
        String requestBody = "{\n" +
                "   \"name\": \"punay\",\n" +
                "   \"coveeer\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/punay.jpeg\",\n" +
                "   \"icon\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/punay.jpeg\",\n" +
                "   \"logo\": \"https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/punay.jpeg\"\n" +
                "}";

        given()
                .queryParam("id","7d50628f-546d-43c9-ba26-61330206ca09")
                .header("Content-Type", "application/json")
                .header("token", cosAdminToken)
                .body(requestBody)
                .when()
                .post("/internal/ui/instance")
                .then()
                .statusCode(400)
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));
    }

}
