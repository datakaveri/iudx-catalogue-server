package iudx.catalogue.server.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
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
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.onboarder.OnboarderService;
import iudx.catalogue.server.validator.ValidatorService;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The Catalogue Server API Verticle.
 *
 * <h1>Catalogue Server API Verticle</h1>
 *
 * <p>
 * The API Server verticle implements the IUDX Catalogue Server APIs. It handles
 * the API requests from the clients and interacts with the associated Service
 * to respond.
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
  private final int port = 8443;
  private String keystore;
  private String keystorePassword;
  private String basePath = "/iudx/cat/v1";
  private ArrayList<String> itemTypes;
  private ArrayList<String> geoRels;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a
   * cluster, reads the configuration, obtains a proxy for the Event bus services
   * exposed through service discovery, start an HTTPs server at port 8443.
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
        allowedHeaders.add("Accept");
        allowedHeaders.add("token");
        allowedHeaders.add("Content-Length");
        allowedHeaders.add("Content-Type");
        allowedHeaders.add("Host");
        allowedHeaders.add("Origin");
        allowedHeaders.add("Referer");
        allowedHeaders.add("Access-Control-Allow-Origin");

        /* Define the APIs, methods, endpoints and associated methods. */

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders));

        router.route("/apis/*").handler(StaticHandler.create());

        /* New item create */
        router.post(basePath.concat("/item")).handler(this::createItem);

        /* Search for an item */
        router.get(basePath.concat("/search")).handler(this::search);

        /* list all the tags */
        router.get(basePath.concat("/tags")).handler(this::listTags);

        /* list all the domains */
        router.get(basePath.concat("/domains")).handler(this::listDomains);

        /* list all the cities associated with the cataloque instance */
        router.get(basePath.concat("/cities")).handler(this::listCities);

        /* list all the resource server associated with the cataloque instance */
        router.get(basePath.concat("/resourceservers")).handler(this::listResourceServers);

        /* list all the providers associated with the cataloque instance */
        router.get(basePath.concat("/providers")).handler(this::listProviders);

        /* list all the resource groups associated with the cataloque instance */
        router.get(basePath.concat("/resourcegroups")).handler(this::listResourceGroups);

        /*
         * Update an item in the database using itemId [itemId=ResourceItem,
         * ResourceGroupItem, ResourceServerItem, ProviderItem, DataDescriptorItem]
         */
        router.put(basePath.concat("/item/:resItem/:resGrpItem/:resSvrItem/:pvdrItem/:dataDesItem"))
            .handler(this::updateItem);

        /* Delete an item from database using itemId */
        router.delete(basePath.concat("/item/:resItem/:resGrpItem/:resSvrItem/:pvdrItem/:dataDesItem"))
            .handler(this::deleteItem);

        /* list the item from database using itemId */
        router.get(basePath.concat("/items/:resItem/:resGrpItem/:resSvrItem/:pvdrItem/:dataDesItem"))
            .handler(this::listItems);

        /* Get all resources belonging to a resource group */
        router.getWithRegex(basePath.concat("\\/(?<id>.*)\\/resource")).handler(this::listResourceRelationship);

        /* Get resource group of an item belonging to a resource */
        router.getWithRegex(basePath.concat("\\/(?<id>.*)\\/resourceGroup"))
            .handler(this::listResourceGroupRelationship);

        router.get(basePath.concat("/ui/cities")).handler(this::getCities);
        router.post(basePath.concat("/ui/cities")).handler(this::setCities);
        router.put(basePath.concat("/ui/cities")).handler(this::updateCities);
        router.get(basePath.concat("/ui/config")).handler(this::getConfig);
        router.post(basePath.concat("/ui/config")).handler(this::setConfig);
        router.delete(basePath.concat("/ui/config")).handler(this::deleteConfig);
        router.put(basePath.concat("/ui/config")).handler(this::updateConfig);
        router.patch(basePath.concat("/ui/config")).handler(this::appendConfig);
        router.getWithRegex(basePath.concat("\\/(?<id>.*)\\/provider")).handler(this::getProvider);
        router.getWithRegex(basePath.concat("\\/(?<id>.*)\\/resourceServer")).handler(this::getResourceServer);
        router.getWithRegex(basePath.concat("\\/(?<id>.*)\\/type")).handler(this::getDataModel);

        /* Populating itemTypes */
        itemTypes = new ArrayList<String>();
        itemTypes.add("Resource");
        itemTypes.add("ResourceGroup");
        itemTypes.add("ResourceServer");
        itemTypes.add("Provider");

        /* Populating geo spatials relations */
        geoRels = new ArrayList<String>();
        geoRels.add("within");
        geoRels.add("near");
        geoRels.add("coveredBy");
        geoRels.add("intersects");
        geoRels.add("equals");
        geoRels.add("disjoint");

        /* Read the configuration and set the HTTPs server properties. */

        try {

          inputstream = new FileInputStream("config.properties");
          properties.load(inputstream);

          keystore = properties.getProperty("keystore");
          keystorePassword = properties.getProperty("keystorePassword");

        } catch (Exception ex) {
          logger.info(ex.toString());
        }

        server = vertx.createHttpServer(new HttpServerOptions().setSsl(true)
            .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword)));

        server.requestHandler(router).listen(port);

        /* Get a handler for the Service Discovery interface. */

        discovery = ServiceDiscovery.create(vertx);

        /* Get a handler for the DatabaseService from Service Discovery interface. */

        EventBusService.getProxy(discovery, DatabaseService.class, databaseServiceDiscoveryHandler -> {
          if (databaseServiceDiscoveryHandler.succeeded()) {
            database = databaseServiceDiscoveryHandler.result();
            logger.info("\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                + database.getClass().getName() + " +++++++ ");
          } else {
            logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
          }
        });
        /* Get a handler for the OnboarderService from Service Discovery interface. */

        EventBusService.getProxy(discovery, OnboarderService.class, onboarderServiceDiscoveryHandler -> {
          if (onboarderServiceDiscoveryHandler.succeeded()) {
            onboarder = onboarderServiceDiscoveryHandler.result();
            logger.info("\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                + onboarder.getClass().getName() + " +++++++ ");
          } else {
            logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
          }
        });
        /* Get a handler for the ValidatorService from Service Discovery interface. */

        EventBusService.getProxy(discovery, ValidatorService.class, validatorServiceDiscoveryHandler -> {
          if (validatorServiceDiscoveryHandler.succeeded()) {
            validator = validatorServiceDiscoveryHandler.result();
            logger.info("\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                + validator.getClass().getName() + " +++++++ ");
          } else {
            logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
          }
        });
        /*
         * Get a handler for the AuthenticationService from Service Discovery interface.
         */

        EventBusService.getProxy(discovery, AuthenticationService.class, authenticatorServiceDiscoveryHandler -> {
          if (authenticatorServiceDiscoveryHandler.succeeded()) {
            authenticator = authenticatorServiceDiscoveryHandler.result();
            logger.info("\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                + authenticator.getClass().getName() + " +++++++ ");
          } else {
            logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
          }
        });
      }
    });
  }

  /**
   * Processes the attribute, geoSpatial, and text search requests and returns the
   * results from the database.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void search(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    logger.info("routed to search");
    logger.info(request.params().toString());
    if ((request.getParam("property") == null || request.getParam("value") == null)
        && (request.getParam("geoproperty") == null || request.getParam("georel") == null
            || request.getParam("geometry") == null || request.getParam("coordinates") == null)
        && (request.getParam("q") == null || request.getParam("limit") == null || request.getParam("offset") == null)) {
      logger.error("Invalid Syntax");
      JsonObject json = new JsonObject();
      json.put("status", "invalidSyntax").put("results", new JsonArray());
      response.headers().add("content-type", "application/json").add("content-length",
          String.valueOf(json.toString().length()));
      response.setStatusCode(400);
      response.write(json.toString());
      response.end();
      return;
    }
    MultiMap params = request.params();
    /* Pattern to validate text search string */
    Pattern textPattern = Pattern.compile("^[\\*]{0,1}[A-Za-z ]+[\\*]{0,1}");
    JsonObject queryJson = new JsonObject();
    String instanceID = request.host();
    if (request.getParam("property") != null && request.getParam("property").toLowerCase().contains("provider.name")) {
      queryJson.put("instanceID", instanceID);
    } else if ((request.getParam("geoproperty") != null) && (request.getParam("geometry") != null)
        && (request.getParam("georel") != null)
        && (!request.getParam("geoproperty").equals("location")
            || (!request.getParam("geometry").equals("bbox") && !request.getParam("geometry").equals("Point")
                && !request.getParam("geometry").equals("LineString")
                && !request.getParam("geometry").equals("Polygon"))
            || (!request.getParam("georel").equals("within") && !request.getParam("georel").equals("near")
                && !request.getParam("georel").equals("coveredBy") && !request.getParam("georel").equals("intersects")
                && !request.getParam("georel").equals("equals") && !request.getParam("georel").equals("disjoint")))) {
      logger.error("invalid geo spatial search parameter value");
      JsonObject json = new JsonObject();
      json.put("status", "invalidValue").put("results", new JsonArray());
      response.headers().add("content-type", "application/json").add("content-length",
          String.valueOf(json.toString().length()));
      response.setStatusCode(400);
      response.write(json.toString());
      response.end();
      return;
    } else if (request.getParam("q") != null
        && !textPattern.matcher(request.getParam("q").replaceAll("\"", "")).matches()) {
      logger.error("invalid text search string");
      JsonObject json = new JsonObject();
      json.put("status", "invalidValue").put("results", new JsonArray());
      response.headers().add("content-type", "application/json").add("content-length",
          String.valueOf(json.toString().length()));
      response.setStatusCode(400);
      response.write(json.toString());
      response.end();
      return;
    }
    /* Pattern to match array passed in query parameter string */
    Pattern arrayPattern = Pattern.compile("^\\[.*\\]$");
    outerloop: for (String str : params.names()) {
      if (arrayPattern.matcher(params.get(str)).matches()) {
        JsonArray value = new JsonArray();
        String[] split = params.get(str).split("\\],");
        for (String s : split) {
          JsonArray json = new JsonArray();
          String[] paramValues = s.split(",");
          for (String val : paramValues) {
            if (str.equalsIgnoreCase("coordinates")) {
              try {
                double coordinate = Double
                    .parseDouble(val.strip().replaceAll("\"", "").replaceAll("\\[", "").replaceAll("\\]", ""));
                json.add(coordinate);
              } catch (NumberFormatException e) {
                logger.error("invalid coordinate value");
                JsonObject invalidValue = new JsonObject();
                invalidValue.put("status", "invalidValue").put("results", new JsonArray());
                response.headers().add("content-type", "application/json").add("content-length",
                    String.valueOf(invalidValue.toString().length()));
                response.setStatusCode(400);
                response.write(invalidValue.toString());
                response.end();
                return;
              }
            } else {
              json.add(val.strip().replaceAll("\"", "").replaceAll("\\[", "").replaceAll("\\]", ""));
            }
          }
          if (split.length > 1 || str.equalsIgnoreCase("value")) {
            value.add(json);
          } else {
            queryJson.put(str, json);
            continue outerloop;
          }
        }
        queryJson.put(str, value);
      } else if (str.equalsIgnoreCase("limit") || str.equalsIgnoreCase("offset")) {
        int number = Integer.parseInt(params.get(str));
        queryJson.put(str, number);
      } else {
        queryJson.put(str, params.get(str).strip().replaceAll("\"", ""));
      }
    }
    logger.info("search query : " + queryJson);
    database.searchQuery(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString("status");
        if (status.equalsIgnoreCase("success")) {
          response.setStatusCode(200);
        } else if (status.equalsIgnoreCase("partial-content")) {
          response.setStatusCode(206);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add("content-type", "application/json").add("content-length",
            String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        logger.info("response: " + resultJson);
        response.end();
      } else if (handler.failed()) {
        logger.error(handler.cause().getMessage());
        response.headers().add("content-type", "text");
        response.setStatusCode(500);
        response.end("Internal server error");
      }
    });
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
    queryJson.put("instanceID", instanceID).put("operation", "getCities");
    logger.info("search query : " + queryJson);
    database.searchQuery(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString("status");
        if (status.equalsIgnoreCase("success")) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add("content-type", "application/json").add("content-length",
            String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        logger.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        logger.info(handler.cause().getMessage());
        response.headers().add("content-type", "text");
        response.setStatusCode(500);
        response.end("Internal server error");
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
        queryJson.put("instanceID", instanceID);
        logger.info("search query : " + queryJson);
        database.createItem(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString("status");
            if (status.equalsIgnoreCase("success")) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add("content-type", "application/json").add("content-length",
                String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            logger.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            dbHandler.cause().getMessage();
            response.headers().add("content-type", "text");
            response.setStatusCode(500);
            response.end("Internal server error");
          }
        });
      } else if (validationHandler.failed()) {
        logger.info(validationHandler.cause().getMessage());
        response.headers().add("content-type", "text");
        response.setStatusCode(400);
        response.end("Bad Request");
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
        queryJson.put("instanceID", instanceID);
        logger.info("search query : " + queryJson);
        database.updateItem(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString("status");
            if (status.equalsIgnoreCase("success")) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add("content-type", "application/json").add("content-length",
                String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            logger.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            logger.info(dbHandler.cause().getMessage());
            response.headers().add("content-type", "text");
            response.setStatusCode(500);
            response.end("Internal server error");
          }
        });
      } else if (validationHandler.failed()) {
        logger.info(validationHandler.cause().getMessage());
        response.headers().add("content-type", "text");
        response.setStatusCode(400);
        response.end("Bad Request");
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
    queryJson.put("instanceID", instanceID).put("operation", "getConfig");
    logger.info("search query : " + queryJson);
    database.searchQuery(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString("status");
        if (status.equalsIgnoreCase("success")) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add("content-type", "application/json").add("content-length",
            String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        logger.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        logger.info(handler.cause().getMessage());
        response.headers().add("content-type", "text");
        response.setStatusCode(500);
        response.end("Internal server error");
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
        queryJson.put("instanceID", instanceID);
        logger.info("search query : " + queryJson);
        database.createItem(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString("status");
            if (status.equalsIgnoreCase("success")) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add("content-type", "application/json").add("content-length",
                String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            logger.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            logger.error(dbHandler.cause().getMessage());
            response.headers().add("content-type", "text");
            response.setStatusCode(500);
            response.end("Internal server error");
          }
        });
      } else if (validationHandler.failed()) {
        logger.error(validationHandler.cause().getMessage());
        response.headers().add("content-type", "text");
        response.setStatusCode(400);
        response.end("Bad Request");
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
    queryJson.put("instanceID", instanceID);
    logger.info("search query : " + queryJson);
    database.deleteItem(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString("status");
        if (status.equalsIgnoreCase("success")) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add("content-type", "application/json").add("content-length",
            String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        logger.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        logger.error(handler.cause().getMessage());
        response.headers().add("content-type", "text");
        response.setStatusCode(500);
        response.end("Internal server error");
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
        queryJson.put("instanceID", instanceID);
        logger.info("search query : " + queryJson);
        database.updateItem(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString("status");
            if (status.equalsIgnoreCase("success")) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add("content-type", "application/json").add("content-length",
                String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            logger.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            logger.error(dbHandler.cause().getMessage());
            response.headers().add("content-type", "text");
            response.setStatusCode(500);
            response.end("Internal server error");
          }
        });
      } else if (validationHandler.failed()) {
        logger.error(validationHandler.cause().getMessage());
        response.headers().add("content-type", "text");
        response.setStatusCode(400);
        response.end("Bad Request");
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
    String instanceID = request.getHeader("Host");

    /* checking and comparing itemType from the request body */
    if (requestBody.containsKey("itemType") && itemTypes.contains(requestBody.getString("itemType"))) {
      /* Populating query mapper */
      requestBody.put("instanceID", instanceID);

      /* checking auhthentication info in requests */
      if (request.headers().contains("token")) {
        authenticationInfo.put("token", request.getHeader("token"));

        /* Authenticating the request */
        authenticator.tokenInterospect(requestBody, authenticationInfo, authhandler -> {
          if (authhandler.succeeded()) {
            logger.info("Authenticating item creation request ".concat(authhandler.result().toString()));
            /* Validating the request */
            validator.validateItem(requestBody, valhandler -> {
              if (valhandler.succeeded()) {
                logger.info("Item creation validated".concat(authhandler.result().toString()));
                /* Requesting database service, creating a item */
                database.createItem(requestBody, dbhandler -> {
                  if (dbhandler.succeeded()) {
                    logger.info("Item created".concat(dbhandler.result().toString()));
                    response.putHeader("content-type", "application/json").setStatusCode(201)
                        .end(dbhandler.result().toString());
                  } else if (dbhandler.failed()) {
                    logger.error("Item creation failed".concat(dbhandler.cause().toString()));
                    response.putHeader("content-type", "application/json").setStatusCode(500)
                        .end(dbhandler.cause().toString());
                  }
                });
              } else if (valhandler.failed()) {
                logger.error("Item validation failed".concat(valhandler.cause().toString()));
                response.putHeader("content-type", "application/json").setStatusCode(500)
                    .end(valhandler.cause().toString());
              }
            });
          } else if (authhandler.failed()) {
            logger.error("Unathorized request".concat(authhandler.cause().toString()));
            response.putHeader("content-type", "application/json").setStatusCode(401)
                .end(authhandler.cause().toString());
          }
        });
      } else {
        logger.error("InvalidHeader, 'token' header");
        response.putHeader("content-type", "application/json").setStatusCode(400)
            .end(new ResponseHandler.Builder().withStatus("invalidHeader").build().toJsonString());
      }
    } else {
      logger.error("InvalidValue, 'itemType' attribute is missing or is empty");
      response.putHeader("content-type", "application/json").setStatusCode(400)
          .end(new ResponseHandler.Builder().withStatus("invalidValue").build().toJsonString());
    }
  }

  /**
   * Updates a already created item in the database. Endpoint: PATCH
   * /iudx/cat/v1/update/itemId
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
    String instanceID = request.getHeader("Host");

    /* Building complete itemID from HTTP request path parameters */
    String itemId = routingContext.pathParam("resItem").concat("/")
        .concat(routingContext.pathParam("resGrpItem").concat("/").concat(routingContext.pathParam("resSvrItem"))
            .concat("/").concat(routingContext.pathParam("pvdrItem").concat("/"))
            .concat(routingContext.pathParam("dataDesItem")));

    logger.info("Updating an item, Id: ".concat(itemId));

    /* checking and comparing itemType from the request body */
    if (requestBody.containsKey("itemType") && itemTypes.contains(requestBody.getString("itemType"))) {
      /* Populating query mapper */
      requestBody.put("instanceID", instanceID);

      /* checking auhthentication info in requests */
      if (request.headers().contains("token")) {
        authenticationInfo.put("token", request.getHeader("token"));

        /* Authenticating the request */
        authenticator.tokenInterospect(requestBody, authenticationInfo, authhandler -> {
          if (authhandler.succeeded()) {
            logger.info("Authenticating item update request ".concat(authhandler.result().toString()));
            /* Validating the request */
            validator.validateItem(requestBody, valhandler -> {
              if (valhandler.succeeded()) {
                logger.info("Item update validated ".concat(authhandler.result().toString()));
                /* Requesting database service, creating a item */
                database.updateItem(requestBody, dbhandler -> {
                  if (dbhandler.succeeded()) {
                    logger.info("Item updated ".concat(dbhandler.result().toString()));
                    response.putHeader("content-type", "application/json").setStatusCode(200)
                        .end(dbhandler.result().toString());
                  } else if (dbhandler.failed()) {
                    logger.error("Item update failed ".concat(dbhandler.cause().toString()));
                    response.putHeader("content-type", "application/json").setStatusCode(500)
                        .end(dbhandler.cause().toString());
                  }
                });
              } else if (valhandler.failed()) {
                logger.error("Item validation failed ".concat(valhandler.cause().toString()));
                response.putHeader("content-type", "application/json").setStatusCode(500)
                    .end(valhandler.cause().toString());
              }
            });
          } else if (authhandler.failed()) {
            logger.error("Unathorized request ".concat(authhandler.cause().toString()));
            response.putHeader("content-type", "application/json").setStatusCode(401)
                .end(authhandler.cause().toString());
          }
        });
      } else {
        logger.error("InvalidHeader 'token' header");
        response.putHeader("content-type", "application/json").setStatusCode(400)
            .end(new ResponseHandler.Builder().withStatus("invalidHeader").build().toJsonString());
      }
    } else {
      logger.error("InvalidValue, 'itemType' attribute is missing or is empty");
      response.putHeader("content-type", "application/json").setStatusCode(400)
          .end(new ResponseHandler.Builder().withStatus("invalidValue").build().toJsonString());
    }
  }

  /**
   * Deletes a created item in the database. Endpoint: DELETE
   * /iudx/cat/v1/delete/itemId
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
    String instanceID = request.getHeader("Host");

    /* Building complete itemID from HTTP request path parameters */
    String itemId = routingContext.pathParam("resItem").concat("/")
        .concat(routingContext.pathParam("resGrpItem").concat("/").concat(routingContext.pathParam("resSvrItem"))
            .concat("/").concat(routingContext.pathParam("pvdrItem").concat("/"))
            .concat(routingContext.pathParam("dataDesItem")));

    /* Populating query mapper */
    requestBody.put("instanceID", instanceID);
    requestBody.put("id", itemId);
    // requestBody.put("itemType", "Resource/ResourceGroup");

    logger.info("Deleting an item, Id: ".concat(itemId));

    /* checking auhthentication info in requests */
    if (request.headers().contains("token")) {
      authenticationInfo.put("token", request.getHeader("token"));

      /* Authenticating the request */
      authenticator.tokenInterospect(null, authenticationInfo, authhandler -> {
        if (authhandler.succeeded()) {
          logger.info("Authenticating item delete request".concat(authhandler.result().toString()));
          /* Requesting database service, creating a item */
          database.deleteItem(requestBody, dbhandler -> {
            if (dbhandler.succeeded()) {
              logger.info("Item deleted".concat(dbhandler.result().toString()));
              response.putHeader("content-type", "application/json").setStatusCode(200)
                  .end(dbhandler.result().toString());
            } else if (dbhandler.failed()) {
              logger.error("Item deletion failed".concat(dbhandler.cause().toString()));
              response.putHeader("content-type", "application/json").setStatusCode(400)
                  .end(dbhandler.cause().toString());
            }
          });
        } else if (authhandler.failed()) {
          logger.error("Unathorized request".concat(authhandler.cause().toString()));
          response.putHeader("content-type", "application/json").setStatusCode(401).end(authhandler.cause().toString());
        }
      });
    } else {
      logger.error("Invalid 'token' header");
      response.putHeader("content-type", "application/json").setStatusCode(400)
          .end(new ResponseHandler.Builder().withStatus("invalidHeader").build().toJsonString());
    }
  }

  /**
   * Geo Spatial property (Circle,Polygon) based database search. Validates the
   * request query params.
   * 
   * @param routingContext handles web requests in Vert.x Web
   * @TODO: Unused, remove
   */
  private void searchItem(RoutingContext routingContext) {

    logger.info("Searching the database for Item");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    /* Collection of query parameters from HTTP request */
    MultiMap queryParameters = routingContext.queryParams();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader("Host");

    /* Circle and Polygon based item search */
    if (queryParameters.contains("geoproperty") && !queryParameters.get("geoproperty").isBlank()
        && geoRels.contains(queryParameters.get("georel")) && queryParameters.contains("coordinates")
        && !queryParameters.get("coordinates").isBlank()) {

      /* validating circle or polygon as value of geometry query parameter */
      if ("Point".equals(queryParameters.get("geometry")) || "Polygon".equals(queryParameters.get("geometry"))) {

        /* converting multimap to proper JsonObject/JsonArray format */
        requestBody = map2Json(queryParameters);

        if (requestBody != null) {
          /* Populating query mapper */
          requestBody.put("instanceID", instanceID);
          /* Request database service with requestBody for item search */
          database.searchQuery(requestBody, dbhandler -> {
            if (dbhandler.succeeded()) {
              logger.info("Search completed ".concat(dbhandler.result().toString()));
              response.putHeader("content-type", "application/json").setStatusCode(200)
                  .end(dbhandler.result().toString());
            } else if (dbhandler.failed()) {
              logger.error("Issue in Item search ".concat(dbhandler.cause().toString()));
              response.putHeader("content-type", "application/json").setStatusCode(400)
                  .end(dbhandler.cause().toString());
            }
          });
        } else {
          response.putHeader("content-type", "application/json").setStatusCode(400)
              .end(new ResponseHandler.Builder().withStatus("invalidValue").build().toJsonString());
        }
      } else {
        logger.error("Invalid Query parameter values, Expected: 'geometry = Point|Polygon|LineString|bbox'");
        response.putHeader("content-type", "application/json").setStatusCode(400)
            .end(new ResponseHandler.Builder().withStatus("invalidValue").build().toJsonString());
      }
    } else {
      logger.error("Invalid Query parameter syntax, Expected: 'geoproperty, georel, coordinates'");
      response.putHeader("content-type", "application/json").setStatusCode(400)
          .end(new ResponseHandler.Builder().withStatus("invalidSyntax").build().toJsonString());
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
    String instanceID = request.getHeader("Host");

    /* Building complete itemID from HTTP request path parameters */
    String itemId = routingContext.pathParam("resItem").concat("/")
        .concat(routingContext.pathParam("resGrpItem").concat("/").concat(routingContext.pathParam("resSvrItem"))
            .concat("/").concat(routingContext.pathParam("pvdrItem").concat("/"))
            .concat(routingContext.pathParam("dataDesItem")));

    /* Populating query mapper */
    requestBody.put("id", itemId);
    requestBody.put("instanceID", instanceID);

    /* Databse service call for listing item */
    database.listItem(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of items ".concat(dbhandler.result().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing items ".concat(dbhandler.cause().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(400).end(dbhandler.cause().toString());
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
    String instanceID = request.getHeader("Host");

    /* Populating query mapper */
    requestBody.put("instanceID", instanceID);

    /* Request database service with requestBody for listing tags */
    database.listTags(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of tags ".concat(dbhandler.result().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing tags ".concat(dbhandler.cause().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(400).end(dbhandler.cause().toString());
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
    String instanceID = request.getHeader("Host");

    /* Populating query mapper */
    requestBody.put("instanceID", instanceID);

    /* Request database service with requestBody for listing domains */
    database.listDomains(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of domains ".concat(dbhandler.result().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing domains ".concat(dbhandler.cause().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(400).end(dbhandler.cause().toString());
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
    String instanceID = request.getHeader("Host");

    /* Populating query mapper */
    requestBody.put("instanceID", instanceID);

    /* Request database service with requestBody for listing cities */
    database.listCities(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of cities ".concat(dbhandler.result().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing cities ".concat(dbhandler.cause().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(400).end(dbhandler.cause().toString());
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
    String instanceID = request.getHeader("Host");

    /* Populating query mapper */
    requestBody.put("instanceID", instanceID);

    /* Request database service with requestBody for listing resource servers */
    database.listResourceServers(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of resource servers ".concat(dbhandler.result().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing resource servers ".concat(dbhandler.cause().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(400).end(dbhandler.cause().toString());
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
    String instanceID = request.getHeader("Host");

    /* Populating query mapper */
    requestBody.put("instanceID", instanceID);

    /* Request database service with requestBody for listing providers */
    database.listProviders(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of providers ".concat(dbhandler.result().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing providers ".concat(dbhandler.cause().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(400).end(dbhandler.cause().toString());
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
    String instanceID = request.getHeader("Host");

    /* Populating query mapper */
    requestBody.put("instanceID", instanceID);

    /* Request database service with requestBody for listing resource groups */
    database.listResourceGroups(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of resource groups ".concat(dbhandler.result().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing resource groups ".concat(dbhandler.cause().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(400).end(dbhandler.cause().toString());
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
    String instanceID = request.getHeader("Host");

    /* Parsing id from HTTP request */
    String id = request.getParam("id");

    /* Checking if id is either not null nor empty */
    if (id != null && !id.isBlank()) {

      /* Populating query mapper */
      requestBody.put("id", id);
      requestBody.put("instanceID", instanceID);
      requestBody.put("relationship", "resource");

      /*
       * Request database service with requestBody for listing resource relationship
       */
      database.listResourceRelationship(requestBody, dbhandler -> {
        if (dbhandler.succeeded()) {
          logger.info("List of resources belonging to resourceGroups ".concat(dbhandler.result().toString()));
          response.putHeader("content-type", "application/json").setStatusCode(200).end(dbhandler.result().toString());
        } else if (dbhandler.failed()) {
          logger.error("Issue in listing resource relationship ".concat(dbhandler.cause().toString()));
          response.putHeader("content-type", "application/json").setStatusCode(400).end(dbhandler.cause().toString());
        }
      });
    } else {
      logger.error("Issue in path parameter");
      response.putHeader("content-type", "application/json").setStatusCode(400)
          .end(new ResponseHandler.Builder().withStatus("invalidSyntax").build().toJsonString());
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
    String instanceID = request.getHeader("Host");

    /* Parsing id from HTTP request */
    String id = request.getParam("id");

    /* Checking if id is either not null nor empty */
    if (id != null && !id.isBlank()) {

      /* Populating query mapper */
      requestBody.put("id", id);
      requestBody.put("instanceID", instanceID);
      requestBody.put("relationship", "resourceGroup");

      /*
       * Request database service with requestBody for listing resource group
       * relationship
       */
      database.listResourceGroupRelationship(requestBody, dbhandler -> {
        if (dbhandler.succeeded()) {
          logger.info("List of resourceGroup belonging to resource ".concat(dbhandler.result().toString()));
          response.putHeader("content-type", "application/json").setStatusCode(200).end(dbhandler.result().toString());
        } else if (dbhandler.failed()) {
          logger.error("Issue in listing resourceGroup relationship ".concat(dbhandler.cause().toString()));
          response.putHeader("content-type", "application/json").setStatusCode(400).end(dbhandler.cause().toString());
        }
      });
    } else {
      logger.error("Issue in path parameter");
      response.putHeader("content-type", "application/json").setStatusCode(400)
          .end(new ResponseHandler.Builder().withStatus("invalidSyntax").build().toJsonString());
    }
  }

  /**
   * Converts the MultiMap to JsonObject. Checks/validates the value of JsonArray.
   * 
   * @param queryParameters is a MultiMap of request query parameters
   * @return jsonObject of MultiMap query parameters
   */
  private JsonObject map2Json(MultiMap queryParameters) {

    JsonObject jsonBody = new JsonObject();

    for (Entry<String, String> entry : queryParameters.entries()) {
      /* checking if the Collection value is of JsonObject/JsonArray */
      if (!entry.getValue().startsWith("[") && !entry.getValue().endsWith("]")) {
        jsonBody.put(entry.getKey(), entry.getValue());
      } else {
        try {
          jsonBody.put(entry.getKey(), new JsonArray(entry.getValue()));
        } catch (DecodeException decodeException) {
          logger.error("Invalid Json value ".concat(decodeException.toString()));
          return null;
        }
      }
    }
    return jsonBody;
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
        queryJson.put("instanceID", instanceID);
        logger.info("search query : " + queryJson);
        database.updateItem(queryJson, dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject resultJson = dbHandler.result();
            String status = resultJson.getString("status");
            if (status.equalsIgnoreCase("success")) {
              response.setStatusCode(201);
            } else {
              response.setStatusCode(400);
            }
            response.headers().add("content-type", "application/json").add("content-length",
                String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            logger.info("response : " + resultJson);
            response.end();
          } else if (dbHandler.failed()) {
            logger.error(dbHandler.cause().getMessage());
            response.headers().add("content-type", "text");
            response.setStatusCode(500);
            response.end("Internal server error");
          }
        });
      } else if (validationHandler.failed()) {
        logger.error(validationHandler.cause().getMessage());
        response.headers().add("content-type", "text");
        response.setStatusCode(400);
        response.end("Bad Request");
      }
    });
  }

  /**
   * Queries the database and returns all resource servers belonging to an item.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void getResourceServer(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = new JsonObject();
    String instanceID = routingContext.request().host();
    String id = routingContext.request().getParam("id");
    queryJson.put("instanceID", instanceID).put("id", id).put("relationship", "resourceServer");
    logger.info("search query : " + queryJson);
    database.searchQuery(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString("status");
        if (status.equalsIgnoreCase("success")) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add("content-type", "application/json").add("content-length",
            String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        logger.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        logger.error(handler.cause().getMessage());
        response.headers().add("content-type", "text");
        response.setStatusCode(500);
        response.end("Internal server error");
      }
    });
  }

  /**
   * Queries the database and returns provider of an item.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void getProvider(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = new JsonObject();
    String instanceID = routingContext.request().host();
    String id = routingContext.request().getParam("id");
    queryJson.put("instanceID", instanceID).put("id", id).put("relationship", "provider");
    logger.info("search query : " + queryJson);
    database.searchQuery(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString("status");
        if (status.equalsIgnoreCase("success")) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add("content-type", "application/json").add("content-length",
            String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        logger.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        logger.error(handler.cause().getMessage());
        response.headers().add("content-type", "text");
        response.setStatusCode(500);
        response.end("Internal server error");
      }
    });
  }

  /**
   * Queries the database and returns data model of an item.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void getDataModel(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = new JsonObject();
    String instanceID = routingContext.request().host();
    String id = routingContext.request().getParam("id");
    queryJson.put("instanceID", instanceID).put("id", id).put("relationship", "type");
    logger.info("search query : " + queryJson);
    database.searchQuery(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString("status");
        if (status.equalsIgnoreCase("success")) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add("content-type", "application/json").add("content-length",
            String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        logger.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        logger.error(handler.cause().getMessage());
        response.headers().add("content-type", "text");
        response.setStatusCode(500);
        response.end("Internal server error");
      }
    });
  }
}
