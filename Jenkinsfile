// pipeline {
//   agent any
//   tools {
//     maven 'Maven 3.8.8'
//     jdk 'Temurin JDK 21'
//   }
//   stages {
//     stage('Checkout') {
//       steps {
//         git url: 'https://github.com/superthree3am/project3am', branch: 'dev'
//       }
//     }
//     stage('Inject Secret Properties') {
//       steps {
//         withCredentials([file(credentialsId: 'app-properties', variable: 'APP_PROPS')]) {
//           sh 'cp $APP_PROPS ./api/src/main/resources/application.properties'
//         }
//       }
//     }
//     stage('Build (Skip Test)') {
//       steps {
//         //ke dir backend
//         dir('api') { // ganti 'api' sesuai folder pom.xml jika perlu
//           sh "mvn clean verify -DskipTests"
//         }
//       }
//     }
//     stage('Static Code Analysis (SAST) via Sonar') {
//       steps {
//         dir('api') { // ganti 'api' sesuai folder pom.xml jika perlu
//           sh """
//            mvn clean verify sonar:sonar \
//            -Dsonar.projectKey=SAST-BACK-END \
//            -Dsonar.projectName='SAST BACK END' \
//            -Dsonar.host.url=http://sonarqube:9000 \
//            -Dsonar.token=sqp_bbdcd42519deffa89aa8d0c61b7365073b8630ca
//           """
//         }
//       }
//     }
//   }
//   post {
//     success {
//       echo "Pipeline berhasil 🚀"
//     }
//     failure {
//       echo "Pipeline gagal 💥"
//     }
//   }
// }


pipeline {
  agent any

  tools {
    jdk 'JDK 21'
  }

  environment {
    IMAGE_NAME = 'srinesia/api'
    IMAGE_TAG = 'v1'
    DOCKER_CREDENTIALS_ID = 'dockerhub-credentials'
  }

  stages {
    stage('Checkout') {
      steps {
        git url: 'https://github.com/superthree3am/api', branch: 'nesia'
      }
    }

    stage('Build (Skip Test)') {
      steps {
        sh '''
          chmod +x mvnw
          ./mvnw clean verify -DskipTests
        '''
      }
    }

    stage('Static Code Analysis (SAST) via Sonar') {
      steps {
        sh """
          ./mvnw clean verify -DskipTests sonar:sonar \
          -Dsonar.projectKey=coba \
          -Dsonar.projectName='coba' \
          -Dsonar.host.url=http://sonarqube:9000 \
          -Dsonar.token=kode
        """
      }
    }

    stage('Build Docker Image') {
      steps {
        script {
          sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
        }
      }
    }

    stage('Push to Docker Hub') {
      steps {
        script {
          withCredentials([usernamePassword(credentialsId: "${DOCKER_CREDENTIALS_ID}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
            sh '''
              echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
              docker push ${IMAGE_NAME}:${IMAGE_TAG}
            '''
          }
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
