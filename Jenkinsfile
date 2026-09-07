pipeline {
    agent any
    tools {
        jdk     'JDK17'
        maven   'Maven3'
        nodejs  'NodeJS18'
    }
    environment {
        BACKEND_IMAGE  = "tt-carburant-backend"
        FRONTEND_IMAGE = "tt-carburant-frontend"
        IMAGE_TAG      = "${BUILD_NUMBER}"
        SONAR_URL      = "http://host.docker.internal:9000"
    }
    stages {
        stage('Clone Repository') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/MedRiche/tt-carburant.git'
            }
        }
        stage('Backend Build') {
            steps {
                dir('backend') {
                    sh 'mvn clean verify'
                }
            }
            post {
                success { echo 'Backend JAR built and tests passed' }
                failure { echo 'Backend build or tests failed'      }
            }
        }
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    dir('backend') {
                        // On appelle le plugin par ses coordonnees completes
                        // (groupId:artifactId:version:goal) car le prefixe
                        // "sonar" seul n'est pas resolu par Maven sur cet agent
                        sh '''
                            mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.7.0.6970:sonar \
                              -Dsonar.projectKey=tt-carburant \
                              -Dsonar.projectName="TT Carburant" \
                              -Dsonar.java.binaries=target/classes
                        '''
                    }
                }
            }
            post {
                success { echo 'SonarQube analysis completed' }
                failure { echo 'SonarQube analysis failed'    }
            }
        }
        stage('Quality Gate') {
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
            }
            post {
                always { echo 'SonarQube Quality Gate stage completed' }
            }
        }
        stage('Frontend Install & Build') {
            steps {
                dir('demo') {
                    sh 'npm install --legacy-peer-deps'
                    sh 'npm run build:ci'
                }
            }
        }
        stage('Deploy with Docker Compose') {
            steps {
                script {
                    sh 'docker compose -p tt-carburant down --remove-orphans || true'
                    sh 'docker compose -p tt-carburant up -d --build'
                    sh 'sleep 15'
                    sh 'docker compose -p tt-carburant ps'
                }
            }
            post {
                success { echo 'Deployment successful' }
                failure { echo 'Deployment failed'     }
            }
        }
        stage('Health Check') {
            steps {
                script {
                    sh 'sleep 20'
                    sh '''
                        STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health || echo "000")
                        echo "Backend status: HTTP $STATUS"
                    '''
                }
            }
        }
    }
    post {
        success {
            echo 'TTCarburant Pipeline SUCCESS'
        }
        failure {
            echo 'TTCarburant Pipeline FAILED'
            sh 'docker compose -p tt-carburant down --remove-orphans || true'
        }
        always {
            sh 'docker image prune -f || true'
        }
    }
}