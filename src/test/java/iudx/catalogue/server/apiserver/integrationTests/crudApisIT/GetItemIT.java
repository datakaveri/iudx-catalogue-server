package iudx.catalogue.server.apiserver.integrationTests.crudApisIT;


import io.restassured.response.Response;
import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static iudx.catalogue.server.apiserver.integrationTests.crudApisIT.CreateItemIT.*;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the CRUD operations of items in the catalogue server. The tests cover
 * listing owner items, COS (Catalog Operating System) items, DX Resource Server items,
 * DX Provider items, DX Resource Group items, and DX Resource items.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class GetItemIT {

    private static final Logger LOGGER = LogManager.getLogger(GetItemIT.class);

    @Test
    @DisplayName("testing get DX Entity by ID- 200 Success")
    void getDXEntityByID() {
        Response response = given()
                .param("id", resource_server_id)
                .contentType("application/json")
                .when()
                .get("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @DisplayName("testing get DX Entity by ID - 404 Not Found")
    void getDXEntityNotFound() {
        Response response = given()
                .param("id", "7c8b58a7-6e5b-4a97-a15d-8f4aeb4e987e")
                .contentType("application/json")
                .when()
                .get("/item")
                .then()
                .statusCode(404)
                .body("type", is("urn:dx:cat:ItemNotFound"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @DisplayName("testing get DX Entity by ID - 400 Invalid UUID")
    void getDXEntityInvalidUUID() {
        Response response = given()
                .param("id", "dummy-id")
                .contentType("application/json")
                .when()
                .get("/item")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidUUID"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @DisplayName("testing list Type (Data Model) given Resource Id - 200 type of item")
    void getDXEntityTypeRS() {
        Response response = given()
                .param("id", resource_server_id)
                .param("rel","type")
                .contentType("application/json")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .body("title",is("Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @DisplayName("testing list Type (Data Model) given Resource Group Id - 200 type of item")
    void getDXEntityRSGroup() {
        Response response = given()
                .param("id", resource_group_id)
                .param("rel","type")
                .contentType("application/json")
                .when()
                .get("/relationship")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .body("title",is("Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }
}
