package iudx.catalogue.server.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
import iudx.catalogue.server.apiserver.util.Constants;
import iudx.catalogue.server.apiserver.util.QueryMapper;
import iudx.catalogue.server.apiserver.util.ResponseHandler;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.onboarder.OnboarderService;
import iudx.catalogue.server.validator.ValidatorService;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * The Catalogue Server API Verticle.
 *
 * <h1>Catalogue Server API Verticle</h1>
 *
 * <p>
 * The API Server verticle implements the IUDX Catalogue Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 * </p>
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

  private static final Logger logger = LoggerFactory.getLogger(ApiServerVerticle.class);
  private Vertx vertx;
  private ClusterManager mgr;
  private VertxOptions options;
  private ServiceDiscovery discovery;
  private DatabaseService database;
  private OnboarderService onboarder;
  private ValidatorService validator;
  private AuthenticationService authenticator;
  private HttpServer server;
  @SuppressWarnings("unused")
  private Router router;
  private Properties properties;
  private InputStream inputstream;
  private String keystore;
  private String keystorePassword;
  private ArrayList<String> itemTypes;
  private ArrayList<String> geoRels;
  private ArrayList<String> geometries;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, reads the
   * configuration, obtains a proxy for the Event bus services exposed through service discovery,
   * start an HTTPs server at port 8443.
   *
   * @throws Exception which is a startup exception
   */
  @Override
  public void start() throws Exception {

    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {

        vertx = res.result();
        router = Router.router(vertx);
        properties = new Properties();
        inputstream = null;

        /* HTTP request allowed headers */
        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add(Constants.HEADER_ACCEPT);
        allowedHeaders.add(Constants.HEADER_TOKEN);
        allowedHeaders.add(Constants.HEADER_CONTENT_LENGTH);
        allowedHeaders.add(Constants.HEADER_CONTENT_TYPE);
        allowedHeaders.add(Constants.HEADER_HOST);
        allowedHeaders.add(Constants.HEADER_ORIGIN);
        allowedHeaders.add(Constants.HEADER_REFERER);
        allowedHeaders.add(Constants.HEADER_CORS);

        /* Define the APIs, methods, endpoints and associated methods. */

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders));

        router.route(Constants.ROUTE_APIS).handler(StaticHandler.create());

        /* New item create */
        router.post(Constants.ROUTE_ITEMS).handler(this::createItem);

        /* Search for an item */
        router.get(Constants.ROUTE_SEARCH).handler(this::search);

        /* list all the tags */
        router.get(Constants.ROUTE_TAGS).handler(this::listTags);

        /* list all the domains */
        router.get(Constants.ROUTE_DOMAINS).handler(this::listDomains);

        /* list all the cities associated with the cataloque instance */
        router.get(Constants.ROUTE_CITIES).handler(this::listCities);

        /* list all the resource server associated with the cataloque instance */
        router.get(Constants.ROUTE_RESOURCE_SERVERS).handler(this::listResourceServers);

        /* list all the providers associated with the cataloque instance */
        router.get(Constants.ROUTE_PROVIDERS).handler(this::listProviders);

        /* list all the resource groups associated with the cataloque instance */
        router.get(Constants.ROUTE_RESOURCE_GROUPS).handler(this::listResourceGroups);

        /*
         * Update an item in the database using itemId [itemId=ResourceItem, ResourceGroupItem,
         * ResourceServerItem, ProviderItem, DataDescriptorItem]
         */
        router.put(Constants.ROUTE_UPDATE_ITEMS).handler(this::updateItem);

        /* Delete an item from database using itemId */
        router.delete(Constants.ROUTE_DELETE_ITEMS).handler(this::deleteItem);

        /* list the item from database using itemId */
        router.get(Constants.ROUTE_LIST_ITEMS).handler(this::listItems);

        /* Get all resources belonging to a resource group */
        router.getWithRegex(Constants.ROUTE_LIST_RESOURCE_REL)
            .handler(this::listResourceRelationship);

        /* Get resource group of an item belonging to a resource */
        router.getWithRegex(Constants.ROUTE_LIST_RESOURCE_GROUP_REL)
            .handler(this::listResourceGroupRelationship);

        /* Gets the cities configuration from the database */
        router.get(Constants.ROUTE_UI_CITIES).handler(this::getCities);

        /* Create the cities configuration from the database */
        router.post(Constants.ROUTE_UI_CITIES).handler(this::setCities);

        /* Updates the cities configuration from the database */
        router.put(Constants.ROUTE_UI_CITIES).handler(this::updateCities);

        /* Get all the configuration */
        router.get(Constants.ROUTE_UI_CONFIG).handler(this::getConfig);

        /* Creates the configuration */
        router.post(Constants.ROUTE_UI_CONFIG).handler(this::setConfig);

        /* Deletes the configuration */
        router.delete(Constants.ROUTE_UI_CONFIG).handler(this::deleteConfig);

        /* Updates the existing configuration */
        router.put(Constants.ROUTE_UI_CONFIG).handler(this::updateConfig);

        /* Patches the existing configuration */
        router.patch(Constants.ROUTE_UI_CONFIG).handler(this::appendConfig);

        /* Get provider relationship to an item */
        router.getWithRegex(Constants.ROUTE_PROVIDER_REL).handler(this::listProviderRelationship);

        /* Get resource server relationship to an item */
        router.getWithRegex(Constants.ROUTE_RESOURCE_SERVER_REL)
            .handler(this::listResourceServerRelationship);

        /* Get list types with the database for an item */
        router.getWithRegex(Constants.ROUTE_DATA_TYPE).handler(this::listTypes);

        /* Count the Cataloque server items */
        router.get(Constants.ROUTE_COUNT).handler(this::count);

        /* Populating itemTypes */
        itemTypes = new ArrayList<String>();
        itemTypes.add(Constants.ITEM_TYPE_RESOURCE);
        itemTypes.add(Constants.ITEM_TYPE_RESOURCE_GROUP);
        itemTypes.add(Constants.ITEM_TYPE_RESOURCE_SERVER);
        itemTypes.add(Constants.ITEM_TYPE_PROVIDER);

        /* Populating geo spatials relations */
        geoRels = new ArrayList<String>();
        geoRels.add(Constants.GEOREL_WITHIN);
        geoRels.add(Constants.GEOREL_NEAR);
        geoRels.add(Constants.GEOREL_COVERED_BY);
        geoRels.add(Constants.GEOREL_INTERSECTS);
        geoRels.add(Constants.GEOREL_EQUALS);
        geoRels.add(Constants.GEOREL_DISJOINT);

        geometries = new ArrayList<String>();
        geometries.add(Constants.POINT);
        geometries.add(Constants.POLYGON);
        geometries.add(Constants.BBOX);
        geometries.add(Constants.LINE_STRING);

        /* Read the configuration and set the HTTPs server properties. */

        try {

          inputstream = new FileInputStream(Constants.CONFIG_FILE);
          properties.load(inputstream);

          keystore = properties.getProperty(Constants.KEYSTORE_FILE_NAME);
          keystorePassword = properties.getProperty(Constants.KEYSTORE_FILE_PASSWORD);

        } catch (Exception ex) {
          logger.info(ex.toString());
        }

        server = vertx.createHttpServer(new HttpServerOptions().setSsl(true)
            .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword)));

        server.requestHandler(router).listen(Constants.PORT);

        /* Get a handler for the Service Discovery interface. */

        discovery = ServiceDiscovery.create(vertx);

        /* Get a handler for the DatabaseService from Service Discovery interface. */

        EventBusService.getProxy(discovery, DatabaseService.class,
            databaseServiceDiscoveryHandler -> {
              if (databaseServiceDiscoveryHandler.succeeded()) {
                database = databaseServiceDiscoveryHandler.result();
                logger.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + database.getClass().getName() + " +++++++ ");
              } else {
                logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });
        /* Get a handler for the OnboarderService from Service Discovery interface. */

        EventBusService.getProxy(discovery, OnboarderService.class,
            onboarderServiceDiscoveryHandler -> {
              if (onboarderServiceDiscoveryHandler.succeeded()) {
                onboarder = onboarderServiceDiscoveryHandler.result();
                logger.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + onboarder.getClass().getName() + " +++++++ ");
              } else {
                logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });
        /* Get a handler for the ValidatorService from Service Discovery interface. */

        EventBusService.getProxy(discovery, ValidatorService.class,
            validatorServiceDiscoveryHandler -> {
              if (validatorServiceDiscoveryHandler.succeeded()) {
                validator = validatorServiceDiscoveryHandler.result();
                logger.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + validator.getClass().getName() + " +++++++ ");
              } else {
                logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });
        /*
         * Get a handler for the AuthenticationService from Service Discovery interface.
         */

        EventBusService.getProxy(discovery, AuthenticationService.class,
            authenticatorServiceDiscoveryHandler -> {
              if (authenticatorServiceDiscoveryHandler.succeeded()) {
                authenticator = authenticatorServiceDiscoveryHandler.result();
                logger.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + authenticator.getClass().getName() + " +++++++ ");
              } else {
                logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });
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
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* Collection of query parameters from HTTP request */
    MultiMap queryParameters = routingContext.queryParams();

    logger.info("routed to search");
    logger.info(request.params().toString());

    /* validating proper actual query parameters from request */
    if ((request.getParam(Constants.PROPERTY) == null || request.getParam(Constants.VALUE) == null)
        && (request.getParam(Constants.GEOPROPERTY) == null
            || request.getParam(Constants.GEOREL) == null
            || request.getParam(Constants.GEOMETRY) == null
            || request.getParam(Constants.COORDINATES) == null)
        && request
            .getParam(Constants.Q_VALUE) == null /*
                                                  * || request.getParam(Constants.LIMIT) == null ||
                                                  * request.getParam(Constants.OFFSET) == null
                                                  */) {

      logger.error("Invalid Syntax");
      response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(Constants.INVALID_SYNTAX)
              .build().toJsonString());
      return;

      /* checking the values of the query parameters */
    } else if (request.getParam(Constants.PROPERTY) != null
        && !request.getParam(Constants.PROPERTY).isBlank()) {

      /* converting query parameters in json */
      requestBody = QueryMapper.map2Json(queryParameters);

      /* checking the values of the query parameters for geo related count */
    } else if (Constants.LOCATION.equals(request.getParam(Constants.GEOPROPERTY))
        && geometries.contains(request.getParam(Constants.GEOMETRY))
        && geoRels.contains(request.getParam(Constants.GEOREL))) {

      requestBody = QueryMapper.map2Json(queryParameters);

      /* checking the values of the query parameters */
    } else if (request.getParam(Constants.Q_VALUE) != null
        && !request.getParam(Constants.Q_VALUE).isBlank()) {

      requestBody = QueryMapper.map2Json(queryParameters);

    } else {
      response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(Constants.INVALID_VALUE)
              .build().toJsonString());
      return;
    }

    if (requestBody != null) {
      requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);
      database.searchQuery(requestBody, handler -> {
        if (handler.succeeded()) {
          JsonObject resultJson = handler.result();
          String status = resultJson.getString(Constants.STATUS);
          if (status.equalsIgnoreCase(Constants.SUCCESS)) {
            response.setStatusCode(200);
          } else if (status.equalsIgnoreCase(Constants.PARTIAL_CONTENT)) {
            response.setStatusCode(206);
          } else {
            response.setStatusCode(400);
          }
          response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
              .add(Constants.HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
          response.write(resultJson.toString());
          logger.info("response: " + resultJson);
          response.end();
        } else if (handler.failed()) {
          logger.error(handler.cause().getMessage());
          response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
              .setStatusCode(400).end(handler.cause().getMessage());
        }
      });
    } else {
      logger.error("Invalid request query parameters");
      response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(Constants.INVALID_VALUE)
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
    queryJson.put(Constants.INSTANCE_ID_KEY, instanceID).put(Constants.OPERATION,
        Constants.GET_CITIES);
    logger.info("search query : " + queryJson);
    database.getCities(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(Constants.STATUS);
        if (status.equalsIgnoreCase(Constants.SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .add(Constants.HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        logger.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        logger.info(handler.cause().getMessage());
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
        response.setStatusCode(500);
        response.end(Constants.INTERNAL_SERVER_ERROR);
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
    validator.validateItem(queryJson, validationHandler -> {
      if (validationHandler.succeeded()) {
        queryJson.put(Constants.INSTANCE_ID_KEY, instanceID);
        logger.info("search query : " + queryJson);
        database.setCities(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString(Constants.STATUS);
            if (status.equalsIgnoreCase(Constants.SUCCESS)) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                .add(Constants.HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            logger.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            dbHandler.cause().getMessage();
            response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
            response.setStatusCode(500);
            response.end(Constants.INTERNAL_SERVER_ERROR);
          }
        });
      } else if (validationHandler.failed()) {
        logger.info(validationHandler.cause().getMessage());
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
        response.setStatusCode(400);
        response.end(Constants.BAD_REQUEST);
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
    validator.validateItem(queryJson, validationHandler -> {
      if (validationHandler.succeeded()) {
        queryJson.put(Constants.INSTANCE_ID_KEY, instanceID);
        logger.info("search query : " + queryJson);
        database.updateCities(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString(Constants.STATUS);
            if (status.equalsIgnoreCase(Constants.SUCCESS)) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                .add(Constants.HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            logger.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            logger.info(dbHandler.cause().getMessage());
            response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
            response.setStatusCode(500);
            response.end(Constants.INTERNAL_SERVER_ERROR);
          }
        });
      } else if (validationHandler.failed()) {
        logger.info(validationHandler.cause().getMessage());
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
        response.setStatusCode(400);
        response.end(Constants.BAD_REQUEST);
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
    queryJson.put(Constants.INSTANCE_ID_KEY, instanceID).put("operation", "getConfig");
    logger.info("search query : " + queryJson);
    database.getConfig(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(Constants.STATUS);
        if (status.equalsIgnoreCase(Constants.SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .add(Constants.HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        logger.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        logger.info(handler.cause().getMessage());
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
        response.setStatusCode(500);
        response.end(Constants.INTERNAL_SERVER_ERROR);
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
    validator.validateItem(queryJson, validationHandler -> {
      if (validationHandler.succeeded()) {
        queryJson.put(Constants.INSTANCE_ID_KEY, instanceID);
        logger.info("search query : " + queryJson);
        database.setConfig(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString(Constants.STATUS);
            if (status.equalsIgnoreCase(Constants.SUCCESS)) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                .add(Constants.HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            logger.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            logger.error(dbHandler.cause().getMessage());
            response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
            response.setStatusCode(500);
            response.end(Constants.INTERNAL_SERVER_ERROR);
          }
        });
      } else if (validationHandler.failed()) {
        logger.error(validationHandler.cause().getMessage());
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
        response.setStatusCode(400);
        response.end(Constants.BAD_REQUEST);
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
    queryJson.put(Constants.INSTANCE_ID_KEY, instanceID);
    logger.info("search query : " + queryJson);
    database.deleteConfig(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(Constants.STATUS);
        if (status.equalsIgnoreCase(Constants.SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .add(Constants.HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        logger.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        logger.error(handler.cause().getMessage());
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
        response.setStatusCode(500);
        response.end(Constants.INTERNAL_SERVER_ERROR);
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
    validator.validateItem(queryJson, validationHandler -> {
      if (validationHandler.succeeded()) {
        queryJson.put(Constants.INSTANCE_ID_KEY, instanceID);
        logger.info("search query : " + queryJson);
        database.updateConfig(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString(Constants.STATUS);
            if (status.equalsIgnoreCase(Constants.SUCCESS)) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                .add(Constants.HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            logger.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            logger.error(dbHandler.cause().getMessage());
            response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
            response.setStatusCode(500);
            response.end(Constants.INTERNAL_SERVER_ERROR);
          }
        });
      } else if (validationHandler.failed()) {
        logger.error(validationHandler.cause().getMessage());
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
        response.setStatusCode(400);
        response.end(Constants.BAD_REQUEST);
      }
    });
  }

  /**
   * Creates a new item in database.
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  private void createItem(RoutingContext routingContext) {

    logger.info("Creating an item");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    /* JsonObject of authentication related information */
    JsonObject authenticationInfo = new JsonObject();

    /* HTTP request body as Json */
    JsonObject requestBody = routingContext.getBodyAsJson();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* checking and comparing itemType from the request body */
    if (requestBody.containsKey(Constants.ITEM_TYPE)
        && itemTypes.contains(requestBody.getString(Constants.ITEM_TYPE))) {
      /* Populating query mapper */
      requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);

      /* checking auhthentication info in requests */
      if (request.headers().contains(Constants.HEADER_TOKEN)) {
        authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));

        /* Authenticating the request */
        authenticator.tokenInterospect(requestBody, authenticationInfo, authhandler -> {
          if (authhandler.succeeded()) {
            logger.info(
                "Authenticating item creation request ".concat(authhandler.result().toString()));
            /* Validating the request */
            validator.validateItem(requestBody, valhandler -> {
              if (valhandler.succeeded()) {
                logger.info("Item creation validated".concat(authhandler.result().toString()));
                /* Requesting database service, creating a item */
                database.createItem(requestBody, dbhandler -> {
                  if (dbhandler.succeeded()) {
                    logger.info("Item created".concat(dbhandler.result().toString()));
                    response
                        .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                        .setStatusCode(201).end(dbhandler.result().toString());
                  } else if (dbhandler.failed()) {
                    logger.error("Item creation failed".concat(dbhandler.cause().toString()));
                    response
                        .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                        .setStatusCode(500).end(dbhandler.cause().toString());
                  }
                });
              } else if (valhandler.failed()) {
                logger.error("Item validation failed".concat(valhandler.cause().toString()));
                response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                    .setStatusCode(500).end(valhandler.cause().toString());
              }
            });
          } else if (authhandler.failed()) {
            logger.error("Unathorized request".concat(authhandler.cause().toString()));
            response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                .setStatusCode(401).end(authhandler.cause().toString());
          }
        });
      } else {
        logger.error("InvalidHeader, 'token' header");
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .setStatusCode(400).end(new ResponseHandler.Builder()
                .withStatus(Constants.INVALID_HEADER).build().toJsonString());
      }
    } else {
      logger.error("InvalidValue, 'itemType' attribute is missing or is empty");
      response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(Constants.INVALID_VALUE)
              .build().toJsonString());
    }
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
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* Building complete itemID from HTTP request path parameters */
    String itemId = routingContext.pathParam(Constants.RESOURCE_ITEM).concat("/")
        .concat(routingContext.pathParam(Constants.RESOURCE_GRP_ITEM).concat("/")
            .concat(routingContext.pathParam(Constants.RESOURCE_SVR_ITEM)).concat("/")
            .concat(routingContext.pathParam(Constants.PROVIDER_ITEM).concat("/"))
            .concat(routingContext.pathParam(Constants.DATA_DES_ITEM)));

    logger.info("Updating an item, Id: ".concat(itemId));

    /* checking and comparing itemType from the request body */
    if (requestBody.containsKey(Constants.ITEM_TYPE)
        && itemTypes.contains(requestBody.getString(Constants.ITEM_TYPE))) {
      /* Populating query mapper */
      requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);

      /* checking auhthentication info in requests */
      if (request.headers().contains(Constants.HEADER_TOKEN)) {
        authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));

        /* Authenticating the request */
        authenticator.tokenInterospect(requestBody, authenticationInfo, authhandler -> {
          if (authhandler.succeeded()) {
            logger.info(
                "Authenticating item update request ".concat(authhandler.result().toString()));
            /* Validating the request */
            validator.validateItem(requestBody, valhandler -> {
              if (valhandler.succeeded()) {
                logger.info("Item update validated ".concat(authhandler.result().toString()));
                /* Requesting database service, creating a item */
                database.updateItem(requestBody, dbhandler -> {
                  if (dbhandler.succeeded()) {
                    logger.info("Item updated ".concat(dbhandler.result().toString()));
                    response
                        .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                        .setStatusCode(200).end(dbhandler.result().toString());
                  } else if (dbhandler.failed()) {
                    logger.error("Item update failed ".concat(dbhandler.cause().toString()));
                    response
                        .putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                        .setStatusCode(500).end(dbhandler.cause().toString());
                  }
                });
              } else if (valhandler.failed()) {
                logger.error("Item validation failed ".concat(valhandler.cause().toString()));
                response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                    .setStatusCode(500).end(valhandler.cause().toString());
              }
            });
          } else if (authhandler.failed()) {
            logger.error("Unathorized request ".concat(authhandler.cause().toString()));
            response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                .setStatusCode(401).end(authhandler.cause().toString());
          }
        });
      } else {
        logger.error("InvalidHeader 'token' header");
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .setStatusCode(400).end(new ResponseHandler.Builder()
                .withStatus(Constants.INVALID_HEADER).build().toJsonString());
      }
    } else {
      logger.error("InvalidValue, 'itemType' attribute is missing or is empty");
      response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(Constants.INVALID_VALUE)
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
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* Building complete itemID from HTTP request path parameters */
    String itemId = routingContext.pathParam(Constants.RESOURCE_ITEM).concat("/")
        .concat(routingContext.pathParam(Constants.RESOURCE_GRP_ITEM).concat("/")
            .concat(routingContext.pathParam(Constants.RESOURCE_SVR_ITEM)).concat("/")
            .concat(routingContext.pathParam(Constants.PROVIDER_ITEM).concat("/"))
            .concat(routingContext.pathParam(Constants.DATA_DES_ITEM)));

    /* Populating query mapper */
    requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);
    requestBody.put(Constants.ID, itemId);
    // requestBody.put(Constants.ITEM_TYPE, "Resource/ResourceGroup");

    logger.info("Deleting an item, Id: ".concat(itemId));

    /* checking auhthentication info in requests */
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));

      /* Authenticating the request */
      authenticator.tokenInterospect(null, authenticationInfo, authhandler -> {
        if (authhandler.succeeded()) {
          logger.info("Authenticating item delete request".concat(authhandler.result().toString()));
          /* Requesting database service, creating a item */
          database.deleteItem(requestBody, dbhandler -> {
            if (dbhandler.succeeded()) {
              logger.info("Item deleted".concat(dbhandler.result().toString()));
              response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                  .setStatusCode(200).end(dbhandler.result().toString());
            } else if (dbhandler.failed()) {
              logger.error("Item deletion failed".concat(dbhandler.cause().toString()));
              response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                  .setStatusCode(400).end(dbhandler.cause().toString());
            }
          });
        } else if (authhandler.failed()) {
          logger.error("Unathorized request".concat(authhandler.cause().toString()));
          response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
              .setStatusCode(401).end(authhandler.cause().toString());
        }
      });
    } else {
      logger.error("Invalid 'token' header");
      response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(Constants.INVALID_HEADER)
              .build().toJsonString());
    }
  }

  /**
   * List the items from database using itemId.
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listItems(RoutingContext routingContext) {

    logger.info("Listing items from database");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* Building complete itemID from HTTP request path parameters */
    String itemId = routingContext.pathParam(Constants.RESOURCE_ITEM).concat("/")
        .concat(routingContext.pathParam(Constants.RESOURCE_GRP_ITEM).concat("/")
            .concat(routingContext.pathParam(Constants.RESOURCE_SVR_ITEM)).concat("/")
            .concat(routingContext.pathParam(Constants.PROVIDER_ITEM).concat("/"))
            .concat(routingContext.pathParam(Constants.DATA_DES_ITEM)));

    /* Populating query mapper */
    requestBody.put(Constants.ID, itemId);
    requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);

    /* Databse service call for listing item */
    database.listItem(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of items ".concat(dbhandler.result().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing items ".concat(dbhandler.cause().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
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

    logger.info("Listing tags of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing tags */
    database.listTags(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of tags ".concat(dbhandler.result().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing tags ".concat(dbhandler.cause().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
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

    logger.info("Listing domains of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing domains */
    database.listDomains(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of domains ".concat(dbhandler.result().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing domains ".concat(dbhandler.cause().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
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

    logger.info("Listing cities of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing cities */
    database.listCities(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of cities ".concat(dbhandler.result().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing cities ".concat(dbhandler.cause().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
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

    logger.info("Listing resource servers of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing resource servers */
    database.listResourceServers(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of resource servers ".concat(dbhandler.result().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing resource servers ".concat(dbhandler.cause().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
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

    logger.info("Listing providers of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing providers */
    database.listProviders(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of providers ".concat(dbhandler.result().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing providers ".concat(dbhandler.cause().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
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

    logger.info("Listing resource groups of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing resource groups */
    database.listResourceGroups(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of resource groups ".concat(dbhandler.result().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing resource groups ".concat(dbhandler.cause().toString()));
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
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

    logger.info("Searching for relationship of resource");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* Parsing id from HTTP request */
    String id = request.getParam(Constants.ID);

    /* Checking if id is either not null nor empty */
    if (id != null && !id.isBlank()) {

      /* Populating query mapper */
      requestBody.put(Constants.ID, id);
      requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);
      requestBody.put(Constants.RELATIONSHIP, Constants.REL_RESOURCE);

      /*
       * Request database service with requestBody for listing resource relationship
       */
      database.listResourceRelationship(requestBody, dbhandler -> {
        if (dbhandler.succeeded()) {
          logger.info("List of resources belonging to resourceGroups "
              .concat(dbhandler.result().toString()));
          response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
              .setStatusCode(200).end(dbhandler.result().toString());
        } else if (dbhandler.failed()) {
          logger.error(
              "Issue in listing resource relationship ".concat(dbhandler.cause().toString()));
          response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
              .setStatusCode(400).end(dbhandler.cause().toString());
        }
      });
    } else {
      logger.error("Issue in path parameter");
      response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(Constants.INVALID_SYNTAX)
              .build().toJsonString());
    }
  }

  /**
   * Get all resourceGroup relationships.
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  public void listResourceGroupRelationship(RoutingContext routingContext) {

    logger.info("Searching for relationship of resource and resourceGroup");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* Parsing id from HTTP request */
    String id = request.getParam(Constants.ID);

    /* Checking if id is either not null nor empty */
    if (id != null && !id.isBlank()) {

      /* Populating query mapper */
      requestBody.put(Constants.ID, id);
      requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);
      requestBody.put(Constants.RELATIONSHIP, Constants.REL_RESOURCE_GRP);

      /*
       * Request database service with requestBody for listing resource group relationship
       */
      database.listResourceGroupRelationship(requestBody, dbhandler -> {
        if (dbhandler.succeeded()) {
          logger.info(
              "List of resourceGroup belonging to resource ".concat(dbhandler.result().toString()));
          response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
              .setStatusCode(200).end(dbhandler.result().toString());
        } else if (dbhandler.failed()) {
          logger.error(
              "Issue in listing resourceGroup relationship ".concat(dbhandler.cause().toString()));
          response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
              .setStatusCode(400).end(dbhandler.cause().toString());
        }
      });
    } else {
      logger.error("Issue in path parameter");
      response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(Constants.INVALID_SYNTAX)
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
    validator.validateItem(queryJson, validationHandler -> {
      if (validationHandler.succeeded()) {
        queryJson.put(Constants.INSTANCE_ID_KEY, instanceID);
        logger.info("search query : " + queryJson);
        database.appendConfig(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString(Constants.STATUS);
            if (status.equalsIgnoreCase(Constants.SUCCESS)) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                .add(Constants.HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            logger.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            logger.error(dbHandler.cause().getMessage());
            response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
            response.setStatusCode(500);
            response.end(Constants.INTERNAL_SERVER_ERROR);
          }
        });
      } else if (validationHandler.failed()) {
        logger.error(validationHandler.cause().getMessage());
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
        response.setStatusCode(400);
        response.end(Constants.BAD_REQUEST);
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
    String id = routingContext.request().getParam(Constants.ID);
    queryJson.put(Constants.INSTANCE_ID_KEY, instanceID).put(Constants.ID, id)
        .put(Constants.RELATIONSHIP, Constants.REL_RESOURCE_SVR);
    logger.info("search query : " + queryJson);
    database.listResourceServerRelationship(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(Constants.STATUS);
        if (status.equalsIgnoreCase(Constants.SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .add(Constants.HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        logger.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        logger.error(handler.cause().getMessage());
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
        response.setStatusCode(500);
        response.end(Constants.INTERNAL_SERVER_ERROR);
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
    String id = routingContext.request().getParam(Constants.ID);
    queryJson.put(Constants.INSTANCE_ID_KEY, instanceID).put(Constants.ID, id)
        .put(Constants.RELATIONSHIP, Constants.REL_PROVIDER);
    logger.info("search query : " + queryJson);
    database.listProviderRelationship(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(Constants.STATUS);
        if (status.equalsIgnoreCase(Constants.SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .add(Constants.HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        logger.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        logger.error(handler.cause().getMessage());
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
        response.setStatusCode(500);
        response.end(Constants.INTERNAL_SERVER_ERROR);
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
    String id = routingContext.request().getParam(Constants.ID);
    queryJson.put(Constants.INSTANCE_ID_KEY, instanceID).put(Constants.ID, id)
        .put(Constants.RELATIONSHIP, Constants.REL_TYPE);
    logger.info("search query : " + queryJson);
    database.listTypes(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(Constants.STATUS);
        if (status.equalsIgnoreCase(Constants.SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .add(Constants.HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        logger.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        logger.error(handler.cause().getMessage());
        response.headers().add(Constants.HEADER_CONTENT_TYPE, Constants.TEXT);
        response.setStatusCode(500);
        response.end(Constants.INTERNAL_SERVER_ERROR);
      }
    });
  }

  /**
   * Counting the cataloque items.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void count(RoutingContext routingContext) {

    logger.info("Counting the request parameters");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);

    /* Collection of query parameters from HTTP request */
    MultiMap queryParameters = routingContext.queryParams();

    if (!queryParameters.contains(Constants.ATTRIBUTE_FILTER)) {

      /* validating proper actual query parameters from request */
      if ((request.getParam(Constants.PROPERTY) == null
          || request.getParam(Constants.VALUE) == null)
          && (request.getParam(Constants.GEOPROPERTY) == null
              || request.getParam(Constants.GEOREL) == null
              || request.getParam(Constants.GEOMETRY) == null
              || request.getParam(Constants.COORDINATES) == null)
          && (request.getParam(Constants.Q_VALUE) == null
              || request.getParam(Constants.LIMIT) == null
              || request.getParam(Constants.OFFSET) == null)) {

        logger.error("Invalid Syntax");
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .setStatusCode(400).end(new ResponseHandler.Builder()
                .withStatus(Constants.INVALID_SYNTAX).build().toJsonString());
        return;

        /* checking the values of the query parameters */
      } else if (request.getParam(Constants.PROPERTY) != null
          && !request.getParam(Constants.PROPERTY).isBlank()) {

        /* converting query parameters in json */
        requestBody = QueryMapper.map2Json(queryParameters);

        /* checking the values of the query parameters for geo related count */
      } else if (Constants.LOCATION.equals(request.getParam(Constants.GEOPROPERTY))
          && geometries.contains(request.getParam(Constants.GEOMETRY))
          && geoRels.contains(request.getParam(Constants.GEOREL))) {

        requestBody = QueryMapper.map2Json(queryParameters);

        /* checking the values of the query parameters */
      } else if (request.getParam(Constants.Q_VALUE) != null
          && !request.getParam(Constants.Q_VALUE).isBlank()) {

        requestBody = QueryMapper.map2Json(queryParameters);

      } else {
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .setStatusCode(400).end(new ResponseHandler.Builder()
                .withStatus(Constants.INVALID_VALUE).build().toJsonString());
        return;
      }

      if (requestBody != null) {
        logger.info("Count query : " + requestBody);
        requestBody.put(Constants.INSTANCE_ID_KEY, instanceID);

        /* Request database service with requestBody for counting */
        database.countQuery(requestBody, dbhandler -> {
          if (dbhandler.succeeded()) {
            logger.info("Count query completed ".concat(dbhandler.result().toString()));
            response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                .setStatusCode(200).end(dbhandler.result().toString());
          } else if (dbhandler.failed()) {
            logger.error("Issue in count query ".concat(dbhandler.cause().toString()));
            response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
                .setStatusCode(400).end(dbhandler.cause().toString());
          }
        });
      } else {
        logger.error("Invalid request query parameters");
        response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
            .setStatusCode(400).end(new ResponseHandler.Builder()
                .withStatus(Constants.INVALID_VALUE).build().toJsonString());
      }
    } else {
      logger.error("Invalid request query parameters");
      response.putHeader(Constants.HEADER_CONTENT_TYPE, Constants.MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(Constants.INVALID_VALUE)
              .build().toJsonString());
    }
  }
}
