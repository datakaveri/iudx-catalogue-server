package iudx.catalogue.server.apiserver.integrationTests.crudApisIT;

import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static iudx.catalogue.server.apiserver.integrationTests.crudApisIT.CreateItemIT.*;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.*;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the CRUD operations of items in the catalogue server. The tests cover
 * deleting owner items, COS (Catalog Operating System) items, DX Resource Server items,
 * DX Provider items, DX Resource Group items, and DX Resource items.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class DeleteItemIT {
    private static final Logger LOGGER = LogManager.getLogger(DeleteItemIT.class);

    @Test
    @DisplayName("testing delete a Resource Item DX Entity - 200")
    void DeleteRSItemDXEntity() {
        given()
                .param("id", resource_item_id)
                .header("token", token)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing delete a Resource Group DX Entity - 200")
    void DeleteRSGroupDXEntity() {
        given()
                .param("id", resource_group_id)
                .header("token", token)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing delete a Provider DX Entity - 200")
    void DeleteProviderDXEntity() {
        given()
                .param("id", provider_id)
                .header("token", adminToken)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing delete a Resource Server DX Entity - 200")
    void DeleteResourceServerDXEntity() {
        given()
                .param("id", resource_server_id)
                .header("token", cosAdminToken)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing delete a COS DX Entity - 200")
    void DeleteCosDXEntity() {
        given()
                .param("id", cos_id)
                .header("token", cosAdminToken)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing delete a Owner DX Entity - 200")
    void DeleteOwnerDXEntity() {
        given()
                .param("id", owner_id)
                .header("token", cosAdminToken)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing delete a DX Entity - 404 Not Found")
    void DeleteDXEntity404() {
        given()
                .param("id", "7c8b58a7-6e5b-4a97-a15d-8f4aeb4e987e")
                .header("token", token)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(404)
                .body("type", is("urn:dx:cat:ItemNotFound"));
    }
    @Test
    @DisplayName("testing delete a DX Entity - 400 Invalid UUID")
    void DeleteDXEntityInvalidUUID() {
        given()
                .queryParam("id", "dummy-id")
                .header("token", token)
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSyntax"));
    }
    @Test
    @DisplayName("testing delete a DX Entity - 401 Invalid credentials")
    void DeleteDXEntityInvalidCred() {
        given()
                .queryParam("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
                .header("token", "abc")
                .contentType("application/json")
                .when()
                .delete("/item")
                .then()
                .statusCode(401)
                .body("type", is("urn:dx:cat:InvalidAuthorizationToken"));
    }
}
