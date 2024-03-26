package iudx.catalogue.server.apiserver.integrationtests.crudApisIT;

import static io.restassured.RestAssured.*;
import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_STACK;
import static iudx.catalogue.server.authenticator.TokensForITs.*;
import static org.hamcrest.Matchers.*;

import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RestAssuredConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StackRestApisIT {
  public static final Logger LOGGER = LogManager.getLogger(StackRestApisIT.class);

  public static String stacId;

  private JsonObject createPayload() {
    JsonObject jsonPayload = new JsonObject();
    jsonPayload.put("type", "Catalog");
    jsonPayload.put("stac_version", "1.0.0");
    jsonPayload.put(
        "description", "This object represents a Catalog in a SpatioTemporal Asset Catalog.");

    JsonArray linksArray = new JsonArray();
    linksArray.add(
        createLink(
            "child",
            "https://ogc.iudx.io/collections/af1c526b-283e-441b-a4fb-09e6760de9c7",
            "Link to the STAC collection"));
    linksArray.add(
        createLink(
            "child",
            "https://ogc.iudx.io/collections/f64743f6-1f74-4ada-8020-45cfb2d08a1b",
            "Link to the STAC collection"));
    linksArray.add(createLink("self", "https://ogc.iudx.io/catalog", "Link to the STAC catalog"));
    linksArray.add(createLink("root", "https://ogc.iudx.io/catalog", "Link to the STAC catalog"));

    jsonPayload.put("links", linksArray);
    return jsonPayload;
  }

  private JsonObject createChildObject() {
    return createLink(
        "child",
        "https://ogc.iudx.io/collections/af1c526b-283e-441b-a4fb-09e6760de9c7/test-3",
        "Link to the STAC collection");
  }

  private JsonObject createLink(String rel, String href, String title) {
    return new JsonObject()
        .put("rel", rel)
        .put("href", href)
        .put("type", "application/json")
        .put("title", title);
  }

  private void assertResponse(
      Response response, int statusCode, String type, String title, String detail) {
    response
        .then()
        .statusCode(statusCode)
        .body("type", is(type))
        .body("title", equalTo(title))
        .body("detail", equalTo(detail));
  }

  private void assertResponse(Response response, int statusCode, String type, String detail) {
    response.then().statusCode(statusCode).body("type", is(type)).body("detail", equalTo(detail));
  }

  private Response createStacItem(JsonObject payloadJson, String token) {
    LOGGER.debug("createStackItem payloadJson:: " + payloadJson);
    return given()
        .contentType("application/json")
        .header("token", token)
        .body(payloadJson.toString())
        .when()
        .post(ROUTE_STACK)
        .then()
        .extract()
        .response();
  }

  private Response updateStacItem(JsonObject payloadJson, String token) {
    LOGGER.debug("updateStackItem payloadJson:: " + payloadJson);
    return given()
        .contentType("application/json")
        .header("token", token)
        .body(payloadJson.toString())
        .when()
        .patch(ROUTE_STACK)
        .then()
        .extract()
        .response();
  }

  private Response getStacItem(String stackId) {
    LOGGER.debug("getStacItem stackId:: {} ", stackId);
    return given()
        .contentType("application/json")
        .param("id", stackId)
        .when()
        .get(ROUTE_STACK)
        .then()
        .extract()
        .response();
  }

  private Response deleteStackItem(String stackId, String token) {
    LOGGER.debug("deleteStacItem stackId::{} ", stackId);
    return given()
        .contentType("application/json")
        .header("token", token)
        .param("id", stackId)
        .when()
        .delete(ROUTE_STACK)
        .then()
        .extract()
        .response();
  }

  @Test
  @Order(1)
  @DisplayName("Create Stac Item - Success (201)")
  void testCreateStackItemSuccess() {
    JsonObject payloadJson = createPayload();
    Response response = createStacItem(payloadJson, cosAdminToken);

    // Extract the generated ID from the response
    JsonObject json = new JsonObject(response.body().prettyPrint());
    stacId = json.getJsonArray("results").getJsonObject(0).getString("id");
    LOGGER.info("Stac ID: {}", stacId);

    assertResponse(response, 201, "Success", "Stac created successfully.");
  }

  @Test
  @Order(2)
  @DisplayName("Create Stac Item - Conflict (409)")
  void testCreateStackItemConflicts() {
    try {
      Thread.sleep(2000); // 2 seconds delay
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    JsonObject payloadJson = createPayload();
    Response response = createStacItem(payloadJson, cosAdminToken);
    assertResponse(
        response,
        409,
        "urn:dx:cat:Conflicts",
        "Conflicts",
        "STAC already exists,creation skipped");
  }

  @Test
  @Order(3)
  @DisplayName("Create stac item - Invalid Authorization Token (401)")
  void createStackItemNotAuthorization() {
    JsonObject payloadJson = createPayload();
    Response response = createStacItem(payloadJson, adminToken);
    assertResponse(
        response,
        401,
        "urn:dx:cat:InvalidAuthorizationToken",
        "Token is invalid",
        "Incorrect audience value in jwt");
  }

  @Test
  @Order(4)
  @DisplayName("Create stac item - Bad Request (400)")
  void createStackItemInvalidSchema() {
    JsonObject payloadJson = createPayload();
    payloadJson.remove("links");
    Response response = createStacItem(payloadJson, adminToken);
    assertResponse(
        response, 400, "urn:dx:cat:InvalidSchema", "Invalid Schema", "Invalid schema provided");
  }

  @Test
  @Order(5)
  @DisplayName("Update stack item - Invalid Schema (400)")
  void updateStackItemInvalidSchema() {
    JsonObject payloadJson = createChildObject();
    payloadJson.remove("id");
    Response response = updateStacItem(payloadJson, adminToken);
    assertResponse(
        response, 400, "urn:dx:cat:InvalidSchema", "Invalid Schema", "Invalid schema provided");
  }

  @Test
  @Order(6)
  @DisplayName("Update stac item - Not Found (404)")
  void updateStackItemNotFound() {
    try {
      Thread.sleep(2000); // 2 seconds delay
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    JsonObject payloadJson = createChildObject();
    payloadJson.put("id", "f47ac10b-58cc-4372-a567-0e02b2c3d479");
    Response response = updateStacItem(payloadJson, cosAdminToken);
    assertResponse(
        response, 404, "urn:dx:cat:ItemNotFound", "Item is not found", "Fail: stac doesn't exist");
  }

  @Test
  @Order(7)
  @DisplayName("Update stackitem - Invalid Authorization Token (401)")
  void updateStackItemInvalidAuthorization() {
    JsonObject payloadJson = createChildObject();
    payloadJson.put("id", "f47ac10b-58cc-4372-a567-0e02b2c3d479");
    Response response = updateStacItem(payloadJson, adminToken);
    assertResponse(
        response,
        401,
        "urn:dx:cat:InvalidAuthorizationToken",
        "Token is invalid",
        "Incorrect audience value in jwt");
  }

  @Test
  @Order(8)
  @DisplayName("Update stac item - Success (200)")
  void updateStackItemSuccess() {
    LOGGER.debug("stacId {}", stacId);
    try {
      Thread.sleep(2000); // 2 seconds delay
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    JsonObject payloadJson = createChildObject();
    payloadJson.put("id", stacId);

    Response response = updateStacItem(payloadJson, cosAdminToken);
    assertResponse(
        response, 201, "urn:dx:cat:Success", "Success", "Success: Item updated successfully");
  }

  @Test
  @Order(8)
  @DisplayName("Get stac item - Success (200)")
  void getStackItemSuccess() {
    LOGGER.debug("stacId {}", stacId);
    try {
      Thread.sleep(2000); // 2 seconds delay
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Response response = getStacItem(stacId);
    response
        .then()
        .statusCode(200)
        .body("type", is("urn:dx:cat:Success"))
        .body("title", equalTo("Success"));
  }

  @Test
  @Order(9)
  @DisplayName("Get stack item - Not Found (404)")
  void getStackItemNotFound() {
    LOGGER.debug("stacId : {}", stacId);
    Response response = getStacItem("7c8a0478-7986-4d6e-91d1-2ba82bd22863");
    assertResponse(
        response, 404, "urn:dx:cat:ItemNotFound", "Item is not found", "Fail: Stac doesn't exist");
  }

  @Test
  @Order(10)
  @DisplayName("Get stack item - Bad Request (400) - Invalid Id")
  void getStackItemInvalidId() {
    Response response = getStacItem("761a9527-94ed-48d2-8995-b061cdfe1f9");
    assertResponse(
        response,
        400,
        "urn:dx:cat:InvalidUUID",
        "Invalid syntax of uuid",
        "The id is invalid or not present");
  }

  @Test
  @Order(11)
  @DisplayName("Delete stac item - Bad Request (400) - Invalid Id")
  void deleteStackItemInvalidId() {
    Response response = deleteStackItem("61a9527-94ed-48d2-8995-b061cdfe1f9", cosAdminToken);
    assertResponse(
        response,
        400,
        "urn:dx:cat:InvalidUUID",
        "Invalid syntax of uuid",
        "The id is invalid or not present");
  }

  @Test
  @Order(12)
  @DisplayName("Delete stac item - Invalid Authorization Token (401)")
  void deleteStackItemInvalidToken() {
    Response response = deleteStackItem("714f82e7-146b-411c-b2e1-8618141d8b99", adminToken);
    assertResponse(
        response,
        401,
        "urn:dx:cat:InvalidAuthorizationToken",
        "Token is invalid",
        "Incorrect audience value in jwt");
  }

  @Test
  @Order(13)
  @DisplayName("Delete stac item - Not Found (404)")
  void deleteStackItemNotFound() {
    LOGGER.debug("stackId : {} ", stacId);
    Response response = deleteStackItem("714f82e7-146b-411c-b2e1-8618141d8b99", cosAdminToken);
    assertResponse(response, 404, "urn:dx:cat:ItemNotFound", "Item not found, can't delete");
  }

  @Test
  @Order(14)
  @DisplayName("Delete stac item - Success (200)")
  void deleteStackItemSuccess() {
    LOGGER.debug("stacId : {} ", stacId);
    try {
      Thread.sleep(2000); // 2 seconds delay
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Response response = deleteStackItem(stacId, cosAdminToken);
    assertResponse(response, 200, "Success", "Stac deleted successfully.");
  }
}
