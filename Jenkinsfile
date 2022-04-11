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
          sh 'docker-compose up test'
        }
        xunit (
          thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
          tools: [ JUnit(pattern: 'target/surefire-reports/TEST-iudx.catalogue.server.apiserver*.xml') ]
        )
        jacoco classPattern: 'target/classes', execPattern: 'target/DatabaseServiceTest.exec,target/jacoco2.exec', sourcePattern: 'src/main/java', exclusionPattern: 'iudx/catalogue/server/apiserver/*,iudx/catalogue/server/deploy/*,iudx/catalogue/server/mockauthenticator/*,iudx/catalogue/server/apiserver/util/*,iudx/catalogue/server/**/*EBProxy.*,iudx/catalogue/server/**/*ProxyHandler.*,iudx/catalogue/server/**/reactivex/*,iudx/catalogue/server/**/reactivex/*'
      }
      post{
        failure{
          script{
            sh 'docker-compose down --remove-orphans'
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
            sh 'scp src/test/resources/iudx-catalogue-server-v3.5.postman_collection_test.json jenkins@jenkins-master:/var/lib/jenkins/iudx/cat/Newman/'
            sh 'docker-compose up -d perfTest'
            sh 'sleep 45'
        }
      }
    }
    
    stage('Run Jmeter Performance Tests'){
      steps{
        node('master') {      
          script{
            sh 'rm -rf /var/lib/jenkins/iudx/cat/Jmeter/Report ; mkdir -p /var/lib/jenkins/iudx/cat/Jmeter/Report ; /var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t /var/lib/jenkins/iudx/cat/Jmeter/CatalogueServer.jmx -l /var/lib/jenkins/iudx/cat/Jmeter/Report/JmeterTest.jtl -e -o /var/lib/jenkins/iudx/cat/Jmeter/Report'
          }
          perfReport filterRegex: '', showTrendGraphs: true, sourceDataFiles: '/var/lib/jenkins/iudx/cat/Jmeter/Report/*.jtl'
        }
      }
      post{
        failure{
          script{
            sh 'docker-compose down --remove-orphans'
          }
          error "Test failure. Stopping pipeline execution!"
        }
      }
    }
    
    stage('Integration Tests and OWASP ZAP pen test'){
      steps{
        node('master') {
          script{
            startZap ([host: 'localhost', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0'])
            sh 'HTTP_PROXY=\'127.0.0.1:8090\' newman run /var/lib/jenkins/iudx/cat/Newman/iudx-catalogue-server-v3.5.postman_collection_test.json -e /home/ubuntu/configs/cat-postman-env.json --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/cat/Newman/report/report.html --reporter-htmlextra-skipSensitiveData'
            runZapAttack()
          }
        }
      }
      post{
        always{
          node('master') {
            script{
               archiveZap failHighAlerts: 1, failMediumAlerts: 1, failLowAlerts: 1
               publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: '/var/lib/jenkins/iudx/cat/Newman/report/', reportFiles: 'report.html', reportName: 'Integration Test Report', reportTitles: ''])
            }  
          }
        }
        cleanup{
          script{
            sh 'docker-compose down --remove-orphans'
          } 
        }
      }
    }

    stage('Push Image') {
      when{
        expression {
          return env.GIT_BRANCH == 'origin/v3.5.0';
        }
      }
      steps{
        script {
          docker.withRegistry( registryUri, registryCredential ) {
            devImage.push("3.5.0-${env.GIT_HASH}")
            deplImage.push("3.5.0-${env.GIT_HASH}")
          }
        }
      }
    }
  }
}