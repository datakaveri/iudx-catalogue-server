<p align="center">
<img src="./cdpg.png" width="300">
</p>

# Setup and Installation Guide
This document contains the installation and configuration information required to deploy the 
Data Exchange (DX) Catalogue Server.

## Configuration
In order to connect the DX Catalogue Server with PostgreSQL, RabbitMQ, ElasticSearch, Pelias 
Servers for GeoCoding and NLP Services, DX AAA Server, etc please refer [Configurations](./Configurations.md). It contains appropriate information which shall be updated as per the deployment.

## Dependencies
In this section we explain about the dependencies and their scope. It is expected that the dependencies are met before starting the deployment of DX Catalogue Server.

### External Dependencies
| Software Name | Purpose                                                                                                               |
|:--------------|:----------------------------------------------------------------------------------------------------------------------|
| ElasticSearch | For indexing the meta-data                                                                                            |
| PostgreSQL    | For storing information related to access request based CRUD operations, approved access requests in auditing table   |
| RabbitMQ      | To publish auditing related data to auditing server via RabbitMQ exchange                                             |
| Pelias Server | To access the information related to geocoding service and nlp service APIs                                           |


### Internal Dependencies
| Software Name                                               | Purpose                                                                  |
|:------------------------------------------------------------|:-------------------------------------------------------------------------|
| DX Authentication Authorization and Accounting (AAA) Server | Used to download certificate for JWT token decoding and to get user info |

## Prerequisites
### Keycloak registration for DX Catalogue as trustee and APD
- The trustee user must be registered on Keycloak as a user
    - This can be done via the keycloak admin panel, or by using Data Exchange (DX) UI
    - The trustee user need not have any roles beforehand
