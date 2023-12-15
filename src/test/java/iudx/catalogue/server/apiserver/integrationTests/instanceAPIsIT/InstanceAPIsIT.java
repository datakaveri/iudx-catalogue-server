package iudx.catalogue.server.apiserver.integrationTests.instanceAPIsIT;

import io.restassured.response.Response;
import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static io.restassured.RestAssured.port;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.cosAdminToken;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.token;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Instance APIs in the Catalog Server.
 * Note: These tests assume the availability of the required authentication tokens and valid
 * configurations for the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class InstanceAPIsIT {
    private static final Logger LOGGER = LogManager.getLogger(InstanceAPIsIT.class);

    @Test
    @Order(1)
    @DisplayName("testing create instance - 201")
    void createInstance201() {

        LOGGER.debug(basePath);
        LOGGER.debug(baseURI);
        LOGGER.debug(port);
        Response response = given()
                        .queryParam("id", "poone")
                        .header("token", cosAdminToken)
                        .when()
                        .post("/instance")
                        .then()
                        .statusCode(201)
                        .body("type", is("urn:dx:cat:Success"))
                        .body("title", is("Success"))
                        .body("detail", equalTo("Success: Item created"))
                        .extract()
                        .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @Order(2)
    @DisplayName("testing create instance - 400 (invalid query param)")
    void createInstanceInvalidQueryParam() {
       Response response= given()
                .queryParam("ide", "poone")
                .header("token", cosAdminToken)
                .when()
                .post("/instance")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"))
                .body("title", is("Invalid Syntax"))
                .body("detail", equalTo("id not present in the request"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }
    @Test
    @Order(3)
    @DisplayName("testing create instance - 401 (Unauthorized Request)")
    void createInstanceUnauthReq() {
        given()
                .queryParam("id", "pune")
                .header("token", token)
                .when()
                .post("/instance")
                .then()
                .statusCode(401)
                .body("type", is("urn:dx:cat:InvalidAuthorizationToken"));
    }
    @Test
    @Order(4)
    @DisplayName("testing delete instance - 200 Success")
    void DeleteInstance() {
        Response response = given()
                .queryParam("id", "poone")
                .header("token", cosAdminToken)
                .when()
                .delete("/instance")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }
    @Test
    @Order(5)
    @DisplayName("testing delete instance - 401 Unauthorized access")
    void DeleteInstanceUnAuth() {
        given()
                .queryParam("id", "poone")
                .header("token", token)
                .when()
                .delete("/instance")
                .then()
                .statusCode(401)
                .body("type", is("urn:dx:cat:InvalidAuthorizationToken"));
    }
    @Test
    @Order(6)
    @DisplayName("testing delete instance - 404 Not Found")
    void DeleteInstanceNotFound() {
        given()
                .queryParam("id", "-")
                .header("token",cosAdminToken)
                .when()
                .delete("/instance")
                .then()
                .statusCode(404)
                .body("type", is("urn:dx:cat:ItemNotFound"))
                .body("title",is("Item is not found"));
    }
    @Test
    @Order(7)
    @DisplayName("testing get instance - 200 Success")
    void GetInstances() {
        Response response = given()
                .when()
                .get("/list/instance")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }
}
