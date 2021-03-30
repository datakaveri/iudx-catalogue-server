properties([pipelineTriggers([githubPush()])])
pipeline {
  environment {
    devRegistry = 'dockerhub.iudx.io/jenkins/catalogue-dev'
    deplRegistry = 'dockerhub.iudx.io/jenkins/catalogue-depl'
    testRegistry = 'dockerhub.iudx.io/jenkins/catalogue-test'
    registryUri = 'https://dockerhub.iudx.io'
    registryCredential = 'docker-jenkins'
  }
  agent any
  stages {
    stage('Building images') {
      steps{
        script {
          devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
          deplImage = docker.build( deplRegistry, "-f ./docker/prod.dockerfile .")
          testImage = docker.build( testRegistry, "-f ./docker/test.dockerfile .")
        }
      }
    }
  stage('Run Unit Tests and CodeCoverage test'){
    steps{
      script{
        sh 'docker-compose up test'
      }
    }
  }
  stage('Capture Unit Test results'){
    steps{
      xunit (
        thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '20') ],
        tools: [ JUnit(pattern: 'target/surefire-reports/*Test.xml') ]
      )
    }
    post{
      failure{
        error "Test failure. Stopping pipeline execution!"
      }
    }
  }
  stage('Capture Code Coverage'){
    steps{
      jacoco classPattern: 'target/classes', execPattern: 'target/**.exec', sourcePattern: 'src/main/java'
    }
  }
    stage('Run Jmeter Performance Tests'){
      steps{
        script{
          sh 'docker-compose up -d perfTest'
          sh 'sleep 45'
          sh 'rm -rf Jmeter/Report ; mkdir -p Jmeter/Report ; /var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t Jmeter/CatalogueServer.jmx -l Jmeter/Report/JmeterTest.jtl -e -o Jmeter/Report'
	  sh 'docker-compose down --remove-orphans'
        }
      }
    }
    stage('Capture Jmeter report'){
      steps{
	perfReport filterRegex: '', sourceDataFiles: 'Jmeter/Report/*.jtl'
        //perfReport constraints: [absolute(escalationLevel: 'ERROR', meteredValue: 'AVERAGE', operator: 'NOT_GREATER', relatedPerfReport: 'JmeterTest.jtl', success: false, testCaseBlock: testCase('GeoTextAttribute&Filter Search'), value: 800)], filterRegex: '', modeEvaluation: true, modePerformancePerTestCase: true, sourceDataFiles: 'Jmeter/*.jtl'      
      }
    }
    stage('Push Image') {
      steps{
        script {
          docker.withRegistry( registryUri, registryCredential ) {
            devImage.push()
            deplImage.push()
          }
        }
      }
    }
  }
}
