import json
import os
import time
from datetime import datetime
from RabbitMq import RabbitMq

import schedule

cur_dir = os.path.dirname(os.path.realpath(__file__))
fileName = cur_dir + "/config.json"
with open(fileName) as file:
    config = json.load(file)

def main():
    rmq = RabbitMq(config)
    rmq.consume_messages()

if __name__=="__main__":
    schedule.every(config["schedule_time"]).seconds.do(lambda: main())
    while True:
        schedule.run_pending()
        time.sleep(5)