properties([pipelineTriggers([githubPush()])])
pipeline {
  environment {
    devRegistry = 'dockerhub.iudx.io/jenkins/catalogue-dev'
    deplRegistry = 'dockerhub.iudx.io/jenkins/catalogue-depl'
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
    stage('Building dev image') {
      steps{
        script {
          devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
        }
      }
    }
    stage('Building depl image') {
      steps{
        script {
          deplImage = docker.build( deplRegistry, "-f ./docker/depl.dockerfile .")
        }
      }
    }
    stage('run test') {
      steps{
        script{
          def out = sh(returnStdout: true, script: 'docker run alpine echo success')
          echo out
          if (out.trim().equals('success')) {
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
            devImage.push()
            deplImage.push()
          }
        }
      }
    }
    stage('Deploy containers'){
      steps{
        script{
          sh 'docker-compose up dev' 
        }
      }
    }
    // stage('Remove Unused docker image') {
    //  steps{
    //    sh "docker rmi dockerhub.iudx.io/jenkins/catalogue-dev"
    //    sh "docker rmi dockerhub.iudx.io/jenkins/catalogue-depl"
    //  }
    //}
    node('label'){
    // now you are on slave labeled with 'label'
    def workspace = WORKSPACE
    // ${workspace} will now contain an absolute path to job workspace on slave

    workspace = env.WORKSPACE
    // ${workspace} will still contain an absolute path to job workspace on slave

    // When using a GString at least later Jenkins versions could only handle the env.WORKSPACE variant:
    echo "Current workspace is ${env.WORKSPACE}"

    // the current Jenkins instances will support the short syntax, too:
    echo "Current workspace is $WORKSPACE"
    }
  }
}
