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
 * updating owner items, COS (Catalog Operating System) items, DX Resource Server items,
 * DX Provider items, DX Resource Group items, and DX Resource items.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class UpdateItemIT {

    private static final Logger LOGGER = LogManager.getLogger(UpdateItemIT.class);

    @Test
    @DisplayName("testing update owner item - 200")
    void updateOwnerItem() {
        String jsonPayload = "{ \"@context\": \"https://voc.iudx.org.in/\", \"type\": [\"iudx:Owner\"], \"name\": \"iudxOwner\", \"description\": \"testing the owner id updation through integration tests\", \"id\": \"db162c34-78df-493c-93bb-8baa7694a783\" }";

        given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload)
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"));
    }

    @Test
    @DisplayName("testing update COS item - 200")
    void updateCOSItem() {
        String jsonPayload = "{ " +
                "\"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:COS\"]," +
                "\"name\": \"IudxCosPM30\"," +
                "\"owner\": \"" + owner_id + "\"," +
                "\"id\": \"" + cos_id + "\"," +
                "\"description\": \"COS item for postman collection\"," +
                "\"cosURL\": \"cat-test.iudx.io\"," +
                "\"cosUI\": \"cat-test.iudx.io\"" +
                "}";
        given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload)
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"));
    }

    @Test
    @DisplayName("testing update DX Resource Server Item - 200")
    void updateDXResourceItem(){
        String jsonPayload = "{ " +
                "\"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:ResourceServer\"]," +
                "\"name\": \"IudxResourceServerPM\"," +
                "\"id\": \"" + resource_server_id + "\"," +
                "\"description\": \"Multi tenanted IUDX resource server for postman collection\"," +
                "\"tags\": [" +
                "\"IUDX\"," +
                "\"Resource\"," +
                "\"Server\"," +
                "\"Platform\"" +
                "]," +
                "\"cos\": \"" + cos_id + "\"," +
                "\"owner\": \"" + owner_id + "\"," +
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
                "\"type\": [" +
                "\"iudx:HTTPAccess\"" +
                "]," +
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
                "}";
        given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload)
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .body("title", is("Success"));
    }

    @Test
    @DisplayName("testing updation of DX Provider Item - 200")
    void updateDXProviderItem() {
        String jsonPayload = "{ " +
                "\"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:Provider\"]," +
                "\"name\": \"IudxProviderPM\"," +
                "\"id\": \"" + provider_id + "\"," +
                "\"resourceServer\": \"" + resource_server_id + "\"," +
                "\"description\": \"provider for the postman collection\"," +
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
                "}";
        given()
                .contentType("application/json")
                .header("token", adminToken)
                .body(jsonPayload)
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }

    @Test
    @DisplayName("testing updation of DX Resource Group Item - 200")
    void updateDXResourceGroupItem() {
        String jsonPayload = "{ " +
                "\"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:ResourceGroup\", \"iudx:IssueReporting\"]," +
                "\"name\": \"iudxResourceGroupPM\"," +
                "\"id\": \"" + resource_group_id + "\"," +
                "\"description\": \"resource group item for the postman collection\"," +
                "\"tags\": [" +
                "\"swachhata\"," +
                "\"complaints\"," +
                "\"construction material\"," +
                "\"requests\"" +
                "]," +
                "\"provider\": \"" + provider_id + "\"" +
                "}";
        given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload)
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing updation of DX Resource Item - 200")
    void updateDXResourceItem200() {
        String jsonPayload = "{ " +
                "\"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:Resource\", \"iudx:PointOfInterest\"]," +
                "\"name\": \"iudxResourceItemPM\"," +
                "\"id\": \"" + resource_item_id + "\"," +
                "\"label\": \"item for tesr only\"," +
                "\"description\": \"resource item for the postman collection\"," +
                "\"tags\": [" +
                "\"swachhata\"," +
                "\"complaints\"," +
                "\"construction material\"," +
                "\"requests\"" +
                "]," +
                "\"apdURL\": \"rs.apd.iudx.org.in\"," +
                "\"accessPolicy\": \"SECURE\"," +
                "\"resourceServer\": \"" + resource_server_id + "\"," +
                "\"provider\": \"" + provider_id + "\"," +
                "\"resourceGroup\": \"" + resource_group_id + "\"" +
                "}";
        given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload)
                .when()
                .put("/item/")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }

    @Test
    @DisplayName("testing updation of DX Entity - 400")
    void updateDXEntity400() {
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
                "\"provider\": \"e0760835-6066-301b-b224-\"" +
                "}";

        given()
                .contentType("application/json")
                .header("token", cosAdminToken)
                .body(jsonPayload)
                .when()
                .put("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:InvalidSchema"));
    }

    @Test
    @DisplayName("testing updation of DX Entity - 400 (Invalid links)")
    void updateDXEntity400InvalidLinks() {
        String jsonPayload = "{ " +
                "\"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:Resource\", \"iudx:PointOfInterest\"]," +
                "\"name\": \"wifi-location\"," +
                "\"label\": \"Wi-Fi Locations in Pimpri-Chinchwad City\"," +
                "\"description\": \"The physical coordinates of Wi-Fi system locations in Pimpri-Chinchwad city.\"," +
                "\"tags\": [" +
                "\"Wi-Fi\"," +
                "\"Wi-Fi zone\"," +
                "\"hotspot\"," +
                "\"internet\"," +
                "\"Wi-Fi access\"," +
                "\"Wi-Fi location\"," +
                "\"wireless internet\"," +
                "\"internet access\"" +
                "]," +
                "\"apdURL\": \"rs.apd.iudx.org.in\"," +
                "\"accessPolicy\": \"SECURE\"," +
                "\"resourceServer\": \"" + resource_server_id + "\"," +
                "\"provider\": \"" + provider_id + "\"," +
                "\"resourceGroup\": \"83995e8c-fa80-4241-93c0-e86a66154eb6\"" +
                "}";

        given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload)
                .when()
                .put("/item/")
                .then()
                .statusCode(400)
                .body("type", is("urn:dx:cat:LinkValidationFailed"));
    }

    @Test
    @DisplayName("testing update DX Entity - 401 Invalid Credentials")
    void updateDXEntityInvalidCredentials() {
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
                .put("/item/")
                .then()
                .statusCode(401)
                .body("type", is("urn:dx:cat:InvalidAuthorizationToken"));
    }

    @Test
    @DisplayName("testing update DX Entity - 404 Not Found")
    void updateDXEntityNotFound() {
        String jsonPayload = "{ " +
                "\"@context\": \"https://voc.iudx.org.in/\"," +
                "\"type\": [\"iudx:Resource\", \"iudx:PointOfInterest\"]," +
                "\"name\": \"wyfy-location\"," +
                "\"label\": \"Wi-Fi Locations in Pimpri-Chinchwad City\"," +
                "\"description\": \"The physical coordinates of Wi-Fi system locations in Pimpri-Chinchwad city.\"," +
                "\"tags\": [" +
                "\"Wi-Fi\"," +
                "\"Wi-Fi zone\"," +
                "\"hotspot\"," +
                "\"internet\"," +
                "\"Wi-Fi access\"," +
                "\"Wi-Fi location\"," +
                "\"wireless internet\"," +
                "\"internet access\"" +
                "]," +
                "\"apdURL\": \"rs.apd.iudx.org.in\"," +
                "\"accessPolicy\": \"SECURE\"," +
                "\"resourceServer\": \"" + resource_server_id + "\"," +
                "\"provider\": \"" + provider_id + "\"," +
                "\"resourceGroup\": \"" + resource_group_id + "\"" +
                "}";

        given()
                .contentType("application/json")
                .header("token", token)
                .body(jsonPayload)
                .when()
                .put("/item/")
                .then()
                .statusCode(404)
                .body("type", is("urn:dx:cat:ItemNotFound"));
    }
}
