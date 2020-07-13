package iudx.catalogue.server.authenticator;

import java.util.List;

public class Constants {
    public static final String CONFIG_FILE = "config.properties";
    public static final String KEYSTORE_PATH = "keystore";
    public static final String KEYSTORE_PASSWORD = "keystorePassword";
    public static final String AUTH_SERVER_HOST = "auth.iudx.org.in";
    public static final String AUTH_CERTINFO_PATH = "/auth/v1/certificate-info";
    public static final String DUMMY_TOKEN_KEY = "authDummyToken";
    public static final String INVALID_TYPE_STRING = "iudx:Invalid";
    public static final String RESOURCE_TYPE_STRING = "iudx:Resource";
    public static final String RESOURCE_GROUP_TYPE_STRING = "iudx:ResourceGroup";
    public static final List<String> VALID_TYPE_STRINGS = List.of(RESOURCE_TYPE_STRING, RESOURCE_GROUP_TYPE_STRING);
    public static final String DUMMY_RESOURCE_ID = "datakaveri.org/1022f4c20542abd5087107c0b6de4cb3130c5b7b/example.com/catalog/resource_name";
    public static final String DUMMY_RESOURCE_NAME = "resource_name";
    public static final String DUMMY_RESOURCE_GROUP = "catalog";
    public static final String AUTH_TIP_PATH = "/auth/v1/token/introspect";
}
