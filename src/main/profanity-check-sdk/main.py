import json
import time
from datetime import datetime
from RabbitMq import RabbitMq

import schedule

with open("config.json") as file:
    config = json.load(file)

class Main:
    
    def run(self):
        rmq = RabbitMq(config)
        rmq.consume_messages()

#schedule.every(config["schedule_time"]).seconds.do(lambda: Main().run())
Main().run()
#while True:
#    schedule.run_pending()
#    time.sleep(5)
