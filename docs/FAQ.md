<p align="center">
<img src="./cdpg.png" width="300">
</p>

# Frequently Asked Questions (FAQs)

1. How do I request for a new feature to be added or change in an existing feature?
- Please create an issue [here](https://github.com/datakaveri/iudx-catalogue-server/issues)
2. What is the purpose of the Catalogue Server?
- The Catalogue Server stores meta-information related to data resources available on the Data Exchange (DX) platform, enabling users to discover data sources through search capabilities. It allows data providers to manage metadata and provides APIs for storing, retrieving, and managing resource information.
3. How does the Catalogue Server handle search queries?
- The Catalogue Server offers various search options:
  - Attribute Search: Based on key-value pairs, such as tags. 
  - Geo-Spatial Search: For locating datasets within specific geographic boundaries (circle, polygon, etc.). 
  - Text Search: General keyword-based search. 
  - Complex Search: Combines attribute, geo-spatial, and text-based filters.
4. What is the multi-tenancy feature of the Catalogue Server?
- The multi-tenancy feature allows different entities (e.g., smart cities, organizations) to use the Catalogue Server while keeping their data isolated. It supports segregation based on entities, such as city or organization, ensuring proper data partitioning for different tenants.
5. What are the available APIs for interacting with the Catalogue Server?
- The primary APIs include:
  - **Search APIs:** /iudx/cat/v1/search, /iudx/cat/v1/count, /iudx/cat/v1/geo, 
    /iudx/cat/v1/nlpsearch
  - **CRUD APIs:** /iudx/cat/v1/item, /iudx/cat/v1/instance, /iudx/cat/v1/relationship
  - **Listing APIs:** /iudx/cat/v1/list/{type}, /iudx/cat/v1/internal/ui/providers
  - **Consumer Ratings API:** /iudx/cat/v1/consumer/ratings Refer to the API documentation for a 
    full list of endpoints and their descriptions.
6. How does the Catalogue Server ensure data security?
- The server uses TLS for secure communications between components and limits service exposure to internal networks. It also leverages an API gateway for authentication and authorization, rate limiting to prevent DDoS attacks, and certificate-based token issuance to control access.
7. How can I configure the Catalogue Server?
- Configuration options are available in the config-example.json file, which provides various 
  deployment settings for customizing the Catalogue Server setup according to specific 
  requirements. Please refer [Configurations](./Configurations.md)