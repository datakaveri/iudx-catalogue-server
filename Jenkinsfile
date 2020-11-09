properties([pipelineTriggers([githubPush()])])
pipeline {
  environment {
    devRegistry = 'dockerhub.iudx.io/jenkins/catalogue-dev'
    deplRegistry = 'dockerhub.iudx.io/jenkins/catalogue-depl'
    testRegistry = 'dockerhub.iudx.io/jenkins/catalogue-test'
    registryUri = 'https://dockerhub.iudx.io'
    registryCredential = 'docker-jenkins'
    imageName = 'iudx-dev'
  }
  agent any
  stages {
    stage('Cloning Git') {
      steps {
        git 'https://github.com/karun-singh/iudx-catalogue-server-1.git'
      }
    }
    stage('Building images') {
      steps{
        script {
          devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
          deplImage = docker.build( deplRegistry, "-f ./docker/depl.dockerfile .")
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
                tools: [ JUnit(pattern: 'target/surefire-reports/*SummarizerTest.xml') ]
        )
      }
    }
    stage('Code Coverage'){
      steps{
        jacoco(execPattern: 'target/jacoco.exec')
      }
    }
    stage('Push Image') {
      steps{
        script {
          docker.withRegistry( registryUri, registryCredential ) {
            devImage.push()
            deplImage.push()
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
