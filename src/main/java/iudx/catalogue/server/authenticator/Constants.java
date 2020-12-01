package iudx.catalogue.server.authenticator;

public class Constants {

    public static final String AUTH_SERVER_HOST = "authServerHost";
    public static final String AUTH_CERTINFO_PATH = "/auth/v1/certificate-info";
    public static final String DUMMY_TOKEN_KEY = "authDummyToken";
    public static final String DUMMY_PROVIDER_PREFIX = "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc";
    public static final String AUTH_TIP_PATH = "/auth/v1/token/introspect";

    public static final String TOKEN = "token";
    public static final String OPERATION = "operation";
    public static final String REQUEST = "request";
    public static final String BODY = "body";

    public static final String AUTH_SERVER_ERROR = "Error calling the Auth Server";

    public static final int TOKEN_SIZE = 512;
    public static final String TOKEN_REGEX = "^[a-zA-Z0-9\\/\\@\\.]*$";

}
