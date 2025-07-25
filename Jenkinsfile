pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'rolandmaulana/my-api'
        DOCKER_TAG = 'v1'
        GCP_PROJECT = 'am-finalproject'
        GKE_CLUSTER = 'finalproject-cluster'
        GKE_REGION = 'asia-southeast2' 
        K8S_NAMESPACE = 'default'
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

        stage('Deploy to GKE') {
            steps {
                withCredentials([file(credentialsId: 'gcp-service-account-key', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
                    sh """
                        # Authenticate gcloud with service account
                        gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS

                        # Set project and get credentials
                        gcloud config set project $GCP_PROJECT
                        gcloud container clusters get-credentials $GKE_CLUSTER --zone $GKE_ZONE --project $GCP_PROJECT

                        # Replace image in Kubernetes manifest if needed
                        sed -i 's|image: .*|image: $DOCKER_IMAGE:$DOCKER_TAG|' k8s/deployment.yaml

                        # Apply manifests
                        kubectl apply -n $K8S_NAMESPACE -f k8s/deployment.yaml
                        kubectl apply -n $K8S_NAMESPACE -f k8s/service.yaml
                    """
                }
            }
        }
    }
}
