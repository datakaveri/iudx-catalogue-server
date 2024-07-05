package iudx.catalogue.server.database.elastic;

import static iudx.catalogue.server.util.Constants.*;

import java.util.stream.Stream;

public enum ResponseUrn {
  INVALID_PROPERTY_VALUE_URN(TYPE_INVALID_PROPERTY_VALUE, TITLE_INVALID_PROPERTY_VALUE),
  OPERATION_NOT_ALLOWED_URN(TYPE_OPERATION_NOT_ALLOWED, TITLE_OPERATION_NOT_ALLOWED),
  BAD_FILTER_URN(TYPE_BAD_FILTER, TITLE_BAD_FILTER),
  INVALID_SYNTAX_URN(TYPE_INVALID_SYNTAX, TITLE_INVALID_SYNTAX),
  BAD_REQUEST_URN("urn:dx:cat:badRequest", "bad request parameter"),
  BAD_TEXT_QUERY_URN(TYPE_BAD_TEXT_QUERY, TITLE_BAD_TEXT_QUERY),
  YET_NOT_IMPLEMENTED_URN("urn:dx:cat:general", "urn yet not implemented in backend verticle."),
  INVALID_COORDINATE_POLYGON_URN(TYPE_INVALID_GEO_VALUE, TITLE_INVALID_GEO_VALUE),
  INVALID_GEO_PARAMETER_URN(TYPE_INVALID_GEO_PARAM, TITLE_INVALID_GEO_PARAM);


  private final String urn;
  private final String message;

  ResponseUrn(String urn, String message) {
    this.urn = urn;
    this.message = message;
  }

  public static ResponseUrn fromCode(final String urn) {
    return Stream.of(values())
        .filter(v -> v.urn.equalsIgnoreCase(urn))
        .findAny()
        .orElse(YET_NOT_IMPLEMENTED_URN); // if backend service dont respond with urn
  }

  public String getUrn() {
    return urn;
  }

  public String getMessage() {
    return message;
  }

  public String toString() {
    return "[" + urn + " : " + message + " ]";
  }
}
