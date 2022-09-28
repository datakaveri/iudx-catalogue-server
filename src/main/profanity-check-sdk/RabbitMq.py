import pika, sys, json
from ProfanityCheck import ProfanityCheck

class RabbitMq:

    def __init__(self, config: json):
        
        print('Making a connection to rmq ...')
        credentials = pika.PlainCredentials(config["dataBrokerUserName"], config["dataBrokerPassword"])
        self.connection = pika.BlockingConnection(pika.ConnectionParameters(host=config["dataBrokerIP"],port=config["dataBrokerPort"],virtual_host=config["dataBrokerVhost"],credentials=credentials,heartbeat=config["requestedHeartBeat"]))
        self.channel = self.connection.channel()
        self.queue_name = config["queue-name"]
        self.index_name = config["index-name"]
        self.config = config
        print('connection established to rmq')

    def callback(self,ch, method, properties, body):
        payload = json.loads(body)
        Comment = payload.get('comment')
        Id = payload.get('ratingID')
        print(Id)
        print(Comment)
        pc = ProfanityCheck(self.config,Id,Comment)
        pc.profanity_check()

    def consume_messages(self):
        status=self.channel.queue_declare(self.queue_name,passive=True)
        if status.method.message_count == 0:
            print("empty queue")
        else:
            self.channel.basic_consume(queue=self.queue_name, on_message_callback=self.callback, auto_ack=True)
            self.channel.start_consuming()
        self.connection.close()

