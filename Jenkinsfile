pipeline {
  agent any

  tools {
    nodejs 'Node 20.19.0'
    jdk 'JDK 21'
  }

  environment {
    REGISTRY_URL      = 'docker.io'
    FRONTEND_IMAGE    = 'titoalexsta/frontend:latest'
    BACKEND_IMAGE     = 'titoalexsta/backend:latest'
    OPENSHIFT_PROJECT = 'srinesia28-dev'
  }

  stages {
    stage('Checkout') {
      steps {
        deleteDir()
        dir('frontend') {
          git branch: 'deploy', url: 'https://github.com/superthree3am/web'
        }
        dir('backend') {
          git branch: 'deploysatu', url: 'https://github.com/superthree3am/api'
        }
      }
    }

    stage('Test Frontend') {
      steps {
        dir('frontend') {
          withCredentials([file(credentialsId: 'env-frontend', variable: 'env_file')]) {
            sh '''
              echo "env_file is: $env_file"
              ls -l "$env_file"
              cat "$env_file"
              cp "$env_file" .env
              npm install
              yarn test
              rm .env
            '''
          }
        }
      }
    }

    stage('Build Frontend') {
      steps {
        dir('frontend') {
          sh '''
            npm run build
            docker build --no-cache -t $FRONTEND_IMAGE .
          '''
        }
      }
    }

    stage('Test Backend') {
      steps {
        dir('backend') {
          sh '''
            chmod +x mvnw
            mkdir -p src/main/resources
          '''
          withCredentials([file(credentialsId: 'app-properties-backend', variable: 'APP_PROPS')]) {
            sh 'cp "$APP_PROPS" src/main/resources/application.properties'
          }
          sh './mvnw clean package -DskipTests'
          sh './mvnw test'
        }
      }
    }

    stage('Build Backend') {
      steps {
        dir('backend') {
          sh 'docker build --no-cache -t $BACKEND_IMAGE .'
        }
      }
    }

    stage('Push Images') {
      steps {
        withCredentials([usernamePassword(
          credentialsId: 'dockerhub-creds',
          usernameVariable: 'DOCKER_USER',
          passwordVariable: 'DOCKER_PASS'
        )]) {
          sh '''
            echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin $REGISTRY_URL
            docker push $FRONTEND_IMAGE
            docker push $BACKEND_IMAGE
          '''
        }
      }
    }

    stage('Deploy to OpenShift') {
      steps {
        withCredentials([string(
          credentialsId: 'openshift-token',
          variable: 'OC_TOKEN'
        )]) {
          sh '''
            oc login --token=$OC_TOKEN --server=https://api.rm1.0a51.p1.openshiftapps.com:6443
            oc project $OPENSHIFT_PROJECT
          '''

          dir('backend/openshift') {
            sh '''
              oc apply -f deployment.yml || true
              oc apply -f service.yml || true
              oc apply -f route.yml || true
            '''
          }

          dir('frontend/OpenShift') {
            sh '''
              oc apply -f deployment.yml || true
              oc apply -f service.yml || true
              oc apply -f route.yml || true
            '''
          }
        }
      }
    }
  }
}
