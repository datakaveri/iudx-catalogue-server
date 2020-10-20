pipeline {
  environment {
    registry = "karunsingh97/iudx-dev"
    registryCredential = 'DockerHub'
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
          docker.withRegistry( '', registryCredential ) {
            dockerImage.push()
          }
        }
      }
    }
    stage('Remove Unused docker image') {
      steps{
        sh "docker rmi test-image"
      }
    }
  }
}
