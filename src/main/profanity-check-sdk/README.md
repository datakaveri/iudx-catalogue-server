# IUDX Profanity Check SDK

The IUDX Profanity Check SDK ensures that a comment/review associated with a user rating doesn't contain any foul language or indecent words.
It provides a flow to auto accept or reject comments by using a machine learning model called `cuss-inspect`

![Architecture](../../../docs/sdk-architecture.png)

### Installing Python
* For running the IUDX Profanity Check SDK, Python is required which can be installed by following [this](https://cloud.google.com/python/docs/setup#installing_python) guide.

### Setting Environment
* After successfully installing python make sure that the environment contains path to your project if not then it should be done by running the following command in the command prompt :
 ```commandline
# For Linux
$ export PYTHONPATH="${PYTHONPATH}:/path/to/your/project/"
# For Windows
$ set PYTHONPATH=%PYTHONPATH%;C:\path\to\your\project\
# For MacOS
$ PYTHONPATH="/path/to/your/project/:$PYTHONPATH"
$ export PYTHONPATH
```
* Define the appropriate config options in file named ``config.json``. Please see the 
  [example file](example-config.json) to get idea.
* Now the project is ready to run, it can be done by running the following command in the command prompt :

### Running Project
```commandline
$ python3 main.py
