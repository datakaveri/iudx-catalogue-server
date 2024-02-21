package iudx.catalogue.server.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Validator {

  public static final Logger LOGGER = LogManager.getLogger(Validator.class);
  private static final String PKGBASE;

  static {
    final String pkgName = Validator.class.getPackage().getName();
    PKGBASE = '/' + pkgName.replace(".", "/");
  }

  private final JsonSchema schema;

  /**
   * Creates a new instance of Validator that can validate JSON objects against a given JSON schema.
   *
   * @param schemaPath a String that represents the path of the JSON schema file
   * @throws IOException if there is an error reading the schema file
   */
  public Validator(String schemaPath) throws IOException, ProcessingException {
    final JsonNode schemaNode = loadResource(schemaPath);
    final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    schema = factory.getJsonSchema(schemaNode);
  }

  /**
   * Load one resource from the current package as a {@link JsonNode}.
   *
   * @param name name of the resource (<b>MUST</b> start with {@code /}
   * @return a JSON document
   * @throws IOException resource not found
   */
  public static JsonNode loadResource(final String name) throws IOException {
    return JsonLoader.fromResource(PKGBASE + name);
  }

  /**
   * Load one resource from a string {@link JsonNode}.
   *
   * @param obj Json encoded object
   * @return a JSON document
   * @throws IOException resource not found
   */
  public static JsonNode loadString(final String obj) throws IOException {
    return JsonLoader.fromString(obj);
  }

  /**
   * Check validity of json encoded string.
   *
   * @param obj Json encoded string object
   * @return isValid boolean
   */
  public Future<String> validate(String obj) {
    Promise<String> promise = Promise.promise();
    boolean isValid;
      List<String> schemaErrorList = new ArrayList<>();
    try {
      JsonNode jsonobj = loadString(obj);
      ProcessingReport report = schema.validate(jsonobj);
      report.forEach(
          x -> {
            if (x.getLogLevel().toString().equalsIgnoreCase("error")) {
              LOGGER.error(x.getMessage());
              schemaErrorList.add(x.getMessage());
            }
          });
      isValid = report.isSuccess();

    } catch (IOException | ProcessingException e) {
      isValid = false;
      schemaErrorList.add(e.getMessage());
    }

    if(isValid) {
      promise.complete();
    } else {
      promise.fail(schemaErrorList.toString());
    }
    return promise.future();
  }
}
