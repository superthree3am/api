pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'rolandmaulana/my-api'
        DOCKER_TAG = 'latest'
        OPENSHIFT_PROJECT = 'roland-app'
        OPENSHIFT_SERVER = 'https://api.threeam.finalproject.cloud:6443'
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

        stage('Deploy to OpenShift') {
            steps {
                withCredentials([string(credentialsId: 'oc-token', variable: 'TOKEN')]) {
                    sh '''
                        oc login $OPENSHIFT_SERVER --token=$TOKEN --insecure-skip-tls-verify
                        oc project $OPENSHIFT_PROJECT
                        oc set image deployment/springboot-app springboot-app=$DOCKER_IMAGE:$DOCKER_TAG
                        oc rollout restart deployment/springboot-app
                    '''
                }
            }
        }
    }
}