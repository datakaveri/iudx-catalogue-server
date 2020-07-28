package iudx.catalogue.server.apiserver;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import iudx.catalogue.server.apiserver.util.QueryMapper;
import iudx.catalogue.server.apiserver.util.ResponseHandler;

import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.validator.ValidatorService;

import static iudx.catalogue.server.apiserver.util.Constants.*;

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


  private ClusterManager mgr;
  private VertxOptions options;

  private ServiceDiscovery discovery;

  private DatabaseService dbService;
  private ValidatorService validationService;
  private AuthenticationService authService;

  private HttpServer server;
  private CrudApis crudApis;

  @SuppressWarnings("unused")
  private Router router;

  private String keystore;
  private String keystorePassword;

  private Properties properties;
  private InputStream inputstream;

  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);

  /**
   * This method is used to start the Verticle and joing a cluster
   *
   * @throws Exception which is a startup exception
   */
  @Override
  public void start() throws Exception {

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /* Create or Join a Vert.x Cluster. */
    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {

        Vertx vertx = res.result();
        router = Router.router(vertx);

        properties = new Properties();
        inputstream = null;
        /* Read the configuration and set the HTTPs server properties. */
        try {
          inputstream = new FileInputStream(CONFIG_FILE);
          properties.load(inputstream);
          keystore = properties.getProperty(KEYSTORE_FILE_NAME);
          keystorePassword = properties.getProperty(KEYSTORE_FILE_PASSWORD);
        } catch (Exception ex) {
          LOGGER.info(ex.toString());
        }


        /** Instantiate this server */
        server = vertx.createHttpServer(new HttpServerOptions()
                                            .setSsl(true)
                                            .setKeyStoreOptions(new JksOptions()
                                                                .setPath(keystore)
                                                                .setPassword(keystorePassword)));


        /** Callback managers */
        crudApis = new CrudApis();

        /**
         *
         * Get proxies and handlers
         *
         */

        /* Handler for service discovery */
        discovery = ServiceDiscovery.create(vertx);

        /* Handler for DatabaseService from service discovery */
        EventBusService.getProxy(discovery, DatabaseService.class,
            ar -> {
              if (ar.succeeded()) {
                dbService = ar.result();
                crudApis.setDbService(dbService);
                LOGGER.info("Service Discovery Success. Service name is : "
                        + dbService.getClass().getName());
              } else {
                LOGGER.fatal("DatabaseService Discovery Failed");
              }
            });

        /* Handler for AuthenticationService from service discovery*/
        EventBusService.getProxy(discovery, AuthenticationService.class,
            ar -> {
              if (ar.succeeded()) {
                authService = ar.result();
                crudApis.setAuthService(authService);
                LOGGER.info("Service Discovery Success. Service name is : "
                        + authService.getClass().getName());
              } else {
                LOGGER.fatal("Auth Discovery Failed");
              }
            });

        /* Handler for ValidatorService from service discovery*/
        EventBusService.getProxy(discovery, ValidatorService.class,
            ar -> {
              if (ar.succeeded()) {
                validationService = ar.result();
                crudApis.setValidatorService(validationService);
                LOGGER.info("Service Discovery Success. Service name is : "
                        + validationService.getClass().getName());
              } else {
                LOGGER.fatal("ValidatorService Discovery Failed");
              }
            });


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

        /** Static Resource Handler */
        router.route(ROUTE_STATIC).handler(StaticHandler.create());

        /**
         * Routes for item creation, deletion and updation
         */
        /* Create Item */
        router.post(ROUTE_ITEMS).produces(MIME_APPLICATION_JSON).handler( routingContext -> {
          /* checking auhthentication info in requests */
          if (routingContext.request().headers().contains(HEADER_TOKEN)) {
            crudApis.createItemHandler(routingContext);
          } else {
            LOGGER.warn("Unathorized CUD operation");
            routingContext.response().setStatusCode(401)
                          .end(new ResponseHandler.Builder()
                                .withStatus(INVALID_VALUE)
                                .build().toJsonString());
          }
        });

        /* Update Item */
        router.put(ROUTE_UPDATE_ITEMS).handler(this::updateItem);
        /* Delete Item */
        router.delete(ROUTE_DELETE_ITEMS).handler(this::deleteItem);

        /* Search for an item */
        router.get(ROUTE_SEARCH).handler(this::search);

        /* list all the tags */
        router.get(ROUTE_TAGS).handler(this::listTags);

        /* list all the domains */
        router.get(ROUTE_DOMAINS).handler(this::listDomains);

        /* list all the cities associated with the cataloque instance */
        router.get(ROUTE_CITIES).handler(this::listCities);

        /* list all the resource server associated with the cataloque instance */
        router.get(ROUTE_RESOURCE_SERVERS).handler(this::listResourceServers);

        /* list all the providers associated with the cataloque instance */
        router.get(ROUTE_PROVIDERS).handler(this::listProviders);

        /* list all the resource groups associated with the cataloque instance */
        router.get(ROUTE_RESOURCE_GROUPS).handler(this::listResourceGroups);


        /* list the item from database using itemId */
        router.get(ROUTE_LIST_ITEMS).handler(this::listItems);

        /* Get all resources belonging to a resource group */
        router.getWithRegex(ROUTE_LIST_RESOURCE_REL)
            .handler(this::listResourceRelationship);

        /* Get resource group of an item belonging to a resource */
        router.getWithRegex(ROUTE_LIST_RESOURCE_GROUP_REL)
            .handler(this::listResourceGroupRelationship);

        /* Gets the cities configuration from the database */
        router.get(ROUTE_UI_CITIES).handler(this::getCities);

        /* Create the cities configuration from the database */
        router.post(ROUTE_UI_CITIES).handler(this::setCities);

        /* Updates the cities configuration from the database */
        router.put(ROUTE_UI_CITIES).handler(this::updateCities);

        /* Get all the configuration */
        router.get(ROUTE_UI_CONFIG).handler(this::getConfig);

        /* Creates the configuration */
        router.post(ROUTE_UI_CONFIG).handler(this::setConfig);

        /* Deletes the configuration */
        router.delete(ROUTE_UI_CONFIG).handler(this::deleteConfig);

        /* Updates the existing configuration */
        router.put(ROUTE_UI_CONFIG).handler(this::updateConfig);

        /* Patches the existing configuration */
        router.patch(ROUTE_UI_CONFIG).handler(this::appendConfig);

        /* Get provider relationship to an item */
        router.getWithRegex(ROUTE_PROVIDER_REL).handler(this::listProviderRelationship);

        /* Get resource server relationship to an item */
        router.getWithRegex(ROUTE_RESOURCE_SERVER_REL)
            .handler(this::listResourceServerRelationship);

        /* Get list types with the database for an item */
        router.getWithRegex(ROUTE_DATA_TYPE).handler(this::listTypes);

        /* Count the Cataloque server items */
        router.get(ROUTE_COUNT).handler(this::count);


        /** Start server */
        server.requestHandler(router).listen(PORT);


      }
    });
  }

  /**
   * Processes the attribute, geoSpatial, and text search requests and returns the results from the
   * database.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void search(RoutingContext routingContext) {

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Collection of query parameters from HTTP request */
    MultiMap queryParameters = routingContext.queryParams();

    LOGGER.info("routed to search");
    LOGGER.info(request.params().toString());

    /* validating proper actual query parameters from request */
    if ((request.getParam(PROPERTY) == null || request.getParam(VALUE) == null)
        && (request.getParam(GEOPROPERTY) == null
            || request.getParam(GEOREL) == null
            || request.getParam(GEOMETRY) == null
            || request.getParam(COORDINATES) == null)
        && request
            .getParam(Q_VALUE) == null /*
                                                  * || request.getParam(LIMIT) == null ||
                                                  * request.getParam(OFFSET) == null
                                                  */) {

      LOGGER.error("Invalid Syntax");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(INVALID_SYNTAX)
              .build().toJsonString());
      return;

      /* checking the values of the query parameters */
    } else if (request.getParam(PROPERTY) != null
        && !request.getParam(PROPERTY).isBlank()) {

      /* converting query parameters in json */
      requestBody = QueryMapper.map2Json(queryParameters);

      /* checking the values of the query parameters for geo related count */
    } else if (LOCATION.equals(request.getParam(GEOPROPERTY))
        && GEOMETRIES.contains(request.getParam(GEOMETRY))
        && GEORELS.contains(request.getParam(GEOREL))) {

      requestBody = QueryMapper.map2Json(queryParameters);

      /* checking the values of the query parameters */
    } else if (request.getParam(Q_VALUE) != null
        && !request.getParam(Q_VALUE).isBlank()) {

      requestBody = QueryMapper.map2Json(queryParameters);

    } else {
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(INVALID_VALUE)
              .build().toJsonString());
      return;
    }

    if (requestBody != null) {
      requestBody.put(INSTANCE_ID_KEY, instanceID);
      dbService.searchQuery(requestBody, handler -> {
        if (handler.succeeded()) {
          JsonObject resultJson = handler.result();
          String status = resultJson.getString(STATUS);
          if (status.equalsIgnoreCase(SUCCESS)) {
            response.setStatusCode(200);
          } else if (status.equalsIgnoreCase(PARTIAL_CONTENT)) {
            response.setStatusCode(206);
          } else {
            response.setStatusCode(400);
          }
          response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .add(HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
          response.write(resultJson.toString());
          LOGGER.info("response: " + resultJson);
          response.end();
        } else if (handler.failed()) {
          LOGGER.error(handler.cause().getMessage());
          response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .setStatusCode(400).end(handler.cause().getMessage());
        }
      });
    } else {
      LOGGER.error("Invalid request query parameters");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(INVALID_VALUE)
              .build().toJsonString());
    }
  }

  /**
   * Queries the database and returns the city config for the instanceID.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void getCities(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    String instanceID = routingContext.request().host();
    JsonObject queryJson = new JsonObject();
    queryJson.put(INSTANCE_ID_KEY, instanceID).put(OPERATION,
        GET_CITIES);
    LOGGER.info("search query : " + queryJson);
    dbService.getCities(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(STATUS);
        if (status.equalsIgnoreCase(SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .add(HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        LOGGER.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        LOGGER.info(handler.cause().getMessage());
        response.headers().add(HEADER_CONTENT_TYPE, TEXT);
        response.setStatusCode(500);
        response.end(INTERNAL_SERVER_ERROR);
      }
    });
  }

  /**
   * Creates city config for the obtained instanceID.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void setCities(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = routingContext.getBodyAsJson();
    String instanceID = routingContext.request().host();
    validationService.validateItem(queryJson, validationHandler -> {
      if (validationHandler.succeeded()) {
        queryJson.put(INSTANCE_ID_KEY, instanceID);
        LOGGER.info("search query : " + queryJson);
        dbService.setCities(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString(STATUS);
            if (status.equalsIgnoreCase(SUCCESS)) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .add(HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            LOGGER.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            dbHandler.cause().getMessage();
            response.headers().add(HEADER_CONTENT_TYPE, TEXT);
            response.setStatusCode(500);
            response.end(INTERNAL_SERVER_ERROR);
          }
        });
      } else if (validationHandler.failed()) {
        LOGGER.info(validationHandler.cause().getMessage());
        response.headers().add(HEADER_CONTENT_TYPE, TEXT);
        response.setStatusCode(400);
        response.end(BAD_REQUEST);
      }
    });
  }

  /**
   * Updates city config for the obtained instanceID.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void updateCities(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = routingContext.getBodyAsJson();
    String instanceID = routingContext.request().host();
    validationService.validateItem(queryJson, validationHandler -> {
      if (validationHandler.succeeded()) {
        queryJson.put(INSTANCE_ID_KEY, instanceID);
        LOGGER.info("search query : " + queryJson);
        dbService.updateCities(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString(STATUS);
            if (status.equalsIgnoreCase(SUCCESS)) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .add(HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            LOGGER.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            LOGGER.info(dbHandler.cause().getMessage());
            response.headers().add(HEADER_CONTENT_TYPE, TEXT);
            response.setStatusCode(500);
            response.end(INTERNAL_SERVER_ERROR);
          }
        });
      } else if (validationHandler.failed()) {
        LOGGER.info(validationHandler.cause().getMessage());
        response.headers().add(HEADER_CONTENT_TYPE, TEXT);
        response.setStatusCode(400);
        response.end(BAD_REQUEST);
      }
    });
  }

  /**
   * Queries the database and returns the config for the obtained instanceID.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void getConfig(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    String instanceID = routingContext.request().host();
    JsonObject queryJson = new JsonObject();
    queryJson.put(INSTANCE_ID_KEY, instanceID).put("operation", "getConfig");
    LOGGER.info("search query : " + queryJson);
    dbService.getConfig(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(STATUS);
        if (status.equalsIgnoreCase(SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .add(HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        LOGGER.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        LOGGER.info(handler.cause().getMessage());
        response.headers().add(HEADER_CONTENT_TYPE, TEXT);
        response.setStatusCode(500);
        response.end(INTERNAL_SERVER_ERROR);
      }
    });
  }

  /**
   * Creates config for the obtained instanceID.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void setConfig(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = routingContext.getBodyAsJson();
    String instanceID = routingContext.request().host();
    validationService.validateItem(queryJson, validationHandler -> {
      if (validationHandler.succeeded()) {
        queryJson.put(INSTANCE_ID_KEY, instanceID);
        LOGGER.info("search query : " + queryJson);
        dbService.setConfig(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString(STATUS);
            if (status.equalsIgnoreCase(SUCCESS)) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .add(HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            LOGGER.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            LOGGER.error(dbHandler.cause().getMessage());
            response.headers().add(HEADER_CONTENT_TYPE, TEXT);
            response.setStatusCode(500);
            response.end(INTERNAL_SERVER_ERROR);
          }
        });
      } else if (validationHandler.failed()) {
        LOGGER.error(validationHandler.cause().getMessage());
        response.headers().add(HEADER_CONTENT_TYPE, TEXT);
        response.setStatusCode(400);
        response.end(BAD_REQUEST);
      }
    });
  }

  /**
   * Deletes config of obtained instanceID.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void deleteConfig(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    String instanceID = routingContext.request().host();
    JsonObject queryJson = new JsonObject();
    queryJson.put(INSTANCE_ID_KEY, instanceID);
    LOGGER.info("search query : " + queryJson);
    dbService.deleteConfig(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(STATUS);
        if (status.equalsIgnoreCase(SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .add(HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        LOGGER.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        LOGGER.error(handler.cause().getMessage());
        response.headers().add(HEADER_CONTENT_TYPE, TEXT);
        response.setStatusCode(500);
        response.end(INTERNAL_SERVER_ERROR);
      }
    });
  }

  /**
   * Updates config of the obtained instanceID.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void updateConfig(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = routingContext.getBodyAsJson();
    String instanceID = routingContext.request().host();
    validationService.validateItem(queryJson, validationHandler -> {
      if (validationHandler.succeeded()) {
        queryJson.put(INSTANCE_ID_KEY, instanceID);
        LOGGER.info("search query : " + queryJson);
        dbService.updateConfig(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString(STATUS);
            if (status.equalsIgnoreCase(SUCCESS)) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .add(HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            LOGGER.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            LOGGER.error(dbHandler.cause().getMessage());
            response.headers().add(HEADER_CONTENT_TYPE, TEXT);
            response.setStatusCode(500);
            response.end(INTERNAL_SERVER_ERROR);
          }
        });
      } else if (validationHandler.failed()) {
        LOGGER.error(validationHandler.cause().getMessage());
        response.headers().add(HEADER_CONTENT_TYPE, TEXT);
        response.setStatusCode(400);
        response.end(BAD_REQUEST);
      }
    });
  }


  /**
   * Updates a already created item in the database. Endpoint: PATCH /iudx/cat/v1/update/itemId
   * itemId=ResourceItem/ResourceGroupItem/ResourceServerItem/ProviderItem/DataDescriptorItem
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void updateItem(RoutingContext routingContext) {

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    /* JsonObject of authentication related information */
    JsonObject authenticationInfo = new JsonObject();

    /* HTTP request body as Json */
    JsonObject requestBody = routingContext.getBodyAsJson();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Building complete itemID from HTTP request path parameters */
    String itemId =
        routingContext
            .pathParam(RESOURCE_ITEM)
            .concat("/")
            .concat(
                routingContext
                    .pathParam(RESOURCE_GRP_ITEM)
                    .concat("/")
                    .concat(routingContext.pathParam(RESOURCE_SVR_ITEM))
                    .concat("/")
                    .concat(routingContext.pathParam(PROVIDER_ITEM).concat("/"))
                    .concat(routingContext.pathParam(DATA_DES_ITEM)));

    LOGGER.info("Updating an item, Id: ".concat(itemId));

    /* checking and comparing itemType from the request body */
    if (requestBody.containsKey(ITEM_TYPE)
        && ITEM_TYPES.contains(requestBody.getString(ITEM_TYPE))) {
      /* Populating query mapper */
      requestBody.put(INSTANCE_ID_KEY, instanceID);

      /* checking auhthentication info in requests */
      if (request.headers().contains(HEADER_TOKEN)) {
        authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN))
            .put(OPERATION, PUT);


      String providerId = requestBody.getString(REL_PROVIDER);

      JsonObject authRequest = new JsonObject().put(REL_PROVIDER, providerId);


        /* Authenticating the request */
        authService.tokenInterospect(
            authRequest,
            authenticationInfo,
            authhandler -> {
              if (authhandler.failed()) {
                response
                    .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                    .setStatusCode(401)
                    .end(authhandler.cause().toString());
                return;
              }
              if (authhandler.result().getString(STATUS).equals(SUCCESS)) {
                LOGGER.info(
                    "Authenticating item update request ".concat(authhandler.result().toString()));
                /* Validating the request */
                validationService.validateItem(
                    requestBody,
                    valhandler -> {
                      if (valhandler.succeeded()) {
                        LOGGER.info(
                            "Item update validated ".concat(authhandler.result().toString()));
                        /* Requesting database service, creating a item */
                        dbService.updateItem(
                            requestBody,
                            dbhandler -> {
                              if (dbhandler.succeeded()) {
                                LOGGER.info("Item updated ".concat(dbhandler.result().toString()));
                                response
                                    .putHeader(
                                        HEADER_CONTENT_TYPE,
                                        MIME_APPLICATION_JSON)
                                    .setStatusCode(200)
                                    .end(dbhandler.result().toString());
                              } else if (dbhandler.failed()) {
                                LOGGER.error(
                                    "Item update failed ".concat(dbhandler.cause().toString()));
                                response
                                    .putHeader(
                                        HEADER_CONTENT_TYPE,
                                        MIME_APPLICATION_JSON)
                                    .setStatusCode(500)
                                    .end(dbhandler.cause().toString());
                              }
                            });
                      } else if (valhandler.failed()) {
                        LOGGER.error(
                            "Item validation failed ".concat(valhandler.cause().toString()));
                        response
                            .putHeader(
                                HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                            .setStatusCode(500)
                            .end(valhandler.cause().toString());
                      }
                    });
              } else {
                LOGGER.error("Unathorized request ".concat(authhandler.cause().toString()));
                response
                    .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                    .setStatusCode(401)
                    .end(authhandler.cause().toString());
              }
            });
      } else {
        LOGGER.error("InvalidHeader 'token' header");
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(new ResponseHandler.Builder()
                .withStatus(INVALID_HEADER).build().toJsonString());
      }
    } else {
      LOGGER.error("InvalidValue, 'itemType' attribute is missing or is empty");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(INVALID_VALUE)
              .build().toJsonString());
    }
  }

  /**
   * Deletes a created item in the database. Endpoint: DELETE /iudx/cat/v1/delete/itemId
   * itemId=ResourceItem/ResourceGroupItem/ResourceServerItem/ProviderItem/DataDescriptorItem
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void deleteItem(RoutingContext routingContext) {

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    /* JsonObject of authentication related information */
    JsonObject authenticationInfo = new JsonObject();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Building complete itemID from HTTP request path parameters */
    String itemId = "";
    itemId = itemId 
      + routingContext.pathParam(PROVIDER_ORG) + "/"
      + routingContext.pathParam(PROVIDER_ITEM) + "/"
      + routingContext.pathParam(RESOURCE_SVR_ITEM) + "/"
      + routingContext.pathParam(RESOURCE_GRP_ITEM) + "/"
      + routingContext.pathParam(RESOURCE_ITEM);


    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);
    requestBody.put(ID, itemId);
    // requestBody.put(ITEM_TYPE, "Resource/ResourceGroup");

    LOGGER.info("Deleting an item, Id: ".concat(itemId));

    /* checking auhthentication info in requests */
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo
          .put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN))
          .put(OPERATION, DELETE);

      String providerId = String.join("/", Arrays.copyOfRange(itemId.split("/"), 0, 2));
      LOGGER.info("Provider ID is  " + providerId);

      JsonObject authRequest = new JsonObject().put(REL_PROVIDER, providerId);

      /* Authenticating the request */
      authService.tokenInterospect(
          authRequest,
          authenticationInfo,
          authhandler -> {
            if (authhandler.failed()) {
              response
                .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .setStatusCode(401)
                .end(authhandler.cause().toString());
              return;
            }
            if (authhandler.result().getString(STATUS).equals(SUCCESS)) {
              LOGGER.info(
                  "Authenticating item delete request".concat(authhandler.result().toString()));
              /* Requesting database service, creating a item */
              dbService.deleteItem(
                  requestBody,
                  dbhandler -> {
                    if (dbhandler.succeeded()) {
                      LOGGER.info("Item deleted".concat(dbhandler.result().toString()));
                      response
                          .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                          .setStatusCode(200)
                          .end(dbhandler.result().toString());
                    } else if (dbhandler.failed()) {
                      LOGGER.error("Item deletion failed".concat(dbhandler.cause().toString()));
                      response
                          .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                          .setStatusCode(400)
                          .end(dbhandler.cause().toString());
                    }
                  });
            } else {
              LOGGER.error("Unathorized request".concat(authhandler.cause().toString()));
              response
                  .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                  .setStatusCode(401)
                  .end(authhandler.cause().toString());
            }
          });
    } else {
      LOGGER.error("Invalid 'token' header");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(INVALID_HEADER)
              .build().toJsonString());
    }
  }

  /**
   * List the items from database using itemId.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listItems(RoutingContext routingContext) {

    LOGGER.info("Listing items from database");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Building complete itemID from HTTP request path parameters */
    String itemId =
        routingContext
            .pathParam(RESOURCE_ITEM)
            .concat("/")
            .concat(
                routingContext
                    .pathParam(RESOURCE_GRP_ITEM)
                    .concat("/")
                    .concat(routingContext.pathParam(RESOURCE_SVR_ITEM))
                    .concat("/")
                    .concat(routingContext.pathParam(PROVIDER_ITEM).concat("/"))
                    .concat(routingContext.pathParam(DATA_DES_ITEM)));

    /* Populating query mapper */
    requestBody.put(ID, itemId);
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Databse service call for listing item */
    dbService.listItem(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of items ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing items ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of tags for a catalogue instance.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listTags(RoutingContext routingContext) {

    LOGGER.info("Listing tags of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing tags */
    dbService.listTags(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of tags ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing tags ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get a list of domains for a cataloque instance.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listDomains(RoutingContext routingContext) {

    LOGGER.info("Listing domains of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing domains */
    dbService.listDomains(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of domains ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing domains ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of cities and the catalogue instance ID.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listCities(RoutingContext routingContext) {

    LOGGER.info("Listing cities of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing cities */
    dbService.listCities(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of cities ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing cities ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of resourceServers for a catalogue instance.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listResourceServers(RoutingContext routingContext) {

    LOGGER.info("Listing resource servers of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing resource servers */
    dbService.listResourceServers(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of resource servers ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing resource servers ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of providers for a catalogue instance.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listProviders(RoutingContext routingContext) {

    LOGGER.info("Listing providers of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing providers */
    dbService.listProviders(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of providers ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing providers ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of resource groups for a catalogue instance.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listResourceGroups(RoutingContext routingContext) {

    LOGGER.info("Listing resource groups of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing resource groups */
    dbService.listResourceGroups(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of resource groups ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing resource groups ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get all resources belonging to a resourceGroup.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void listResourceRelationship(RoutingContext routingContext) {

    LOGGER.info("Searching for relationship of resource");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Parsing id from HTTP request */
    String id = request.getParam(ID);

    /* Checking if id is either not null nor empty */
    if (id != null && !id.isBlank()) {

      /* Populating query mapper */
      requestBody.put(ID, id);
      requestBody.put(INSTANCE_ID_KEY, instanceID);
      requestBody.put(RELATIONSHIP, REL_RESOURCE);

      /*
       * Request database service with requestBody for listing resource relationship
       */
      dbService.listResourceRelationship(requestBody, dbhandler -> {
        if (dbhandler.succeeded()) {
          LOGGER.info("List of resources belonging to resourceGroups "
              .concat(dbhandler.result().toString()));
          response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .setStatusCode(200).end(dbhandler.result().toString());
        } else if (dbhandler.failed()) {
          LOGGER.error(
              "Issue in listing resource relationship ".concat(dbhandler.cause().toString()));
          response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .setStatusCode(400).end(dbhandler.cause().toString());
        }
      });
    } else {
      LOGGER.error("Issue in path parameter");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(INVALID_SYNTAX)
              .build().toJsonString());
    }
  }

  /**
   * Get all resourceGroup relationships.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void listResourceGroupRelationship(RoutingContext routingContext) {

    LOGGER.info("Searching for relationship of resource and resourceGroup");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Parsing id from HTTP request */
    String id = request.getParam(ID);

    /* Checking if id is either not null nor empty */
    if (id != null && !id.isBlank()) {

      /* Populating query mapper */
      requestBody.put(ID, id);
      requestBody.put(INSTANCE_ID_KEY, instanceID);
      requestBody.put(RELATIONSHIP, REL_RESOURCE_GRP);

      /*
       * Request database service with requestBody for listing resource group relationship
       */
      dbService.listResourceGroupRelationship(requestBody, dbhandler -> {
        if (dbhandler.succeeded()) {
          LOGGER.info(
              "List of resourceGroup belonging to resource ".concat(dbhandler.result().toString()));
          response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .setStatusCode(200).end(dbhandler.result().toString());
        } else if (dbhandler.failed()) {
          LOGGER.error(
              "Issue in listing resourceGroup relationship ".concat(dbhandler.cause().toString()));
          response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .setStatusCode(400).end(dbhandler.cause().toString());
        }
      });
    } else {
      LOGGER.error("Issue in path parameter");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(INVALID_SYNTAX)
              .build().toJsonString());
    }
  }

  /**
   * Appends config to the obtained instanceID.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void appendConfig(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = routingContext.getBodyAsJson();
    String instanceID = routingContext.request().host();
    validationService.validateItem(queryJson, validationHandler -> {
      if (validationHandler.succeeded()) {
        queryJson.put(INSTANCE_ID_KEY, instanceID);
        LOGGER.info("search query : " + queryJson);
        dbService.appendConfig(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString(STATUS);
            if (status.equalsIgnoreCase(SUCCESS)) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .add(HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            LOGGER.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            LOGGER.error(dbHandler.cause().getMessage());
            response.headers().add(HEADER_CONTENT_TYPE, TEXT);
            response.setStatusCode(400);
            response.end(BAD_REQUEST);
          }
        });
      }
      });
  }

  /**
   * Queries the database and returns all resource servers belonging to an item.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void listResourceServerRelationship(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = new JsonObject();
    String instanceID = routingContext.request().host();
    String id = routingContext.request().getParam(ID);
    queryJson.put(INSTANCE_ID_KEY, instanceID).put(ID, id)
        .put(RELATIONSHIP, REL_RESOURCE_SVR);
    LOGGER.info("search query : " + queryJson);
    dbService.listResourceServerRelationship(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(STATUS);
        if (status.equalsIgnoreCase(SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .add(HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        LOGGER.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        LOGGER.error(handler.cause().getMessage());
        response.headers().add(HEADER_CONTENT_TYPE, TEXT);
        response.setStatusCode(500);
        response.end(INTERNAL_SERVER_ERROR);
      }
    });
  }

  /**
   * Queries the database and returns provider of an item.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void listProviderRelationship(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = new JsonObject();
    String instanceID = routingContext.request().host();
    String id = routingContext.request().getParam(ID);
    queryJson
        .put(INSTANCE_ID_KEY, instanceID)
        .put(ID, id)
        .put(RELATIONSHIP, REL_PROVIDER);
    LOGGER.info("search query : " + queryJson);
    dbService.listProviderRelationship(
        queryJson,
        handler -> {
          if (handler.succeeded()) {
            JsonObject resultJson = handler.result();
            String status = resultJson.getString(STATUS);
            if (status.equalsIgnoreCase(SUCCESS)) {
              response.setStatusCode(200);
            } else {
              response.setStatusCode(400);
            }
            response
                .headers()
                .add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .add(
                    HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            LOGGER.info("response : " + resultJson);
            response.end();
          } else if (handler.failed()) {
            LOGGER.error(handler.cause().getMessage());
            response.headers().add(HEADER_CONTENT_TYPE, TEXT);
            response.setStatusCode(500);
            response.end(INTERNAL_SERVER_ERROR);
          }
        });
  }

  /**
   * Queries the database and returns data model of an item.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void listTypes(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = new JsonObject();
    String instanceID = routingContext.request().host();
    String id = routingContext.request().getParam(ID);
    queryJson
        .put(INSTANCE_ID_KEY, instanceID)
        .put(ID, id)
        .put(RELATIONSHIP, REL_TYPE);
    LOGGER.info("search query : " + queryJson);
    dbService.listTypes(
        queryJson,
        handler -> {
          if (handler.succeeded()) {
            JsonObject resultJson = handler.result();
            String status = resultJson.getString(STATUS);
            if (status.equalsIgnoreCase(SUCCESS)) {
              response.setStatusCode(200);
            } else {
              response.setStatusCode(400);
            }
            response
                .headers()
                .add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .add(
                    HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            LOGGER.info("response : " + resultJson);
            response.end();
          } else if (handler.failed()) {
            LOGGER.error(handler.cause().getMessage());
            response.headers().add(HEADER_CONTENT_TYPE, TEXT);
            response.setStatusCode(500);
            response.end(INTERNAL_SERVER_ERROR);
          }
        });
  }

  /**
   * Counting the cataloque items.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void count(RoutingContext routingContext) {

    LOGGER.info("Counting the request parameters");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Collection of query parameters from HTTP request */
    MultiMap queryParameters = routingContext.queryParams();

    if (!queryParameters.contains(ATTRIBUTE_FILTER)) {

      /* validating proper actual query parameters from request */
      if ((request.getParam(PROPERTY) == null
          || request.getParam(VALUE) == null)
          && (request.getParam(GEOPROPERTY) == null
              || request.getParam(GEOREL) == null
              || request.getParam(GEOMETRY) == null
              || request.getParam(COORDINATES) == null)
          && request.getParam(Q_VALUE) == null) {

        LOGGER.error("Invalid Syntax");
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(new ResponseHandler.Builder()
                .withStatus(INVALID_SYNTAX).build().toJsonString());
        return;

        /* checking the values of the query parameters */
      } else if (request.getParam(PROPERTY) != null
          && !request.getParam(PROPERTY).isBlank()) {

        /* converting query parameters in json */
        requestBody = QueryMapper.map2Json(queryParameters);

        /* checking the values of the query parameters for geo related count */
      } else if (LOCATION.equals(request.getParam(GEOPROPERTY))
          && GEOMETRIES.contains(request.getParam(GEOMETRY))
          && GEORELS.contains(request.getParam(GEOREL))) {

        requestBody = QueryMapper.map2Json(queryParameters);

        /* checking the values of the query parameters */
      } else if (request.getParam(Q_VALUE) != null
          && !request.getParam(Q_VALUE).isBlank()) {

        requestBody = QueryMapper.map2Json(queryParameters);

      } else {
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(new ResponseHandler.Builder()
                .withStatus(INVALID_VALUE).build().toJsonString());
        return;
      }

      if (requestBody != null) {
        LOGGER.info("Count query : " + requestBody);
        requestBody.put(INSTANCE_ID_KEY, instanceID);

        /* Request database service with requestBody for counting */
        dbService.countQuery(requestBody, dbhandler -> {
          if (dbhandler.succeeded()) {
            LOGGER.info("Count query completed ".concat(dbhandler.result().toString()));
            response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .setStatusCode(200).end(dbhandler.result().toString());
          } else if (dbhandler.failed()) {
            LOGGER.error("Issue in count query ".concat(dbhandler.cause().toString()));
            response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .setStatusCode(400).end(dbhandler.cause().toString());
          }
        });
      } else {
        LOGGER.error("Invalid request query parameters");
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(new ResponseHandler.Builder()
                .withStatus(INVALID_VALUE).build().toJsonString());
      }
    } else {
      LOGGER.error("Invalid request query parameters");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(INVALID_VALUE)
              .build().toJsonString());
    }
  }
}
