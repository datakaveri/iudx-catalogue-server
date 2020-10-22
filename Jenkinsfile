properties([pipelineTriggers([githubPush()])])
pipeline {
  environment {
    registry = 'dockerhub.iudx.io/jenkins/catalogue-dev'
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
    stage('Building image') {
      steps{
        script {
          dockerImage = docker.build( registry, "-f ./docker/depl.dockerfile .")
        }
      }
    }
    stage('run test') {
      steps{
        def out = sh label: '', returnStdout: true, script: 'docker run alpine echo success'
      }
    }
    stage('check test') {
      steps{
        script {
          if (out == 'success') {
            echo 'All tests passed, Success'
          } else {
            echo 'Failure'
          }
        }
      }
    }
    stage('Push Image') {
      steps{
        script {
          docker.withRegistry( registryUri, registryCredential ) {
            dockerImage.push()
          }
        }
      }
    }
    stage('Remove Unused docker image') {
      steps{
        sh "docker rmi dockerhub.iudx.io/jenkins/iudx-dev"
      }
    }
  }
}
