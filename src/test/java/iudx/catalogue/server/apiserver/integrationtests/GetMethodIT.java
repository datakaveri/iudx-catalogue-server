package iudx.catalogue.server.apiserver.integrationtests;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
@ExtendWith(RestAssuredConfiguration.class)
public class GetMethodIT {
    private static Logger LOGGER= LogManager.getLogger(GetMethodIT.class);

    @Test
    @DisplayName("Test Success Response")
    public void testSuccessResponse() {
        given()
                .param("id", "bbeacb12-5e54-339d-92e0-d8e063b551a8")
                .when()
                .get("/item")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("type", equalTo("urn:dx:cat:Success"))
                .body("title", equalTo("Success"))
                .body("totalHits", equalTo(1));
    }

    @Test
    @DisplayName("Test ItemNotFound Response")
    public void testItemNotFoundResponse() {
        given()
                .param("id", "63c2f8f0-4085-4ad6-a382-38baa3eec24a")
                .when()
                .get("/item")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("type", equalTo("urn:dx:cat:ItemNotFound"))
                .body("totalHits", equalTo(0));
    }

    @Test
    @DisplayName("Test InvalidUUID Response")
    public void testInvalidUUIDResponse() {
        given()
                .param("id", "dummy-id")
                .when()
                .get("/item")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("type", equalTo("urn:dx:cat:InvalidUUID"));
    }

    @Test
    @DisplayName("Test Generic Success Response for resource id")
    public void testGenericSuccessResponse() {
        given()
                .param("id", "bbeacb12-5e54-339d-92e0-d8e063b551a8")
                .when()
                .get("/item")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("type", equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("Test Success Generic response for resource group id")
    public void testGenericSuccessResponseResourceGroupId(){
        given()
                .param("id","bbeacb12-5e54-339d-92e0-d8e063b551a8")
                .when()
                .get("/item")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("type",equalTo("urn:dx:cat:Success"));
    }

}
