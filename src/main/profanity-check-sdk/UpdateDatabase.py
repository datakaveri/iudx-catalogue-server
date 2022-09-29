import json
from elasticsearch_dsl import Search, Q, UpdateByQuery
from elasticsearch import Elasticsearch, RequestsHttpConnection

class UpdateDatabase:

    def __init__(self,config: json, id: str, status: str):
        
        self.id = id
        self.status = status
        self.config = config
        #connecting to ES cluster   
        
        return

    def update(self):
        config = self.config
        client = Elasticsearch([config["databaseIP"]], port=config["databasePort"],connection_class=RequestsHttpConnection, http_auth=(config["databaseUser"],config["databasePassword"]),use_ssl=False,verify_certs=False)
        index_name = config["index_name"]
        search = Search(index=index_name).using(client)
        query = Q('bool',must=[Q('match', ratingID=self.id), Q('match', status='pending')])
        print(query)
        search = search.query(query)
        response = search.execute()
        for hit in response:
            doc_ = client.get(index=index_name, id=hit.meta.id)
            print(doc_['_source'])
            doc={
                "doc":{
                    "status":self.status
                }
            }
            client.update(index=index_name,id=hit.meta.id, body=doc)
            update_doc = client.get(index=index_name, id=hit.meta.id)
            print(update_doc['_source'])
        print("Database Updated")
        
        return
