package iudx.catalogue.server.apiserver.integrationtests.nlpapis;

import iudx.catalogue.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/* Rest Assured Integration tests for retrieving information about a location using valid and
* invalid questions in NLP Search APIs in Catalogue Server APIs*/

@ExtendWith(RestAssuredConfiguration.class)
public class NlpSearch {
    String question= "where can I halt near chandigarh";
    String inValidQuestion ="where can I halt near chandiga";
    @Test
    @DisplayName("NLP Search by Question Success Test-200")
    public void nlpSearchByQuestionTest(){
        given()
                .queryParam("q",question)
                .header("Content-Type","application/json")
                .when()
                .get("/nlpsearch")
                .then()
                .statusCode(200)
                .log().body()
                .body("type",equalTo("urn:dx:cat:Success"));
    }
    @Test
    @DisplayName("NLP Search by Invalid Question Test-404")
    public void nlpSearchByInvalidQuestionTest(){
        given()
                .queryParam("q",inValidQuestion)
                .header("Content-Type","application/json")
                .when()
                .get("/nlpsearch")
                .then()
                .statusCode(404)
                .log().body()
                .body("type",equalTo("urn:dx:cat:ItemNotFound"));
    }
}
