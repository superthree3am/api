pipeline {
    agent any

    tools {
        maven 'Maven 3.8.8'
        jdk 'JDK 21'
        nodejs 'Node 20.19.0'
    }

    environment {
        REGION                    = 'asia-southeast2'
        GCP_PROJECT_ID            = 'am-finalproject'
        GCP_CLUSTER_NAME          = 'finalproject-cluster'
        REPO_NAME                 = 'fathya-backend-repo'
        IMAGE_NAME                = 'be-app'
        IMAGE_TAG                 = 'latest'
        FULL_IMAGE_NAME           = "${REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/${REPO_NAME}/${IMAGE_NAME}:${IMAGE_TAG}"
        SONAR_QUBE_SERVER_URL     = 'https://sonar3am.42n.fun/'
        SONAR_QUBE_PROJECT_KEY    = 'be-app-sq-gke'
        SONAR_QUBE_PROJECT_NAME   = 'Project SonarQube Backend GKE'
    }


    stages {
        stage('Checkout') {
            steps {
                deleteDir()
                dir('backend') {
                    git branch: 'main', url: 'https://github.com/fathyafi/api-gke-main.git'
                }
                echo "Repository checked out successfully."
            }
        }

        stage('Build Aplikasi (Maven)') {
            steps {
                dir('backend') {
                    script {
                        withMaven(maven: 'Maven 3.8.8') {
                            sh 'mvn clean install -DskipTests'
                            echo "Application built successfully."
                        }
                    }
                }
            }
        }

        stage('Unit Test') {
            steps {
                dir('backend') {
                    script {
                        sh '''
                            chmod +x ./mvnw
                            mkdir -p src/main/resources
                        '''
                        withCredentials([file(credentialsId: 'app-properties-backend', variable: 'APP_PROPS')]) {
                            sh 'cp "$APP_PROPS" src/main/resources/application.properties'
                        }
                        sh './mvnw clean package -DskipTests'
                        withMaven(maven: 'Maven 3.8.8') {
                            sh './mvnw test jacoco:report'
                        }
                    }
                }
            }
        }

        stage('SAST with SonarQube') {
            steps {
                dir('backend') {
                    script {
                        withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                            sh """
                                mvn clean verify sonar:sonar \\
                                -Dsonar.projectKey=${SONAR_QUBE_PROJECT_KEY} \\
                                -Dsonar.projectName="${SONAR_QUBE_PROJECT_NAME}" \\
                                -Dsonar.host.url=${SONAR_QUBE_SERVER_URL} \\
                                -Dsonar.token=${SONAR_TOKEN} \\
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \\
                                -Dsonar.scanner.skipProjectMetadata=true \\
                                -Dsonar.exclusions="**/*Controller.java,**/*Request.java,**/*Response.java"
                            """
                            echo "SonarQube analysis completed."
                        }
                    }
                }
            }
        }

        stage('Build & Push Docker Image to Artifact Registry') {
            steps {
                dir('backend') {
                    withCredentials([file(credentialsId: 'gcp-service-account-key', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
                        script {
                            sh '''
                                gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS
                                gcloud config set project $GCP_PROJECT_ID
                                gcloud auth configure-docker $REGION-docker.pkg.dev --quiet

                                docker build -t $FULL_IMAGE_NAME .
                                docker push $FULL_IMAGE_NAME
                            '''
                            echo "✅ Image pushed to Artifact Registry: ${FULL_IMAGE_NAME}"
                        }
                    }
                }
            }
        }

        stage('Deploy to GKE') {
            steps {
                withCredentials([file(credentialsId: 'gcp-service-account-key', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
                    script {
                        sh '''
                            gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS
                            gcloud config set project $GCP_PROJECT_ID
                            gcloud container clusters get-credentials $GCP_CLUSTER_NAME --region $REGION

                            kubectl apply -f backend/k8s/redis.yml
                            kubectl apply -f backend/k8s/backend.yml
                            kubectl apply -f backend/k8s/hpa.yml
                            kubectl rollout restart deployment backend-app
                        '''
                        echo "🚀 Application deployed to GKE"
                    }
                }
            }   
        }
    }

    post {
        success {
            echo "✅ Pipeline finished successfully!"
        }
        failure {
            echo "❌ Pipeline failed! Check the logs for details."
        }
    }
}