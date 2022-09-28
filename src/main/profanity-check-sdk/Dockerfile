FROM python:3.8-slim
WORKDIR /usr/share/app
COPY . /usr/share/app/
COPY requirements.txt .

RUN apt-get update \
    && /usr/local/bin/python -m pip install --upgrade pip \
    && pip install --no-cache-dir  -r requirements.txt 

RUN useradd -r -u 1001 -g root profanity-user
USER profanity-user
CMD ["python","main.py"]
