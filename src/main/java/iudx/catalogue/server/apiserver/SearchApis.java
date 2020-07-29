/**
 * <h1>SearchApis.java</h1>
 * Callback handlers for CRUD
 */

package iudx.catalogue.server.apiserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import static iudx.catalogue.server.apiserver.util.Constants.*;

import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.validator.ValidatorService;
import iudx.catalogue.server.authenticator.AuthenticationService;

interface SearchApisInterface {
  void setDbService(DatabaseService dbService);
}

public final class SearchApis implements SearchApisInterface {


  private DatabaseService dbService;

  private static final Logger LOGGER = LogManager.getLogger(SearchApis.class);


  /**
   * Crud  constructor
   *
   * @param DBService DataBase Service class
   * @return void
   * @TODO Throw error if load failed
   */
  public SearchApis() {
  }

  public void setDbService(DatabaseService dbService) {
    this.dbService = dbService;
  }


}
