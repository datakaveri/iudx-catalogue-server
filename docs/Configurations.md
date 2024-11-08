<p align="center">
<img src="./cdpg.png" width="300">
</p>

# Modules
This document contains the information on the configurations needed to set up various services and dependencies to bring up the DX Catalogue Server.
Please find the example configuration file [here](https://github.com/datakaveri/iudx-catalogue-server/blob/master/configs/config-example.json).
While running the server, the `config-dev.json` file could be added [configs](https://github.com/datakaveri/iudx-catalogue-server/blob/master/configs).

## Other Configuration

| Key Name                    | Value Datatype | Value Example          | Description                                                                                                                        |
|:----------------------------|:--------------:|:-----------------------|:-----------------------------------------------------------------------------------------------------------------------------------|
| version                     |     Float      | 1.0                    | config version                                                                                                                     |
| zookeepers                  |     Array      | zookeeper              | zookeeper configuration to deploy clustered vert.x instance                                                                        |
| clusterId                   |     String     | iudx-catalogue-cluster | Cluster ID to deploy clustered Vert.x instance                                                                                     |
| commonConfig.dxApiBasePath  |     String     | /dx/api/v1             | API base path for DX API. Reference: [link](https://swagger.io/docs/specification/2-0/api-host-and-base-p                          |
| commonConfig.dxAuthBasePath |     String     | /auth/v1               | API base path for DX authentication server. Reference: [link](https://swagger.io/docs/specification/2-0/api-host-and-base-path/)   |
| commonConfig.isUACInstance  |    boolean     | false                  | Check if UAC instance needs to be deployed                                                                                         |

## Database Verticle

| Key Name                     | Value Datatype | Value Example                                                          | Description                                            |
|:-----------------------------|:--------------:|:-----------------------------------------------------------------------|:-------------------------------------------------------|
| id                           |     String     | iudx.catalogue.server.database.DatabaseVerticle                        | Class name for the Database Verticle                   |
| optionalModules              |     Array      | ["iudx.catalogue.server.geocoding", "iudx.catalogue.server.nlpsearch"] | Optional modules for this verticle                     |
| verticleInstances            |    integer     | 2                                                                      | Number of instances required for the Database Verticle |
| databaseIP                   |     String     | ""                                                                     | Postgres Database IP address                           |
| ratingIndex                  |     String     | ""                                                                     | Elasticsearch index for storing ratings data           |
| docIndex                     |     String     | ""                                                                     | Document index                                         |
| mlayerInstanceIndex          |     String     | ""                                                                     | Elasticsearch index for Mlayer instances               |
| mlayerDomainIndex            |     String     | ""                                                                     | Mlayer domain index                                    |
| databasePort                 |    integer     | 1234                                                                   | Elasticsearch Port number                              |
| databaseUser                 |     String     | dbUserName                                                             | Elasticsearch user name                                |
| databasePassword             |     String     | dbPassword                                                             | Password for Elasticsearch user                        |
| bypassAuth                   |    boolean     | true                                                                   | Bypass authentication for this verticle                |

## Authentication Verticle

| Key Name          | Value Datatype | Value Example                                              | Description                                                            |
|:------------------|:--------------:|:-----------------------------------------------------------|:-----------------------------------------------------------------------|
| id                |     String     | iudx.catalogue.server.authenticator.AuthenticationVerticle | Class name for the Authentication Verticle                             |
| host              |     String     | ""                                                         | Host address for the authentication server                             |
| consumerHost      |     String     | ""                                                         | Host address for the consumer application that requires authentication |
| verticleInstances |    integer     | 1                                                          | Number of instances required for the Authentication Verticle           |
| authServerHost    |     String     | auth.iudx.io                                               | Host name of the authentication server                                 |
| issuer            |     String     | cos.iudx.io                                                | Data Exchange (DX) COS URL to authenticate the issuer in the token     |
| jwtIgnoreExpiry   |    boolean     | true                                                       | Set to true while using the server locally to allow expired tokens     |

## Validator Verticle

| Key Name                | Value Datatype | Value Example                                     | Description                                             |
|:------------------------|:--------------:|:--------------------------------------------------|:--------------------------------------------------------|
| id                      |     String     | iudx.catalogue.server.validator.ValidatorVerticle | Class name for the  Validator Verticle                  |
| verticleInstances       |    integer     | 2                                                 | Number of instances required for the Validator Verticle |
| databaseIP              |     String     | localhost                                         | Elasticsearch IP address                                |
| databasePort            |    integer     | 1234                                              | Elasticsearch Port number                               |
| databaseUser            |     String     | dbUserName                                        | Elasticsearch user name                                 |
| databasePassword        |     String     | dbPassword                                        | Password for Elasticsearch user                         |
| docIndex                |     String     | ""                                                | The index in Elasticsearch where documents are stored   |
| @context                |     String     | ""                                                | Context metadata related to the Validator Verticle      |

## API Server Verticle

| Key Name                  | Value Datatype | Value Example                                     | Description                                                   |
|:--------------------------|:--------------:|:--------------------------------------------------|:--------------------------------------------------------------|
| id                        |   String       | iudx.catalogue.server.apiserver.ApiServerVerticle | Class path for the ApiServer Verticle                         |
| catAdmin                  |     String     | ""                                                | Credential or identifier for the catalog administrator        |
| verticleInstances         |    integer     | 2                                                 | Number of instances required for the ApiServer Verticle       |
| ip                        |     String     | ""                                                | IP address for the ApiServer                                  |
| httpPort                  |    integer     | 8080                                              | Port number for HTTP connections to the ApiServer             |
| ssl                       |    boolean     | false                                             | Flag indicating whether SSL is enabled for secure connections |
| host                      |     String     | ""                                                | Hostname for the ApiServer                                    |
| databaseIP                |     String     | ""                                                | IP address of the Elasticsearch                               |
| docIndex                  |     String     | ""                                                | The index in Elasticsearch where documents are stored         |
| databaseUser              |     String     | ""                                                | Username for accessing the Elasticsearch                      |
| databasePassword          |     String     | ""                                                | Password for the Elasticsearch user                           |
| databasePort              |    integer     | 123                                               | Port number for connecting to the Elasticsearch               |

## Auditing Verticle

| Key Name                  | Value Datatype | Value Example                                   | Description                                                            |
|:--------------------------|:--------------:|:------------------------------------------------|:-----------------------------------------------------------------------|
| id                        |     String     | iudx.catalogue.server.auditing.AuditingVerticle | Class name for the  Auditing Verticle                                  |
| verticleInstances         |    integer     | 1                                               | Number of instances required for the Auditing Verticle                 |
| auditingDatabaseIP        |     String     | localhost                                       | Postgres Database IP address                                           |
| auditingDatabasePort      |    integer     | 5432                                            | Postgres Database Port number                                          |
| auditingDatabaseName      |     String     | auditingDB                                      | Name of the auditing database                                          |
| auditingDatabaseUserName  |     String     | auditUser                                       | User name for Postgres database                                        |
| auditingDatabasePassword  |     String     | auditPassword                                   | Password for Postgres database                                         |
| auditingDatabaseTableName |     String     | auditTable                                      | Name of the table in the Postgres database where audit logs are stored |
| auditingPoolSize          |    integer     | 25                                              | Pool size for Postgres database client                                 |

## Geocoding Verticle

| Key Name            | Value Datatype | Value Example                                     | Description                                             |
|:--------------------|:--------------:|:--------------------------------------------------|:--------------------------------------------------------|
| id                  |     String     | iudx.catalogue.server.geocoding.GeocodingVerticle | Class name for the  Geocoding Verticle                  |
| verticleInstances   |    integer     | 2                                                 | Number of instances required for the Geocoding Verticle |
| peliasUrl           |     String     | http://pelias.io                                  | URL for Pelias Server                                   |
| peliasPort          |    integer     | 4000                                              | Port for Pelias geocoding service                       |

## NLP Search Verticle

| Key Name                   | Value Datatype | Value Example                                     | Description                                              |
|:---------------------------|:--------------:|:--------------------------------------------------|:---------------------------------------------------------|
| id                         |     String     | iudx.catalogue.server.nlpsearch.NLPSearchVerticle | Class name for the NLP Search Verticle                   |
| verticleInstances          |     integer    | 2                                                 | Number of instances required for the NLP Search Verticle |
| nlpServiceUrl              |     String     | http://nlpservice.io                              | URL for NLP search service                               |
| nlpServicePort             |    integer     | 3000                                              | Port for NLP search service                              |

## Rating Verticle

| Key Name                   | Value Datatype | Value Example                               | Description                                          |
|:---------------------------|:--------------:|:--------------------------------------------|:-----------------------------------------------------|
| id                         |     String     | iudx.catalogue.server.rating.RatingVerticle | Class name for the Rating Verticle                   |
| verticleInstances          |    integer     | 1                                           | Number of instances required for the Rating Verticle |
| ratingExchangeName         |     String     | ratings.exchange                            | RabbitMQ Exchange name for rating                    |
| rsAuditingTableName        |     String     | ratings_auditing                            | Auditing table name for ratings                      |
| minReadNumber              |    integer     | 100                                         | Minimum number of reads for rating                   |

## Data Broker Verticle

| Key Name                   | Value Datatype | Value Example                                       | Description                                                                                                                                                                           |
|:---------------------------|:--------------:|:----------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| id                         |     String     | iudx.catalogue.server.databroker.DataBrokerVerticle | Class name for the Data Broker Verticle                                                                                                                                               |
| verticleInstances          |    integer     | 1                                                   | Number of instances required for the Data Broker Verticle                                                                                                                             |
| dataBrokerIP               |     String     | ""                                                  | IP address of the data broker (RabbitMQ)                                                                                                                                              |
| dataBrokerPort             |     integer    | 1234                                                | RabbitMQ Port number                                                                                                                                                                  |
| dataBrokerVhost            |     String     | ""                                                  | Virtual host for the RMQ connection                                                                                                                                                   |
| dataBrokerUserName         |     String     | ""                                                  | RabbitMQ Username                                                                                                                                                                     |
| dataBrokerPassword         |     String     | ""                                                  | RabbitMQ Password                                                                                                                                                                     |
| connectionTimeout          |    integer     | 6000                                                | Maximum time in milliseconds to wait for a connection to the data broker before timing out                                                                                            |
| requestedHeartbeat         |    integer     | 60                                                  | Heartbeat interval in seconds to ensure the connection is alive                                                                                                                       |
| handshakeTimeout           |    integer     | 6000                                                | Maximum time in milliseconds to wait for the handshake process to complete                                                                                                            |
| requestedChannelMax        |    integer     | 5                                                   | Maximum number of channels that can be opened on the connection                                                                                                                       |
| networkRecoveryInterval    |    integer     | 500                                                 | Time in milliseconds to wait before attempting to reconnect after a network failure                                                                                                   |
| automaticRecoveryEnabled   |    boolean     | true/false                                          | Indicates whether automatic recovery of the connection is enabled (true/false)                                                                                                        |

## Mlayer Verticle

| Key Name             | Value Datatype  | Value Example                               | Description                                                                  |
|:---------------------|:----------------|:--------------------------------------------|:-----------------------------------------------------------------------------|
| id                   | String          | iudx.catalogue.server.mlayer.MlayerVerticle | Class name for the Mlayer Verticle                                           |
| verticleInstances    | integer         | 1                                           | Number of instances required for the Mlayer Verticle                         |
| databaseTable        | String          | ""                                          | Name of the database table used by the Mlayer Verticle for auditing          |
| catSummaryTable      | String          | ""                                          | Name of the summary table for catalog information within the Mlayer Verticle |
| excluded_ids         | Array           | []                                          | List of IDs to be excluded from processing by the Mlayer Verticle            |

## Postgres Verticle

| Key Name             | Value Datatype | Value Example                                            | Description                                                                                      |
|:---------------------|:---------------|:---------------------------------------------------------|:-------------------------------------------------------------------------------------------------|
| id                   | String         | iudx.catalogue.server.database.postgres.PostgresVerticle | Class name for the Postgres Verticle                                                             |
| isWorkerVerticle     | Boolean        | false                                                    | Indicates whether this verticle is a worker verticle. Worker verticles run in a separate thread. |
| verticleInstances    | integer        | 1                                                        | Number of instances required for the Postgres Verticle                                           |
| databaseIP           | String         | ""                                                       | IP address of the Postgres database.                                                             |
| databasePort         | integer        | 5432                                                     | Port number for connecting to the Postgres database.                                             |
| databaseName         | String         | ""                                                       | Name of the Postgres database to connect to.                                                     |
| databaseUserName     | String         | ""                                                       | Username for authentication to the Postgres database.                                            |
| databasePassword     | String         | ""                                                       | Password for authentication to the Postgres database.                                            |
| poolSize             | integer        | 25                                                       | Maximum number of connections in the connection pool for the Postgres database.                  |
