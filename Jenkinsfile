pipeline {
    agent any

    environment {
        DOCKER_USER = 'nihir14'
    }

    stages {
        stage('Phase 1: Backend Tests & Build') {
            steps {
                echo 'Running Spring Boot Microservices Tests...'
                dir('backend') {
                    sh 'mvn clean package' // Packages the JARs and runs your passing tests
                }
            }
        }

        stage('Phase 2: SonarQube Quality Scan') {
            steps {
                echo 'Sending code to SonarQube...'
                // This securely injects the sonar-token we saved in Jenkins
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                    dir('backend') {
                        sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=revpay-backend \
                          -Dsonar.host.url=http://sonarqube:9000 \
                          -Dsonar.login=${SONAR_TOKEN}
                        '''
                    }
                }
            }
        }

        stage('Phase 3: Frontend Tests (Angular)') {
            steps {
                echo 'Running Angular Headless Tests...'
                dir('frontend') {
                    sh 'npm install'
                    sh 'npx ng test --watch=false --browsers=ChromeHeadlessNoSandbox'
                }
            }
        }

        stage('Phase 4: Docker Hub Release') {
            steps {
                echo 'Building Docker Images and pushing to Docker Hub...'
                // This securely injects your Docker password
                withCredentials([string(credentialsId: 'docker-hub-credentials', variable: 'DOCKER_PASSWORD')]) {
                    sh 'docker login -u ${DOCKER_USER} -p ${DOCKER_PASSWORD}'

                    // Build & Push Gateway (Example - add the rest here later!)
                    dir('backend') {
                        sh 'docker build -t ${DOCKER_USER}/revpay-api-gateway:latest ./infrastructure/api-gateway'
                        sh 'docker push ${DOCKER_USER}/revpay-api-gateway:latest'

                        sh 'docker build -t ${DOCKER_USER}/revpay-wallet-service:latest ./microservices/wallet-service'
                        sh 'docker push ${DOCKER_USER}/revpay-wallet-service:latest'
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'PIPELINE SUCCESS! Code is tested, scanned, and published.'
        }
        failure {
            echo 'PIPELINE FAILED! Check the Jenkins logs.'
        }
    }
}