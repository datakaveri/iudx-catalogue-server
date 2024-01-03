package iudx.catalogue.server.apiserver.integrationtests.searchAPIsIT;

import io.restassured.response.Response;
import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import iudx.catalogue.server.apiserver.integrationtests.listItemsIT.ListItemsIT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final Logger LOGGER = LogManager.getLogger(RelationshipSearchIT.class);
    @Test
    @DisplayName("testing search a relationship_1 - 200 Success")
    void GetSearchRel1() {
        Response response = given()
                .queryParam("relationship","[provider.name]")
                .queryParam("value","[[IUDXAdmin]]")
                .when()
                .get("/relsearch")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }
    @Test
    @DisplayName("testing search a relationship_2 - 200 Success")
    void GetSearchRel2() {
        Response response = given()
                .queryParam("relationship","[provider.providerOrg.location.geometry.type]")
                .queryParam("value","[[Point]]")
                .when()
                .get("/relsearch")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:cat:Success"))
                .extract()
                .response();
        //Log the entire response details
        LOGGER.debug("Response details:\n" + response.prettyPrint());
    }
}
