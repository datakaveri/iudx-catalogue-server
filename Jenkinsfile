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
    // stage('Run Unit Tests and CodeCoverage test'){
    //   steps{
    //     script{
    //       sh 'docker-compose up test'
    //     }
    //   }
    // }
    // stage('Capture Unit Test results'){
    //   steps{
    //     xunit (
    //             thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '9') ],
    //             tools: [ JUnit(pattern: 'target/surefire-reports/*Test.xml') ]
    //     )
    //   }
    //   post{
    //     failure{
    //     error "Test failure. Stopping pipeline execution!"
    //     }
    //   }
    // }
    // stage('Capture Code Coverage'){
    //   steps{
    //     jacoco classPattern: 'target/classes', execPattern: 'target/**.exec', sourcePattern: 'src/main/java'
    //   }
    // }
    stage('Run Jmeter Performance Tests'){
      steps{
        script{
          //withDockerContainer(args: '-p 8443:8443', image: 'dockerhub.iudx.io/jenkins/catalogue-test') {
          //  sh 'nohup mvn clean compile test-compile exec:java@catalogue-server'
          //}
          //sh 'docker run -d -p 8443:8443 --name perfTest dockerhub.iudx.io/jenkins/catalogue-test'
          //sh 'docker exec -it perfTest sh -c "nohup mvn clean compile test-compile exec:java@catalogue-server"'
          sh 'mkdir -p Jmeter'
          sh 'nohup docker-compose up perfTest & /root/jmeter/apache-jmeter-5.4.1/bin/jmeter.sh -n -t iudx-catalogue-server_complex_search_count.jmx -l JmeterTest.jtl -e -o Jmeter'
        }
      }
    }
    // stage('Push Image') {
    //   steps{
    //     script {
    //       docker.withRegistry( registryUri, registryCredential ) {
    //         devImage.push()
    //         prodImage.push()
    //         testImage.push()
    //       }
    //     }
    //   }
    // }
    // stage('Remove Unused docker image') {
    //  steps{
    //    sh "docker rmi dockerhub.iudx.io/jenkins/catalogue-dev"
    //    sh "docker rmi dockerhub.iudx.io/jenkins/catalogue-depl"
    //  }
    //}
  }
}
