package iudx.catalogue.server.apiserver.integrationTests.crudApisIT;

import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static io.vertx.sqlclient.data.NullValue.JsonObject;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.*;
import static org.hamcrest.Matchers.*;

/**
  * Integration tests for the CRUD operations of items in the catalogue server. The tests cover
  * creating owner items, COS (Catalog Operating System) items, DX Resource Server items,
  * DX Provider items, DX Resource Group items, and DX Resource items.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class CreateItemIT {
    public static final Logger LOGGER = LogManager.getLogger(CreateItemIT.class);
    public static String owner_id;
    public static String cos_id;
    public static String resource_server_id;
    public static String provider_id;
    public static String resource_group_id;
    public static String resource_item_id;

    @BeforeAll
    static void setUp() {
        // Hardcoded IDs for testing purposes.
        owner_id = "db162c34-78df-493c-93bb-8baa7694a783";
        cos_id = "f375c1f4-48fb-4813-a8f5-47f1c9daca8e";
        resource_server_id = "62f63482-8210-4551-a400-41ff2ccf1d87";
        provider_id = "b645fe0f-0a60-4fe6-b5d6-244ac2cdfc1a";
        resource_group_id = "89f3e565-ef05-425b-a322-9aa0dd11535d";
        resource_item_id = "4a5e1405-dc49-48f6-a1a1-e5643e2d0fea";
    }
    @Test
    @Order(1)
    @DisplayName("testing create owner item - 201")
    void createOwnerItem() {
        JsonObject jsonPayload = new JsonObject();
        jsonPayload.put("@context", "https://voc.iudx.org.in/");
        JsonArray typeArray = new JsonArray().add("iudx:Owner");
        jsonPayload.put("type", typeArray);
        jsonPayload.put("name", "iudxOwner");
        jsonPayload.put("description", "testing owner item creation through integration tests");

        LOGGER.debug("json: "+jsonPayload);

        Response response= given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue())
                .extract()
                .response();
        //Log the entire response details
       LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @Order(2)
    @DisplayName("testing create COS item - 201")
    void createCOSItem() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type",new JsonArray().add("iudx:COS"))
                .put("name", "iudxCOSPM2")
                .put("owner", owner_id)
                .put("description", "testing COS item creation through integration tests")
                .put("cosURL","cat-test.iudx.io")
                .put("cosUI","cat-test.iudx.io");

        LOGGER.debug("json: "+jsonPayload);

        Response response = given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue())
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @Order(3)
    @DisplayName("testing the creation of DX Resource Server Item Entity - 201")
    void createDXResourceServerItemEntity() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:ResourceServer"))
                .put("name", "IudxResourceServerPM")
                .put("description", "Multi tenanted IUDX resource server for integration tests")
                .put("tags",new JsonArray().add("IUDX").add("Resource").add("Server").add("Platform"))
                .put("cos", cos_id)
                .put("owner", owner_id)
                .put("resourceServerOrg",new JsonObject()
                .put("name", "iudx")
                .put("additionalInfoURL", "https://iudx.org.in")
                .put("location", new JsonObject()
                        .put("type", "Place")
                        .put("address", "IISc, Bangalore")
                        .put("geometry", new JsonObject()
                                .put("type", "Point")
                                .put("coordinates", new JsonArray().add(77.570423).add(13.013945))
                        )))
                .put("resourceServerRegURL", "rs.iudx.io")
                .put("resourceAccessModalities",new JsonArray()
                        .add(new JsonObject()
                .put("type", new JsonArray().add("iudx:HTTPAccess"))
                .put("protocol", "http")
                .put("accessURL", "rs")
                .put("port", 8080)))
                .put("locations", new JsonObject()
                        .put("type", "Place")
                        .put("address", "IISc, Bangalore")
                        .put("geometry", new JsonObject()
                                .put("type", "Point")
                                .put("coordinates", new JsonArray().add(77.570423).add(13.013945))
                        ));
        LOGGER.debug("json: "+jsonPayload);

        Response response = given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue())
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @Order(4)
    @DisplayName("testing create DX Provider item - 201")
    void createDXProviderItem() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Provider"))
                .put("name", "IudxProviderPM")
                .put("resourceServer", resource_server_id)
                .put("description", "provider for the integration test")
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
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue())
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @Order(5)
    @DisplayName("testing create DX Resource Group Item Entity - 201")
    void createDXResourceGroupItemEntity() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:ResourceGroup").add("iudx:IssueReporting"))
                .put("name", "iudxResourceGroupPM")
                .put("description", "resource group item for the postman collection")
                .put("tags", new JsonArray().add("swachhata").add("complaints").add("construction material").add("requests"))
                .put("provider", provider_id);

        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue())
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @Order(6)
    @DisplayName("testing create DX Resource Item - 201")
    void createDXResourceItem() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Resource").add("iudx:PointOfInterest"))
                .put("name", "iudxResourceItemPM")
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
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue())
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @Order(7)
    @DisplayName("testing create a DX Resource item - 400")
    void createDXResourceItem400() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Resource").add("iudx:PointOfInterest"))
                .put("name", "wifi-location")
                .put("label", "Wi-Fi Locations in Pimpri-Chinchwad City")
                .put("description", "The physical coordinates of Wi-Fi system locations in Pimpri-Chinchwad city.")
                .put("tags", new JsonArray().add("Wi-Fi").add("Wi-Fi zone").add("hotspot").add("internet").add("Wi-Fi access").add("Wi-Fi location").add("wireless internet").add("internet access"))
                .put("apdURL", "rs.apd.iudx.org.in")
                .put("accessPolicy", "SECURE")
                .put("resourceServer", "83995e8c-fa80-4241-93c0-e86a66154eb6")
                .put("provider", "83995e8c-fa80-4241-93c0-e86a66154eb6")
                .put("resourceGroup", "83995e8c-fa80-4241-93c0-e86a66154eb6");

        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:LinkValidationFailed"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @Order(8)
    @DisplayName("testing create DX Entity - 400")
    void createDXEntity400() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:ResourceGroup").add("iudx:IssueReporting"))
                .put("name", "agra-swachhata-app")
                .put("description", "Civic Issues like open defecation, garbage dumping, etc., reported in Agra city via Swachhata app. Publishes all the complaints/issues lodged on the current day, on a daily basis.")
                .put("tags", new JsonArray().add("swachhata").add("complaints").add("reporting").add("issue").add("garbage dump").add("debris removal").add("open defecation").add("construction material").add("requests"))
                .put("provider", "83995e8c-fa80-4241-93c0");

        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSchema"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @Order(9)
    @DisplayName("testing create DX Entity - 400 Invalid UUID")
    void createDXEntityInvalidUUID400() {
        JsonObject jsonPayload = new JsonObject()
                .put("@context", "https://voc.iudx.org.in/")
                .put("type", new JsonArray().add("iudx:Provider"))
                .put("name", "uuid-test")
                .put("description", "provider id for uuid test")
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
                        .put("_summary", "uuid-test provider id for uuid test")
                        .put("id", "2ed6f05f-2f35-3f70-a472-79763fc81")
                        .put("ownerUserId", "d8e46706-b9db-44e1-a9aa-e40839396b01")
                );
        Response response = given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload.toString())
                .when()
                .post("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSchema"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

    @Test
    @Order(10)
    @DisplayName("testing create DX Entity - 401 Invalid Credentials")
    void createDXEntityInvalidCredentials() {
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
                .post("/item/")
                .then()
                .statusCode(401)
                .body("type", is("urn:dx:cat:InvalidAuthorizationToken"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }

}
