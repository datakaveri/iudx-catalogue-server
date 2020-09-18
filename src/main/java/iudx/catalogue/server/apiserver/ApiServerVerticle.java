package iudx.catalogue.server.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.core.http.HttpServerResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.validator.ValidatorService;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.Constants.*;

/**
 * The Catalogue Server API Verticle.
 *
 * <h1>Catalogue Server API Verticle</h1>
 *
 * <p>
 * The API Server verticle implements the IUDX Catalogue Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 *
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @version 1.0
 * @since 2020-05-31
 */
public class ApiServerVerticle extends AbstractVerticle {

  private HttpServer server;
  private CrudApis crudApis;
  private SearchApis searchApis;
  private ListApis listApis;
  private RelationshipApis relApis;

  @SuppressWarnings("unused")
  private Router router;

  private String catAdmin;
  private String keystore;
  private String keystorePassword;

  private Properties properties;
  private InputStream inputstream;

  /* Addresses */
  private static final String DATABASE_SERVICE_ADDRESS = "iudx.catalogue.database.service";
  private static final String AUTH_SERVICE_ADDRESS = "iudx.catalogue.authentication.service";
  private static final String VALIDATION_SERVICE_ADDRESS = "iudx.catalogue.validator.service";

  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);

  /**
   * This method is used to start the Verticle and joing a cluster
   *
   * @throws Exception which is a startup exception
   */
  @Override
  public void start() throws Exception {

    router = Router.router(vertx);

    properties = new Properties();
    inputstream = null;

    /* Read the configuration and set the HTTPs server properties. */
    try {
      inputstream = new FileInputStream(CONFIG_FILE);
      properties.load(inputstream);
      catAdmin = properties.getProperty(CAT_ADMIN);
      keystore = properties.getProperty(KEYSTORE_PATH);
      keystorePassword = properties.getProperty(KEYSTORE_PASSWORD);
    } catch (Exception ex) {
      LOGGER.info(ex.toString());
    }


    /** Instantiate this server */
    server = vertx.createHttpServer(new HttpServerOptions()
        .setSsl(true)
        .setKeyStoreOptions(new JksOptions()
          .setPath(keystore)
          .setPassword(keystorePassword))
        .setCompressionSupported(true)
        .setCompressionLevel(5));


    /** API Callback managers */
    crudApis = new CrudApis();
    searchApis = new SearchApis();
    listApis = new ListApis();
    relApis = new RelationshipApis();

    /**
     *
     * Get proxies and handlers
     *
     */


    DatabaseService dbService 
      = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);

    crudApis.setDbService(dbService);
    listApis.setDbService(dbService);
    searchApis.setDbService(dbService);
    relApis.setDbService(dbService);

    AuthenticationService authService =
        AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    crudApis.setAuthService(authService);

    ValidatorService validationService =
        ValidatorService.createProxy(vertx, VALIDATION_SERVICE_ADDRESS);
    crudApis.setValidatorService(validationService);

    /**
     *
     * API Routes and Callbacks
     *
     */

    /** 
     * Routes - Defines the routes and callbacks
     */
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.route().handler(CorsHandler.create("*").allowedHeaders(ALLOWED_HEADERS));

    /**
     * Documentation routes
     */
    /* Static Resource Handler */
    /* Get openapiv3 spec */
    router.get(ROUTE_STATIC_SPEC)
      .produces(MIME_APPLICATION_JSON)
      .handler( routingContext -> {
        HttpServerResponse response = routingContext.response();
        response.sendFile("docs/openapi.yaml");
      });
    /* Get redoc */
    router.get(ROUTE_DOC)
      .produces(MIME_TEXT_HTML)
      .handler( routingContext -> {
        HttpServerResponse response = routingContext.response();
        response.sendFile("docs/apidoc.html");
      });


    /**
     * Routes for item CRUD
     */
    /* Create Item - Body contains data */
    router.post(ROUTE_ITEMS)
      .consumes(MIME_APPLICATION_JSON)
      .produces(MIME_APPLICATION_JSON)
      .handler( routingContext -> {
        /* checking auhthentication info in requests */
        if (routingContext.request().headers().contains(HEADER_TOKEN)) {
          crudApis.createItemHandler(routingContext);
        } else {
          LOGGER.warn("Fail: Unathorized CRUD operation");
          routingContext.response().setStatusCode(401).end();
        }
      });

    /* Get Item */
    router.get(ROUTE_ITEMS)
      .produces(MIME_APPLICATION_JSON)
      .handler( routingContext -> {
        crudApis.getItemHandler(routingContext);
      });

    /* Update Item - Body contains data */
    router.put(ROUTE_UPDATE_ITEMS)
      .consumes(MIME_APPLICATION_JSON)
      .produces(MIME_APPLICATION_JSON)
      .handler(routingContext -> {
        /* checking auhthentication info in requests */
        if (routingContext.request().headers().contains(HEADER_TOKEN)) {
          /** Update params checked in createItemHandler */
          crudApis.createItemHandler(routingContext);
        } else {
          LOGGER.warn("Unathorized CRUD operation");
          routingContext.response().setStatusCode(401).end();
        }
      });

    /* Delete Item - Query param contains id */
    router.delete(ROUTE_DELETE_ITEMS)
      .produces(MIME_APPLICATION_JSON)
      .handler(routingContext -> {
        /* checking auhthentication info in requests */
        if (routingContext.request().headers().contains(HEADER_TOKEN) &&
            routingContext.queryParams().contains(ID)) {
          /** Update params checked in createItemHandler */
          crudApis.deleteItemHandler(routingContext);
        } else {
          LOGGER.warn("Unathorized CRUD operation");
          routingContext.response().setStatusCode(401).end();
        }
      });

    /* Create instance - Instance name in query param */
    router.post(ROUTE_INSTANCE)
      .produces(MIME_APPLICATION_JSON)
      .handler(routingContext -> {
        /* checking auhthentication info in requests */
        if (routingContext.request().headers().contains(HEADER_TOKEN)) {
          crudApis.createInstanceHandler(routingContext, catAdmin);
        } else {
          LOGGER.warn("Fail: Unathorized CRUD operation");
          routingContext.response().setStatusCode(401).end();
        }
      });

    /* Delete instance - Instance name in query param */
    router.delete(ROUTE_INSTANCE)
      .produces(MIME_APPLICATION_JSON)
      .handler(routingContext -> {
        /* checking auhthentication info in requests */
        LOGGER.debug("Info: HIT instance");
        if (routingContext.request().headers().contains(HEADER_TOKEN)) {
          crudApis.deleteInstanceHandler(routingContext, catAdmin);
        } else {
          LOGGER.warn("Fail: Unathorized CRUD operation");
          routingContext.response().setStatusCode(401).end();
        }
      });

    /**
     * Routes for search and count
     */
    /* Search for an item */
    router.get(ROUTE_SEARCH)
      .produces(MIME_APPLICATION_JSON)
      .handler( routingContext -> {
        searchApis.searchHandler(routingContext);
      });

    /* Count the Cataloque server items */
    router.get(ROUTE_COUNT)
      .produces(MIME_APPLICATION_JSON)
      .handler( routingContext -> {
        searchApis.searchHandler(routingContext);
      });


    /**
     * Routes for list
     */
    /* list the item from database using itemId */
    router.get(ROUTE_LIST_ITEMS)
      .produces(MIME_APPLICATION_JSON)
      .handler(routingContext -> { 
        listApis.listItemsHandler(routingContext);
      });

    /**
     * Routes for relationships
     */
    /* Relationship related search */
    router.get(ROUTE_REL_SEARCH)
      .handler( routingContext -> {
        relApis.relSearchHandler(routingContext);
      });

    /* Get all resources belonging to a resource group */
    router.get(ROUTE_RELATIONSHIP).handler(routingContext -> {
      relApis.listRelationshipHandler(routingContext);
    });



    /**
     * Start server 
     */
    server.requestHandler(router).listen(PORT);

  }
}
