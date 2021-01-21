properties([pipelineTriggers([githubPush()])])
pipeline {
  environment {
    devRegistry = 'dockerhub.iudx.io/jenkins/catalogue-dev'
    prodRegistry = 'dockerhub.iudx.io/jenkins/catalogue-prod'
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
          prodImage = docker.build( prodRegistry, "-f ./docker/prod.dockerfile .")
          testImage = docker.build( testRegistry, "-f ./docker/test.dockerfile .")
        }
      }
    }
    stage('Run Tests'){
      steps{
        script{
          sh 'docker-compose up test'
        }
      }
    }
    stage('Capture Test results'){
      steps{
        xunit (
                thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
                tools: [ JUnit(pattern: 'target/surefire-reports/*Test.xml') ]
        )
      }
    }
    stage('Code Coverage'){
      steps{
        jacoco execPattern: 'target/**.exec', runAlways: true
      }
    }
    stage('Push Image') {
      steps{
        script {
          docker.withRegistry( registryUri, registryCredential ) {
            devImage.push()
            prodImage.push()
            testImage.push()
          }
        }
      }
    }
    // stage('Remove Unused docker image') {
    //  steps{
    //    sh "docker rmi dockerhub.iudx.io/jenkins/catalogue-dev"
    //    sh "docker rmi dockerhub.iudx.io/jenkins/catalogue-depl"
    //  }
    //}
  }
}
