pipeline {
  environment {
    devRegistry = 'ghcr.io/datakaveri/cat-dev'
    deplRegistry = 'ghcr.io/datakaveri/cat-prod'
    testRegistry = 'ghcr.io/datakaveri/cat-test:latest'
    registryUri = 'https://ghcr.io'
    registryCredential = 'datakaveri-ghcr'
    GIT_HASH = GIT_COMMIT.take(7)
  }
  agent { 
    node {
      label 'slave1' 
    }
  }
  stages {

    stage('Building images') {
      steps{
        script {
          echo 'Pulled - ' + env.GIT_BRANCH
          devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
          deplImage = docker.build( deplRegistry, "-f ./docker/prod.dockerfile .")
          testImage = docker.build( testRegistry, "-f ./docker/test.dockerfile .")
        }
      }
    }

    stage('Unit Tests and CodeCoverage Test'){
      steps{
        script{
          sh 'docker compose up test'
        }
        xunit (
          thresholds: [ skipped(failureThreshold: '6'), failed(failureThreshold: '0') ],
          tools: [ JUnit(pattern: 'target/surefire-reports/*.xml') ]
        )
        jacoco classPattern: 'target/classes', execPattern: 'target/*.exec', sourcePattern: 'src/main/java', exclusionPattern: 'iudx/catalogue/server/apiserver/*,iudx/catalogue/server/deploy/*,iudx/catalogue/server/mockauthenticator/*,iudx/catalogue/server/**/*EBProxy.*,iudx/catalogue/server/**/*ProxyHandler.*,iudx/catalogue/server/**/reactivex/*,**/constants.class,**/*Verticle.class'
      }
      post{
        failure{
          script{
            sh 'docker compose down --remove-orphans'
          }
          error "Test failure. Stopping pipeline execution!"
        }
        cleanup{
          script{
            sh 'sudo rm -rf target/'
          }
        }        
      }
    }

    stage('Start Cat-Server for Performance and Integration Testing'){
      steps{
        script{
            sh 'scp Jmeter/CatalogueServer.jmx jenkins@jenkins-master:/var/lib/jenkins/iudx/cat/Jmeter/'
            sh 'scp src/test/resources/iudx-catalogue-server-v4.5.0.postman_collection.json jenkins@jenkins-master:/var/lib/jenkins/iudx/cat/Newman/'
            sh 'docker compose up -d perfTest'
            sh 'sleep 45'
        }
      }
      post{
        failure{
          script{
            sh 'docker compose down --remove-orphans'
          }
        }
      }
    }
    
    stage('Run Jmeter Performance Tests'){
      steps{
        node('built-in') {      
          script{
            sh 'rm -rf /var/lib/jenkins/iudx/cat/Jmeter/Report ; mkdir -p /var/lib/jenkins/iudx/cat/Jmeter/Report ; /var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t /var/lib/jenkins/iudx/cat/Jmeter/CatalogueServer.jmx -l /var/lib/jenkins/iudx/cat/Jmeter/Report/JmeterTest.jtl -e -o /var/lib/jenkins/iudx/cat/Jmeter/Report'
          }
          perfReport filterRegex: '', showTrendGraphs: true, sourceDataFiles: '/var/lib/jenkins/iudx/cat/Jmeter/Report/*.jtl'
        }
      }
      post{
        failure{
          script{
            sh 'docker compose down --remove-orphans'
          }
          error "Test failure. Stopping pipeline execution!"
        }
      }
    }
    
    stage('Integration Tests and OWASP ZAP pen test'){
      steps{
        node('built-in') {
          script{
            startZap ([host: 'localhost', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0'])
            sh 'curl http://127.0.0.1:8090/JSON/pscan/action/disableScanners/?ids=10096'
            sh 'HTTP_PROXY=\'127.0.0.1:8090\' newman run /var/lib/jenkins/iudx/cat/Newman/iudx-catalogue-server-v4.5.0.postman_collection.json -e /home/ubuntu/configs/cat-postman-env.json -n 2 --delay-request 1000 --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/cat/Newman/report/report.html --reporter-htmlextra-skipSensitiveData'
            runZapAttack()
          }
        }
      }
      post{
        always{
          node('built-in') {
            script{
               publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: '/var/lib/jenkins/iudx/cat/Newman/report/', reportFiles: 'report.html', reportName: 'Integration Test Report', reportTitles: ''])
               archiveZap failHighAlerts: 1, failMediumAlerts: 1, failLowAlerts: 1
            }  
          }
        }
        cleanup{
          script{
            sh 'docker compose down --remove-orphans'
          } 
        }
      }
    }

    stage('Continuous Deployment') {
      when {
        allOf {
          anyOf {
            changeset "docker/**"
            changeset "docs/**"
            changeset "pom.xml"
            changeset "src/main/**"
            triggeredBy cause: 'UserIdCause'
          }
          expression {
            return env.GIT_BRANCH == 'origin/master';
          }
        }
      }
      stages {
        stage('Push Images') {
          steps {
            script {
              docker.withRegistry( registryUri, registryCredential ) {
                devImage.push("5.0.0-alpha-${env.GIT_HASH}")
                deplImage.push("5.0.0-alpha-${env.GIT_HASH}")
              }
            }
          }
        }
        stage('Docker Swarm deployment') {
          steps {
            script {
              sh "ssh azureuser@docker-swarm 'docker service update cat_cat --image ghcr.io/datakaveri/cat-prod:5.0.0-alpha-${env.GIT_HASH}'"
              sh 'sleep 10'
            }
          }
          post{
            failure{
              error "Failed to deploy image in Docker Swarm"
            }
          }
        }
        stage('Integration test on swarm deployment') {
          steps {
            node('built-in') {
              script{
                sh 'newman run /var/lib/jenkins/iudx/cat/Newman/iudx-catalogue-server-v4.5.0.postman_collection.json -e /home/ubuntu/configs/cd/cat-postman-env.json --delay-request 1000 --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/cat/Newman/report/cd-report.html --reporter-htmlextra-skipSensitiveData'
              }
            }
          }
          post{
            always{
              node('built-in') {
                script{
                  publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: '/var/lib/jenkins/iudx/cat/Newman/report/', reportFiles: 'cd-report.html', reportTitles: '', reportName: 'Docker-Swarm Integration Test Report'])
                }
              }
            }
            failure{
              error "Test failure. Stopping pipeline execution!"
            }
          }
        }
      }
    }
  }
}
