### Making configurable base path
- Base path can be added in postman environment file or in postman.
- `iudx-catalogue-server-v4.0.postman-env.json` has **values** array which has a field named **base** whose **value** is currently set to `iudx/cat/v1`, **dxAuthBasePath** with value `auth/v1`.
- The **value** could be changed according to the deployment and then the collection with the `iudx-catalogue-server-v4.0.postman-env.json` file can be uploaded to Postman
- For the changing the **base**, **dxAuthBasePath** value in postman after importing the collection and environment files, locate `CAT Environment` from **Environments** in sidebar of Postman application.
- To know more about Postman environments, refer : [postman environments](https://learning.postman.com/docs/sending-requests/managing-environments/)
- The **CURRENT VALUE** of the variable could be changed


