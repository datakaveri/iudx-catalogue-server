![IUDX](./docs/iudx.png)
# iudx-catalogue-server
The catalogue is [IUDXs](https://iudx.org.in) data discovery and dataset metadata publishing portal.
It allows data providers to publish their data *resources* by making an IUDX vocabulary annotated meta-data document describing their datasource and affiliated terminologies.
The datasources publish their data to the IUDX *Resource Server*.
It allows consumers of such data to easily discover such *resources* by means of powerful
queries and consume the data from *Resource Servers* in an automated and machine interpretable way.

<p align="center">
<img src="./docs/cat_overview.png">
</p>


## Features
- Search and discovery of data resources hosted on IUDX platform
- Support for text, geo-spatial, relationship, list and attributes searches
- Upload, delete and modify operations on catalogue objects (meta-information corresponding to resources)
- Stores meta-information as JSON-LD documents using published vocabulary and attributes
- Scalable, service mesh architecture based implementation using open source components: Vert.X API framework, Elasticsearch for data-base
- Hazelcast and Zookeeper based cluster management and service discovery


## Live 
The live running instance of the IUDX catalogue can be found [here](https://catalogue.iudx.org.in).

## API Docs 
The api docs can be found [here](https://catalogue.iudx.org.in/apis).



## Get Started

### Prerequisite - Make configuration
Make a config file based on the template in `./configs/config-example.json` 
- Generate a certificate using Lets Encrypt or other methods
- Make a Java Keystore File and mention its path and password in the appropriate sections
- Modify the database url and associated credentials in the appropriate sections

### Docker based
1. Install docker and docker-compose
2. Clone this repo
3. Build the images 
   ` ./docker/build.sh`
4. Modify the `docker-compose.yml` file to map the config file you just created
5. Start the server in production (prod) or development (dev) mode using docker-compose 
   ` docker-compose up prod `


### Maven based
1. Install java 13 and maven
2. Use the maven exec plugin based starter to start the server 
   `mvn clean compile exec:java@catalogue-server`

### Redeployer
A hot-swappable redeployer is provided for quick development 
`./redeploy.sh`


## Contributing
We follow Git Merge based workflow 
1. Fork this repo
2. Create a new feature branch in your fork. Multiple features must have a hyphen separated name, or refer to a milestone name as mentioned in Github -> Projects  
4. Commit to your fork and raise a Pull Request with upstream


## License
[MIT](./LICENSE.txt)