- The COS admin user must call the create APD API : [Link to the API](https://authorization.iudx.org.in/apis#tag/Access-Policy-Domain-(APD)-APIs/operation/post-auth-v1-apd)
  with the name as the name of the APD, owner as email address of trustee (as registered on Keycloak)
  and URL as the domain of the APD
- Once the APD has been successfully registered, the trustee user will gain the trustee role
  scoped to that particular APD.
    - They can verify this by calling the list user roles API : [Link to the API](https://authorization.iudx.org.in/apis#tag/User-APIs/operation/get-auth-v1-user-roles)
- The trustee can get client credentials to be used in APD Operations by calling the
  get default client credentials API : [Link to the API](https://authorization.iudx.org.in/apis#tag/User-APIs/operation/get-auth-v1-user-clientcredentials)

### ELK
- To setup Elasticsearch-Logstash-Kibana(ELK) and installation instructions available [here]
  (https://github.com/datakaveri/iudx-deployment/tree/master/Docker-Swarm-deployment/single-node/elk)

#### RabbitMQ
- To setup RabbitMQ refer the setup and installation instructions available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker)
- After deployment of RabbitMQ, we need to ensure that there are certain prerequisites met. In 
  case if it is not met, please login to RabbitMQ management portal using the RabbitMQ 
  management UI and create the following

##### Create vHost

| Type  | Name          | Details                    |
|-------|---------------|----------------------------|
| vHost | IUDX-INTERNAL | Create a vHost in RabbitMQ |


##### Create Exchange

| Exchange Name | Type of exchange | features | Details                                                                              |
|---------------|------------------|----------|--------------------------------------------------------------------------------------|
| auditing      | direct           | durable  | Create an exchange in vHost IUDX-INTERNAL to allow audit information to be published |


##### Create Queue and Bind to Exchange
| Exchange Name | Queue Name | vHost   | routing key | Details                                                                                                                                   |
|---------------|------------|---------|-------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| auditing      | direct     | durable | #           | Create a queue in vHost IUDX-INTERNAL to allow audit information to be consumed. Ensure that the queue is binded to the auditing exchange |

##### User and Permissions
Create a DX Catalogue user using the RabbitMQ management UI and set write permission. This user 
will be used by DX Catalogue server to publish audit data

| API                         | Body           | Details                                                |
|-----------------------------|----------------|--------------------------------------------------------|
| /api/users/user/permissions | As shown below | Set permission for a user to publish audit information |


Body for the API request

```
 "permissions": [
        {
          "vhost": "IUDX-INTERNAL",
          "permission": {
            "configure": "^$",
            "write": "^auditing$",
            "read": "^$"
          }
        }
]
```

#### PostgresQL
- To setup PostgreSQL refer setup and installation instructions available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/postgres)
- **Note** : PostgreSQL database should be configured with a RBAC user having CRUD privileges

| Table Name               | Purpose                                                                                                                                             |
|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| auditing_table           | To store logged information about endpoint, caller of the endpoint, timestamp when the POST, DELETE, PUT requests respond with 200 success response |
| auditing_rs              | To store rating information of resources                                                                                                            |

#### Auditing
- Auditing is done using the DX Auditing Server which uses Immudb and Postgres for storing the audit logs
- To setup immuclient for immudb please refer [immudb setup guide](https://github.com/datakaveri/iudx-deployment/tree/master/docs/immudb-setup)
- The schema for auditing table in PostgreSQL is present here - [postgres auditing table schema](https://github.com/datakaveri/iudx-resource-server/blob/master/src/main/resources/db/migration/V5_2__create-auditing-acl-apd-table.sql)
- The schema for Immudb table, index for the table is present here - [immudb schema in DX Auditing Server](https://github.com/datakaveri/auditing-server/tree/main/src/main/resources/immudb/migration)

| Table Name     | Purpose                                                                                                                                             | DB                 |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|--------------------|
| auditing_table | To store logged information about endpoint, caller of the endpoint, timestamp when the POST, DELETE, PUT requests respond with 200 success response | Immudb, PostgreSQL |

- **Note** : User ID, endpoint and epoch time are indexed in Immudb to retrieve the logs faster

## Installation Steps
### Maven
1. Install java 11 and maven
2. Use the maven exec plugin based starter to start the server. Goto the root folder where the pom.xml file is present and run the below command.
   `mvn clean compile exec:java@catalogue-server`

### JAR
1. Install java 11 and maven
2. Set Environment variables
```
export CATALOGUE_URL=https://<catalogue-domain-name>
export LOG_LEVEL=INFO
```
3. Use maven to package the application as a JAR. Goto the root folder where the pom.xml file is present and run the below command.
   `mvn clean package -Dmaven.test.skip=true`
4. 2 JAR files would be generated in the `target/` directory
    - `iudx.catalogue.server-cluster-0.0.1-SNAPSHOT-fat.jar` - clustered vert.x containing 
      micrometer metrics
    - `iudx.catalogue.server-dev-0.0.1-SNAPSHOT-fat.jar` - non-clustered vert.x and does not 
      contain micrometer metrics

#### Running the clustered JAR
**Note**: The clustered JAR requires Zookeeper to be installed. Refer [here](https://zookeeper.apache.org/doc/current/zookeeperStarted.html) to learn more about how to set up Zookeeper. Additionally, the `zookeepers` key in the config being used needs to be updated with the IP address/domain of the system running Zookeeper.
The JAR requires 3 runtime arguments when running:

* --config/-c : path to the config file
* --host/-i : the hostname for clustering
* --modules/-m : comma separated list of module names to deploy

e.g. ```java -jar target/iudx.catalogue.server-cluster-0.0.1-SNAPSHOT-fat.jar --host $(hostname)
-c secrets/config.json -m iudx.catalogue.server.authenticator.AuthenticationVerticle, iudx.
catalogue.server.database.DatabaseVerticle,
iudx.catalogue.server.apiserver.ApiServerVerticle, iudx.catalogue.server.auditing.
AuditingVerticle, iudx.catalogue.server.rating.RatingVerticle, iudx.catalogue.server.databroker.DataBrokerVerticle```

Use the `--help/-h` argument for more information. You may additionally append an 
`CATALOGUE_JAVA_OPTS` environment
variable containing any Java options to pass to the application.

e.g.
```
$ export CATALOGUE_JAVA_OPTS="Xmx40496m"
$ java $CATALOGUE_JAVA_OPTS -jar target/iudx.catalogue.server-cluster-0.0.1-SNAPSHOT-fat.jar ...

```

#### Running the non-clustered JAR
The JAR requires 1 runtime argument when running

* --config/-c : path to the config file

e.g. `java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.
Log4j2LogDelegateFactory -jar target/iudx.catalogue.server-cluster-0.0.1-SNAPSHOT-fat.jar -c 
configs/config.json`

Use the `--help/-h` argument for more information. You may additionally append an `RS_JAVA_OPTS` environment variable containing any Java options to pass to the application.

e.g.
```
$ export CATALOGUE_JAVA_OPTS="-Xmx1024m"
$ java CATALOGUE_JAVA_OPTS -jar target/iudx.catalogue.server-cluster-0.0.1-SNAPSHOT-fat.jar ...
```

### Docker
1. Install docker and docker-compose
2. Clone this repo
3. Build the images
   ` ./docker/build.sh`
4. Modify the `docker-compose.yml` file to map the config file
5. Start the server in production (prod) or development (dev) mode using docker-compose
   ` docker-compose up prod `

## Logging and Monitoring
### Log4j 2
- For asynchronous logging, logging messages to the console in a specific format, Apache's log4j 2 is used
- For log formatting, adding appenders, adding custom logs, setting log levels, log4j2.xml could be updated : [link](https://github.com/datakaveri/iudx-catalogue-server/blob/master/src/main/resources/log4j2.xml)
- Please find the reference to log4j 2 : [here](https://logging.apache.org/log4j/2.x/manual/index.html)

### Micrometer
- Micrometer is used for observability of the application
- Reference link: [vertx-micrometer-metrics](https://vertx.io/docs/vertx-micrometer-metrics/java/)
- The metrics from micrometer is stored in Prometheus which can be used to alert, observe,
  take steps towards the current state of the application
- The data sent to Prometheus can then be visualised in Grafana
- Reference link: [vertx-prometheus-grafana](https://how-to.vertx.io/metrics-prometheus-grafana-howto/)
- DX Deployment repository references for [Prometheus](https://github.com/datakaveri/iudx-deployment/tree/master/K8s-deployment/K8s-cluster/addons/mon-stack/prometheus), [Loki](https://github.com/datakaveri/iudx-deployment/tree/master/K8s-deployment/K8s-cluster/addons/mon-stack/loki), [Grafana](https://github.com/datakaveri/iudx-deployment/tree/master/K8s-deployment/K8s-cluster/addons/mon-stack/grafana)

## Testing
### Unit Testing
1. Run the server through either docker, maven or redeployer
2. Run the unit tests and generate a surefire report
   `mvn clean test-compile surefire:test surefire-report:report`
3. Jacoco reports are stored in `./target/`

### Integration Testing

Integration tests are through Postman/Newman whose script can be found from [here](https://github.com/datakaveri/iudx-catalogue-server/tree/master/src/test/resources).
1. Install prerequisites
- [postman](https://www.postman.com/) + [newman](https://www.npmjs.com/package/newman)
- [newman reporter-htmlextra](https://www.npmjs.com/package/newman-reporter-htmlextra)
2. Example Postman environment can be found [here](https://github.com/datakaveri/iudx-catalogue-server/blob/master/src/test/resources/CAT.postman_environmentv5.5.0.json)
- Please find the README to setup postman environment file [here](https://github.com/datakaveri/iudx-catalogue-server/blob/master/src/test/resources/README.md)
3. Run the server through either docker, maven or redeployer
4. Run the integration tests and generate the newman report
   `newman run <postman-collection-path> -e <postman-environment> --insecure -r htmlextra --reporter-htmlextra-export .`
5. Command to store report in `target/newman`:  `newman run <postman-collection-path> -e <postman-environment> --insecure -r htmlextra --reporter-htmlextra-export ./target/newman/report.html`

### Performance Testing
- JMeter is for used performance testing, load testing of the application
- Please find the reference to JMeter : [here](https://jmeter.apache.org/usermanual/get-started.html)
- Command to generate HTML report at `target/jmeter`
```
rm -r -f target/jmeter && jmeter -n -t jmeter/<file-name>.jmx -l target/jmeter/sample-reports.csv -e -o target/jmeter/
```

### Security Testing
- For security testing, Zed Attack Proxy(ZAP) Scanning is done to discover security risks, vulnerabilities to help us address them
- A report is generated to show vulnerabilities as high risk, medium risk, low risk and false positive
- Please find the reference to ZAP : [here](https://www.zaproxy.org/getting-started/)