package iudx.catalogue.server.apiserver.integrationTests.searchAPIsIT;

import iudx.catalogue.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the search APIs by relationship search in the Catalog Server.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class RelationshipSearchIT {
    @Test
    @DisplayName("testing search a relationship_1 - 200 Success")
    void GetSearchRel1() {
        given()
                .queryParam("relationship","[provider.name]")
                .queryParam("value","[[IUDXAdmin]]")
                .when()
                .get("/relsearch")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("testing search a relationship_2 - 200 Success")
    void GetSearchRel2() {
        given()
                .queryParam("relationship","[provider.providerOrg.location.geometry.type]")
                .queryParam("value","[[Point]]")
                .when()
                .get("/relsearch")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"));
    }
}
