package iudx.catalogue.server.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Validator {

  public static final Logger LOGGER = LogManager.getLogger(Validator.class);
  private static final String PKGBASE;
  private final JsonSchema schema;

  static {
    final String pkgName = Validator.class.getPackage().getName();
    PKGBASE = '/' + pkgName.replace(".", "/");
  }

  /**
   * Creates a new instance of Validator that can validate JSON objects against a given JSON schema.
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
  public boolean validate(String obj) {
    boolean isValid;
    try {
      JsonNode jsonobj = loadString(obj);
      isValid = schema.validInstance(jsonobj);
    } catch (IOException | ProcessingException e) {
      isValid = false;
    }
    return isValid;
  }
}
