pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'rolandmaulana/my-api'
        DOCKER_TAG = 'v1'
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/superthree3am/api.git', branch: 'roland-deploy'
            }
        }

        stage('Build JAR') {
            steps {
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t $DOCKER_IMAGE:$DOCKER_TAG ."
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    sh """
                        echo $PASSWORD | docker login -u $USERNAME --password-stdin
                        docker push $DOCKER_IMAGE:$DOCKER_TAG
                    """
                }
            }
        }
    }
}