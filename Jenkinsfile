pipeline {
  agent any
  tools {
    maven 'Maven 3.8.8'
    jdk 'Temurin JDK 17'
  }
  stages {
    stage('Checkout') {
      steps {
        git url: 'https://github.com/superthree3am/project3am', branch: 'dev'
      }
    }
    stage('Inject Secret Properties') {
      steps {
        withCredentials([file(credentialsId: 'app-properties', variable: 'APP_PROPS')]) {
          sh 'cp $APP_PROPS ./api/src/main/resources/application.properties'
        }
      }
    }
    stage('Build (Skip Test)') {
      steps {
        //ke dir backend
        dir('api') { // ganti 'api' sesuai folder pom.xml jika perlu
          sh "mvn clean verify -DskipTests"
        }
      }
    }
    stage('Static Code Analysis (SAST) via Sonar') {
      steps {
        dir('api') { // ganti 'api' sesuai folder pom.xml jika perlu
          sh """
           mvn clean verify sonar:sonar \
           -Dsonar.projectKey=SAST-BACK-END \
           -Dsonar.projectName='SAST BACK END' \
           -Dsonar.host.url=http://sonarqube:9000 \
           -Dsonar.token=sqp_bbdcd42519deffa89aa8d0c61b7365073b8630ca
          """
        }
      }
    }
  }
  post {
    success {
      echo "Pipeline berhasil 🚀"
    }
    failure {
      echo "Pipeline gagal 💥"
    }
  }
}
