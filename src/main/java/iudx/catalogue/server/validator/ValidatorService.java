package iudx.catalogue.server.validator;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Validator Service.
 *
 * <h1>Validator Service</h1>
 *
 * <p>The Validator Service in the IUDX Catalogue Server defines the operations to be performed with
 * the IUDX File server.
 *
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-05-31
 */
@VertxGen
@ProxyGen
public interface ValidatorService {

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   *
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return ValidatorServiceVertxEBProxy which is a service proxy
   */
  @GenIgnore
  static ValidatorService createProxy(Vertx vertx, String address) {
    return new ValidatorServiceVertxEBProxy(vertx, address);
  }

  /**
   * The validateSchema method implements the item schema validation.
   *
   * @param request which is a JsonObject
   * @return Future<JsonObject> which is a Vert.x Future
   */
  Future<JsonObject> validateSchema(JsonObject request);

  /**
   * The validateItem method implements the item validation flow based on the schema of the item.
   *
   * @param request which is a JsonObject
   * @return Future<JsonObject> which is a Vert.x Future
   */

  /*
   * {@inheritDoc}
   */
  Future<JsonObject> validateItem(JsonObject request);

  Future<JsonObject> validateRating(JsonObject request);

  Future<JsonObject> validateMlayerInstance(JsonObject request);

  Future<JsonObject> validateMlayerDomain(JsonObject request);

  Future<JsonObject> validateMlayerGeoQuery(JsonObject request);

  Future<JsonObject> validateMlayerDatasetId(JsonObject requestData);
}
