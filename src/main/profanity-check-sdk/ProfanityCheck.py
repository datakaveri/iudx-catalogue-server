from cuss_inspect import predict, predict_prob
from UpdateDatabase import UpdateDatabase

import json

class ProfanityCheck:

    def __init__(self, config: json, id: str, comment: str):
        self.config = config 
        self.id = id
        self.comment = comment

        return

    def profanity_check(self):

        modeloutput = predict(self.comment)
        if modeloutput==1:
            status='Denied'
        else:
            status = 'Approved'
        print("verification result:",status)
        es = UpdateDatabase(self.config,self.id,status)
        es.update()
