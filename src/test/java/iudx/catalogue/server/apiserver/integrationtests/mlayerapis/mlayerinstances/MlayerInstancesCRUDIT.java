package iudx.catalogue.server.apiserver.integrationtests.mlayerapis.mlayerinstances;
import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.given;
import static iudx.catalogue.server.authenticator.TokensForITs.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/* Rest Assured Integration tests for the CRUD operations of Mlayer Instances in Catalogue Middle layer specific APIs. The tests cover
 creating, updating, retrieving and deleting Mlayer Instances in Mlayer APIs*/

@ExtendWith(RestAssuredConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MlayerInstancesCRUDIT {
    private static String instanceId;
    // Creating Mlayer Instance
    @Test
    @Order(1)
    @DisplayName("Create Mlayer Instance Success Test-201")
    public void createMlayerInstanceTest(){
        //Request Body
        JsonObject requestBody = new JsonObject()
                .put("name", "bhavya")
                .put("cover", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/bhavya.jpg")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/bhavya.jpg")
                .put("logo", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/bhavya.jpg")
                .put("coordinates", new JsonArray());

        Response resp= given()
                .header("Content-Type", "application/json")
                .header("token", cosAdminToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/instance");
        JsonObject respJson = new JsonObject(resp.body().asString());
        JsonObject firstResult = respJson.getJsonArray("results").getJsonObject(0);
        instanceId = firstResult.getString("id");
        resp.then()
                .statusCode(201)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("results[0].id", notNullValue());
    }
    @Test
    @Order(2)
    @DisplayName("Create Mlayer Instance With Invalid Token Test-401")
    public void createMlayerInstanceWithInvalidTokenTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", "divyaIUDX")
                .put("cover", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/punay.jpg")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/punay.jpg")
                .put("logo", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/punay.jpg")
                .put("coordinates", new JsonArray());
        given()
                .header("Content-Type", "application/json")
                .header("token", "abc")
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/instance")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));
    }
    @Test
    @Order(3)
    @DisplayName("Create Mlayer Instance With Invalid Schema Test-400")
    public void createMlayerInstanceWithInvalidSchemaTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", "punay")
                .put("coveeer", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/punay.jpeg")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/punay.jpeg")
                .put("logo", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/punay.jpeg");

        given()
                .header("Content-Type", "application/json")
                .header("token", cosAdminToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/internal/ui/instance")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));
    }

    // Updating Mlayer Instance

    @Test
    @Order(4)
    @DisplayName("Update Mlayer Instance success response test- 200")
    public void updateMlayerInstanceSuccessTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", "bhavya")
                .put("cover", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/bhavya.jpeg")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/bhavya.jpeg")
                .put("logo", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/bhavya.jpeg")
                .put("coordinates", new JsonArray());
        given()
                .queryParam("id",instanceId)
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/internal/ui/instance")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));

    }
    @Test
    @Order(5)
    @DisplayName("Update Mlayer Instance with invalid token test- 401")
    public void updateMlayerInstanceWithInvalidTokenTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", "bhavya")
                .put("cover", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/bhavya.jpeg")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/bhavya.jpeg")
                .put("logo", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/bhavya.jpeg")
                .put("coordinates", new JsonArray());
        given()
                .queryParam("id",instanceId)
                .header("Content-Type","application/json")
                .header("token","abc")
                .body(requestBody.encodePrettily())
                .when()
                .put("/internal/ui/instance")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));

    }
    @Test
    @Order(6)
    @DisplayName("Update Mlayer Instance with invalid schema test- 400")
    public void updateMlayerInstanceWithInvalidSchemaTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", "bhavya")
                .put("coveer", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/covers/bhavya.jpeg")
                .put("icon", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/icons/bhavya.jpeg")
                .put("logo", "https://iudx-catalogue-assets.s3.ap-south-1.amazonaws.com/instances/logo/bhavya.jpeg");

        given()
                .queryParam("id",instanceId)
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/internal/ui/instance")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidSchema"));

    }

    // Retrieving Mlayer Instance

    @Test
    @Order(7)
    @DisplayName("Get All Mlayer Instance success response test- 200")
    public void getAllMlayerInstanceSuccessTest(){
        given()
                .header("Content-Type","application/json")
                .when()
                .get("/internal/ui/instance")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));

    }
    @Test
    @Order(8)
    @DisplayName("Get Mlayer Instance success response by Id test- 200")
    public void getMlayerInstanceByIdSuccessTest(){
        given()
                .queryParam("id",instanceId)
                .header("Content-Type","application/json")
                .header("token",cosAdminToken)
                .when()
                .get("/internal/ui/instance")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));

    }

    @Test
    @Order(9)
    @DisplayName("Get Mlayer Instance by Limit and Offset Test-200")
    public void getMlayerInstanceByLimitAndOffset() {
        given()
                .param("id", instanceId)
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
    @Order(10)
    @DisplayName("Invalid limit value Test-400")
    public void getMlayerInstanceWithInvalidLimit() {
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
    @Order(11)
    @DisplayName("Invalid Offset value Test-400")
    public void getMlayerInstanceWithInvalidOffset() {
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
    @Order(12)
    @DisplayName("Limit and offset > 10K Test-400")
    public void getMlayerInstanceWithLimitOrOffsetGreaterThan10K() {
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

    // Deleting Mlayer Instance

    @Test
    @Order(12)
    @DisplayName("Delete Mlayer Instance success response test- 200")
    public void deleteMlayerInstanceSuccessTest() {
        given()
                .queryParam("id", instanceId)
                .header("Content-Type", "application/json")
                .header("token", cosAdminToken)
                .when()
                .delete("/internal/ui/instance")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:Success"));
    }

    @Test
    @Order(14)
    @DisplayName("Delete Mlayer Instance with Invalid Token response test- 401")
    public void deleteMlayerInstanceWithInvalidTokenTest() {
        given()
                .queryParam("id", instanceId)
                .header("Content-Type", "application/json")
                .header("token", "abc")
                .when()
                .delete("/internal/ui/instance")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:cat:InvalidAuthorizationToken"));
    }
    @AfterEach
    public void tearDown() {
        // Introduce a delay
        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
