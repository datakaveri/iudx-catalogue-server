package iudx.catalogue.server.apiserver.integrationTests.crudApisIT;

import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
 * updating owner items, COS (Catalog Operating System) items, DX Resource Server items,
 * DX Provider items, DX Resource Group items, and DX Resource items.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class UpdateItemIT {

    private static final Logger LOGGER = LogManager.getLogger(UpdateItemIT.class);

    @Test
    @DisplayName("testing update owner item - 200")
    void updateOwnerItem() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Owner"))
                .put("name", "iudxOwner")
                .put("description", "testing the owner id updation through integration tests")
                .put("id", owner_id);

        Response response = given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @DisplayName("testing update COS item - 200")
    void updateCOSItem() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:COS"))
                .put("name", "IudxCosPM30")
                .put("owner", owner_id)
                .put("id", cos_id)
                .put("description", "COS item for postman collection")
                .put("cosURL", "cat-test.iudx.io")
                .put("cosUI", "cat-test.iudx.io");

        Response response = given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @DisplayName("testing update DX Resource Server Item - 200")
    void updateDXResourceItem(){
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:ResourceServer"))
                .put("name", "IudxResourceServerPM")
                .put("id", resource_server_id)
                .put("description", "Multi tenanted IUDX resource server for postman collection")
                .put("tags", new JsonArray().add("IUDX").add("Resource").add("Server").add("Platform"))
                .put("cos", cos_id)
                .put("owner", owner_id)
                .put("resourceServerOrg", new JsonObject()
                        .put("name", "iudx")
                        .put("additionalInfoURL", "https://iudx.org.in")
                        .put("location", new JsonObject()
                                .put("type", "Place")
                                .put("address", "IISc, Bangalore")
                                .put("geometry", new JsonObject()
                                        .put("type", "Point")
                                        .put("coordinates", new JsonArray().add(77.570423).add(13.013945))
                                )
                        )
                )
                .put("resourceServerRegURL", "rs.iudx.io")
                .put("resourceAccessModalities", new JsonArray()
                        .add(new JsonObject()
                                .put("type", new JsonArray().add("iudx:HTTPAccess"))
                                .put("protocol", "http")
                                .put("accessURL", "rs")
                                .put("port", 8080)
                        )
                )
                .put("location", new JsonObject()
                        .put("type", "Place")
                        .put("address", "IISc, Bangalore")
                        .put("geometry", new JsonObject()
                                .put("type", "Point")
                                .put("coordinates", new JsonArray().add(77.570423).add(13.013945))
                        )
                );

        Response response = given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @DisplayName("testing updation of DX Provider Item - 200")
    void updateDXProviderItem() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Provider"))
                .put("name", "IudxProviderPM")
                .put("id", provider_id)
                .put("resourceServer", resource_server_id)
                .put("description", "provider for the postman collection")
                .put("ownerUserId", "b2c27f3f-2524-4a84-816e-91f9ab23f837")
                .put("providerOrg", new JsonObject()
                        .put("name", "Datakaveri")
                        .put("additionalInfoURL", "https://datakaveri.org")
                        .put("location", new JsonObject()
                                .put("type", "Place")
                                .put("address", "IIsc")
                                .put("geometry", new JsonObject()
                                        .put("type", "Point")
                                        .put("coordinates", new JsonArray().add(75.92).add(14.5))
                                )
                        )
                );
        Response response = given()
                .contentType("application/json")
                .header("token", adminToken)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @DisplayName("testing updation of DX Resource Group Item - 200")
    void updateDXResourceGroupItem() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:ResourceGroup").add("iudx:IssueReporting"))
                .put("name", "iudxResourceGroupPM")
                .put("id", resource_group_id)
                .put("description", "resource group item for the postman collection")
                .put("tags", new JsonArray().add("swachhata").add("complaints").add("construction material").add("requests"))
                .put("provider", provider_id);
        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }
    @Test
    @DisplayName("testing updation of DX Resource Item - 200")
    void updateDXResourceItem200() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Resource").add("iudx:PointOfInterest"))
                .put("name", "iudxResourceItemPM")
                .put("id", resource_item_id)
                .put("label", "item for test only")
                .put("description", "resource item for the postman collection")
                .put("tags", new JsonArray().add("swachhata").add("complaints").add("construction material").add("requests"))
                .put("apdURL", "rs.apd.iudx.org.in")
                .put("accessPolicy", "SECURE")
                .put("resourceServer", resource_server_id)
                .put("provider", provider_id)
                .put("resourceGroup", resource_group_id);

        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @DisplayName("testing updation of DX Entity - 400")
    void updateDXEntity400() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:ResourceGroup").add("iudx:IssueReporting"))
                .put("name", "agra-swachhata-app")
                .put("description", "Civic Issues like open defecation, garbage dumping, etc., reported in Agra city via Swachhata app. Publishes all the complaints/issues lodged on the current day, on a daily basis.")
                .put("tags", new JsonArray().add("swachhata").add("complaints").add("reporting").add("issue").add("garbage dump").add("debris removal").add("open defecation").add("construction material").add("requests"))
                .put("provider", provider_id);

        Response response = given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSchema"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @DisplayName("testing updation of DX Entity - 400 (Invalid links)")
    void updateDXEntity400InvalidLinks() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Resource").add("iudx:PointOfInterest"))
                .put("name", "wifi-location")
                .put("label", "Wi-Fi Locations in Pimpri-Chinchwad City")
                .put("description", "The physical coordinates of Wi-Fi system locations in Pimpri-Chinchwad city.")
                .put("tags", new JsonArray().add("Wi-Fi").add("Wi-Fi zone").add("hotspot").add("internet").add("Wi-Fi access").add("Wi-Fi location").add("wireless internet").add("internet access"))
                .put("apdURL", "rs.apd.iudx.org.in")
                .put("accessPolicy", "SECURE")
                .put("resourceServer", resource_server_id)
                .put("provider", provider_id)
                .put("resourceGroup", "83995e8c-fa80-4241-93c0-e86a66154eb6");

        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:LinkValidationFailed"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @DisplayName("testing update DX Entity - 401 Invalid Credentials")
    void updateDXEntityInvalidCredentials() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:COS"))
                .put("name", "cos.cop.iudx.org.in")
                .put("owner", owner_id)
                .put("description", "COS Smart Kalyan-Dombivli Development Corporation Limited")
                .put("cosURL", "kdmc.cop-nec.iudx.org.in")
                .put("cosUI", "https://catalogue.kdmc.cop-nec.iudx.org.in/");


        Response response = given()
                .contentType("application/json")
                .header("token", "abc")
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(401)
                .body("type", is("urn:dx:cat:InvalidAuthorizationToken"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @DisplayName("testing update DX Entity - 404 Not Found")
    void updateDXEntityNotFound() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Resource").add("iudx:PointOfInterest"))
                .put("name", "wyfy-location")
                .put("label", "Wi-Fi Locations in Pimpri-Chinchwad City")
                .put("description", "The physical coordinates of Wi-Fi system locations in Pimpri-Chinchwad city.")
                .put("tags", new JsonArray().add("Wi-Fi").add("Wi-Fi zone").add("hotspot").add("internet").add("Wi-Fi access").add("Wi-Fi location").add("wireless internet").add("internet access"))
                .put("apdURL", "rs.apd.iudx.org.in")
                .put("accessPolicy", "SECURE")
                .put("resourceServer", resource_server_id)
                .put("provider", provider_id)
                .put("resourceGroup", resource_group_id);

        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .put("/item/")
                .then()
                .statusCode(404)
                .body("type", is("urn:dx:cat:ItemNotFound"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }
}
