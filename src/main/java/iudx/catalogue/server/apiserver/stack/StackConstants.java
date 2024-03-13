package iudx.catalogue.server.apiserver.stack;

public class StackConstants {
  static final String DOC_ID = "_id";
  static String GET_STACK_QUERY =
      "{\n"
          + "  \"query\": {\n"
          + "    \"bool\": {\n"
          + "      \"must\": [\n"
          + "        {\n"
          + "          \"match\": {\n"
          + "            \"id.keyword\":\"$1\"\n"
          + "          }\n"
          + "        }\n"
          + "      ]\n"
          + "    }\n"
          + "  }\n"
          + "}";
  static String CHECK_STACK_EXISTANCE_QUERY =
      "{\n"
          + "  \"query\": {\n"
          + "    \"bool\": {\n"
          + "      \"should\": [\n"
          + "        {\n"
          + "          \"bool\": {\n"
          + "            \"must\": [\n"
          + "              {\n"
          + "                \"match\": {\n"
          + "                  \"links.rel.keyword\": \"self\"\n"
          + "                }\n"
          + "              },\n"
          + "              {\n"
          + "                \"match\": {\n"
          + "                  \"links.href.keyword\":\"$1\"\n"
          + "                }\n"
          + "              }\n"
          + "              ]\n"
          + "          }\n"
          + "        },\n"
          + "        {\n"
          + "          \"bool\": {\n"
          + "            \"must\": [\n"
          + "              {\n"
          + "                \"match\": {\n"
          + "                  \"links.rel.keyword\":\"root\"\n"
          + "                }\n"
          + "              },\n"
          + "              {\n"
          + "                \"match\": {\n"
          + "                  \"links.href.keyword\":\"$2\"\n"
          + "                }\n"
          + "              }\n"
          + "              ]\n"
          + "          }\n"
          + "        }\n"
          + "        ]\n"
          + "    }\n"
          + "  }\n"
          + "}";
  static String PATCH_QUERY =
      "{\n"
          + "  \"script\": {\n"
          + "    \"source\": \"ctx._source.links.add(params)\",\n"
          + "    \"lang\": \"painless\",\n"
          + "    \"params\": {\n"
          + "      \"rel\": \"$1\",\n"
          + "      \"href\": \"$2\"\n";

  static String TYPE = " \"type\": \"$1\"\n";
  static String TITLE = " \"title\": \"$1\"\n";

  static String CLOSED_QUERY = "    }\n" + "  }\n" + "}";
}
