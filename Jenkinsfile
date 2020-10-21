properties([pipelineTriggers([githubPush()])])
pipeline {
  environment {
    registry = 'https://dockerhub.iudx.io'
    registryUri = 'https://dockerhub.iudx.io'
    registryCredential = 'docker-jenkins'
    imageName = 'iudx-dev'
    dockerImage = ''
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
