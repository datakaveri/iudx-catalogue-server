package iudx.catalogue.server.apiserver.integrationTests.crudApisIT;

import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static iudx.catalogue.server.authenticator.JwtTokenHelper.*;
import static org.hamcrest.Matchers.*;

/**
  * Integration tests for the CRUD operations of items in the catalogue server. The tests cover
  * creating owner items, COS (Catalog Operating System) items, DX Resource Server items,
  * DX Provider items, DX Resource Group items, and DX Resource items.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class CreateItemIT {
    private static final Logger LOGGER = LogManager.getLogger(CreateItemIT.class);
    public static String owner_id="db162c34-78df-493c-93bb-8baa7694a783";
    public static String cos_id="f375c1f4-48fb-4813-a8f5-47f1c9daca8e";
    public static String resource_server_id="62f63482-8210-4551-a400-41ff2ccf1d87";
    public static String provider_id="b645fe0f-0a60-4fe6-b5d6-244ac2cdfc1a";
    public static String resource_group_id="89f3e565-ef05-425b-a322-9aa0dd11535d";
    public static String resource_item_id="4a5e1405-dc49-48f6-a1a1-e5643e2d0fea";

    @Test
    @DisplayName("testing create owner item - 201")
    void createOwnerItem() {

        String jsonPayload = "{ \"@context\": \"https://voc.iudx.org.in/\", \"type\": [\"iudx:Owner\"], \"name\": \"iudxOwner6\", \"description\": \"testing owner item creation through integration tests\" }";

        given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload)
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue()); // Check if 'id' property in 'results' is not null
    }

    @Test
    @DisplayName("testing create COS item - 201")
    void createCOSItem() {
        String jsonPayload = String.format("{ \"@context\": \"https://voc.iudx.org.in/\", " +
                "\"type\": [\"iudx:COS\"], " +
                "\"name\": \"IudxCosPM2\", " +
                "\"owner\": \"%s\", " +
                "\"description\": \"testing COS item creation through integration tests\", " +
                "\"cosURL\": \"cat-test.iudx.io\", " +
                "\"cosUI\": \"cat-test.iudx.io\" }", owner_id);

        cos_id=given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload)
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue())
                .extract()
                .path("results.id"); // Check if 'id' property in 'results' is not null
        LOGGER.debug("cos_id"+cos_id);
    }

    @Test
    @DisplayName("testing the creation of DX Resource Server Item Entity - 201")
    void createDXResourceServerItemEntity() {
        String jsonPayload = String.format("{ \"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:ResourceServer\"]," +
                "\"name\": \"IudxResourceServerPM\"," +
                "\"description\": \"Multi tenanted IUDX resource server for integration tests\"," +
                "\"tags\": [\"IUDX\", \"Resource\", \"Server\", \"Platform\"]," +
                "\"cos\": \"%s\"," +
                "\"owner\": \"%s\"," +
                "\"resourceServerOrg\": {" +
                "\"name\": \"iudx\"," +
                "\"additionalInfoURL\": \"https://iudx.org.in\"," +
                "\"location\": {" +
                "\"type\": \"Place\"," +
                "\"address\": \"IISc, Bangalore\"," +
                "\"geometry\": {" +
                "\"type\": \"Point\"," +
                "\"coordinates\": [77.570423, 13.013945]" +
                "}" +
                "}" +
                "}," +
                "\"resourceServerRegURL\": \"rs.iudx.io\"," +
                "\"resourceAccessModalities\": [" +
                "{" +
                "\"type\": [\"iudx:HTTPAccess\"]," +
                "\"protocol\": \"http\"," +
                "\"accessURL\": \"rs\"," +
                "\"port\": 8080" +
                "}" +
                "]," +
                "\"location\": {" +
                "\"type\": \"Place\"," +
                "\"address\": \"IISc, Bangalore\"," +
                "\"geometry\": {" +
                "\"type\": \"Point\"," +
                "\"coordinates\": [77.570423, 13.013945]" +
                "}" +
                "}" +
                "}", cos_id, owner_id);

        given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload)
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue());
    }

    @Test
    @DisplayName("testing create DX Provider item - 201")
    void createDXProviderItem() {
        String jsonPayload = String.format("{ \"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:Provider\"]," +
                "\"name\": \"IudxProviderPM\"," +
                "\"resourceServer\": \"%s\"," +
                "\"description\": \"provider for the integration test\"," +
                "\"ownerUserId\": \"b2c27f3f-2524-4a84-816e-91f9ab23f837\"," +
                "\"providerOrg\": {" +
                "\"name\": \"Datakaveri\"," +
                "\"additionalInfoURL\": \"https://datakaveri.org\"," +
                "\"location\": {" +
                "\"type\": \"Place\"," +
                "\"address\": \"IIsc\"," +
                "\"geometry\": {" +
                "\"type\": \"Point\"," +
                "\"coordinates\": [75.92, 14.5]" +
                "}" +
                "}" +
                "}" +
                "}", resource_server_id);

        given()
                .contentType("application/json")
                .header("token", adminToken)
                .body(jsonPayload)
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue()); // Check if 'id' property in 'results' is not null
    }

    @Test
    @DisplayName("testing create DX Resource Group Item Entity - 201")
    void createDXResourceGroupItemEntity() {
        String jsonPayload = String.format("{ \"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:ResourceGroup\", \"iudx:IssueReporting\"]," +
                "\"name\": \"iudxResourceGroupPM\"," +
                "\"description\": \"resource group item for the postman collection\"," +
                "\"tags\": [\"swachhata\", \"complaints\", \"construction material\", \"requests\"]," +
                "\"provider\": \"%s\"" +
                "}", provider_id);

        given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload)
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue()); // Check if 'id' property in 'results' is not null
    }

    @Test
    @DisplayName("testing create DX Resource Item - 201")
    void createDXResourceItem() {
        String jsonPayload = String.format("{ \"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:Resource\", \"iudx:PointOfInterest\"]," +
                "\"name\": \"iudxResourceItemPM\"," +
                "\"label\": \"item for tesr only\"," +
                "\"description\": \"resource item for the postman collection\"," +
                "\"tags\": [\"swachhata\", \"complaints\", \"construction material\", \"requests\"]," +
                "\"apdURL\": \"rs.apd.iudx.org.in\"," +
                "\"accessPolicy\": \"SECURE\"," +
                "\"resourceServer\": \"%s\"," +
                "\"provider\": \"%s\"," +
                "\"resourceGroup\": \"%s\"" +
                "}", resource_server_id, provider_id, resource_group_id);

        given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload)
                .when()
                .post("/item/")
                .then()
                .statusCode(201)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"))
                .body("detail", equalTo("Success: Item created"))
                .body("results.id", notNullValue());
    }

    @Test
    @DisplayName("testing create a DX Resource item - 400")
    void createDXResourceItem400() {
        String jsonPayload = "{ " +
                "\"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:Resource\", \"iudx:PointOfInterest\"]," +
                "\"name\": \"wifi-location\"," +
                "\"label\": \"Wi-Fi Locations in Pimpri-Chinchwad City\"," +
                "\"description\": \"The physical coordinates of Wi-Fi system locations in Pimpri-Chinchwad city.\"," +
                "\"tags\": [\"Wi-Fi\", \"Wi-Fi zone\", \"hotspot\", \"internet\", \"Wi-Fi access\", \"Wi-Fi location\", \"wireless internet\", \"internet access\"]," +
                "\"apdURL\": \"rs.apd.iudx.org.in\"," +
                "\"accessPolicy\": \"SECURE\"," +
                "\"resourceServer\": \"83995e8c-fa80-4241-93c0-e86a66154eb6\"," +
                "\"provider\": \"83995e8c-fa80-4241-93c0-e86a66154eb6\"," +
                "\"resourceGroup\": \"83995e8c-fa80-4241-93c0-e86a66154eb6\"" +
                "}";

        given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload)
                .when()
                .post("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:LinkValidationFailed"));
    }

    @Test
    @DisplayName("testing create DX Entity - 400")
    void createDXEntity400() {
        String jsonPayload = "{ " +
                "\"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:ResourceGroup\", \"iudx:IssueReporting\"]," +
                "\"name\": \"agra-swachhata-app\"," +
                "\"description\": \"Civic Issues like open defecation, garbage dumping, etc., reported in Agra city via Swachhata app. Publishes all the complaints/issues lodged on the current day, on a daily basis.\"," +
                "\"tags\": [" +
                "\"swachhata\"," +
                "\"complaints\"," +
                "\"reporting\"," +
                "\"issue\"," +
                "\"garbage dump\"," +
                "\"debris removal\"," +
                "\"open defecation\"," +
                "\"construction material\"," +
                "\"requests\"" +
                "]," +
                "\"provider\": \"83995e8c-fa80-4241-93c0\"" +
                "}";

        given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload)
                .when()
                .post("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSchema"));
    }

    @Test
    @DisplayName("testing create DX Entity - 400 Invalid UUID")
    void createDXEntityInvalidUUID400() {
        String jsonPayload = "{ " +
                "\"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:Provider\"]," +
                "\"name\": \"uuid-test\"," +
                "\"description\": \"provider id for uuid test\"," +
                "\"providerOrg\": {" +
                "\"name\": \"Datakaveri\"," +
                "\"additionalInfoURL\": \"https://datakaveri.org\"," +
                "\"location\": {" +
                "\"type\": \"Place\"," +
                "\"address\": \"IIsc\"," +
                "\"geometry\": {" +
                "\"type\": \"Point\"," +
                "\"coordinates\": [75.92, 14.5]" +
                "}" +
                "}" +
                "}," +
                "\"_summary\": \"uuid-test provider id for uuid test\"," +
                "\"id\": \"2ed6f05f-2f35-3f70-a472-79763fc81\"," +
                "\"ownerUserId\": \"d8e46706-b9db-44e1-a9aa-e40839396b01\"" +
                "}";

        given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload)
                .when()
                .post("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSchema"));
    }

    @Test
    @DisplayName("testing create DX Entity - 401 Invalid Credentials")
    void createDXEntityInvalidCredentials() {
        String jsonPayload = "{ " +
                "\"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:COS\"]," +
                "\"name\": \"cos.cop.iudx.org.in\"," +
                "\"owner\": \"" + owner_id + "\"," +
                "\"description\": \"COS Smart Kalyan-Dombivli Development Corporation Limited\"," +
                "\"cosURL\": \"kdmc.cop-nec.iudx.org.in\"," +
                "\"cosUI\": \"https://catalogue.kdmc.cop-nec.iudx.org.in/\"" +
                "}";

        given()
                .contentType("application/json")
                .header("token", "abc")
                .body(jsonPayload)
                .when()
                .post("/item/")
                .then()
                .statusCode(401)
                .body("type", is("urn:dx:cat:InvalidAuthorizationToken"));
    }

}
