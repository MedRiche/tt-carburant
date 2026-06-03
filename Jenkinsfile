pipeline {
    agent any

    tools {
        jdk     'JDK17'
        maven   'Maven3'
        nodejs  'NodeJS18'
    }

    environment {
        // Docker image names
        BACKEND_IMAGE  = "tt-carburant-backend"
        FRONTEND_IMAGE = "tt-carburant-frontend"
        IMAGE_TAG      = "${BUILD_NUMBER}"
    }

    stages {

        /* ══════════════════════════════════════════
           CI — ÉTAPE 1 : CLONE REPOSITORY
        ══════════════════════════════════════════ */
        stage('Clone Repository') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/MedRiche/tt-carburant.git'
            }
        }

        /* ══════════════════════════════════════════
           CI — ÉTAPE 2 : BACKEND BUILD
        ══════════════════════════════════════════ */
        stage('Backend Build') {
            steps {
                dir('backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
            post {
                success { echo '✅ Backend JAR built successfully' }
                failure { echo '❌ Backend build failed'           }
            }
        }

        /* ══════════════════════════════════════════
           CI — ÉTAPE 3 : FRONTEND BUILD
        ══════════════════════════════════════════ */
        stage('Frontend Install & Build') {
            steps {
                dir('demo') {
                    sh 'npm install --legacy-peer-deps'
                    sh 'npm run build:ci'
                }
            }
            post {
                success { echo '✅ Angular build successful' }
                failure { echo '❌ Angular build failed'     }
            }
        }

        /* ══════════════════════════════════════════
           CD — ÉTAPE 1 : BUILD DOCKER IMAGES
        ══════════════════════════════════════════ */
        stage('Docker Build Images') {
            steps {
                script {
                    echo '🐳 Building Docker images...'

                    // Build Backend image
                    sh """
                        docker build \
                          -t ${BACKEND_IMAGE}:${IMAGE_TAG} \
                          -t ${BACKEND_IMAGE}:latest \
                          ./backend
                    """

                    // Build Frontend image
                    sh """
                        docker build \
                          -t ${FRONTEND_IMAGE}:${IMAGE_TAG} \
                          -t ${FRONTEND_IMAGE}:latest \
                          ./demo
                    """
                }
            }
            post {
                success { echo '✅ Docker images built successfully' }
                failure { echo '❌ Docker build failed'              }
            }
        }

        /* ══════════════════════════════════════════
           CD — ÉTAPE 2 : DEPLOY WITH DOCKER COMPOSE
        ══════════════════════════════════════════ */
        stage('Deploy with Docker Compose') {
            steps {
                script {
                    echo '🚀 Deploying application...'

                    // Stop existing containers cleanly
                    sh 'docker compose down --remove-orphans || true'

                    // Pull + start all services in detached mode
                    sh 'docker compose up -d --build'

                    // Wait for services to be healthy
                    sh 'sleep 15'

                    // Show running containers
                    sh 'docker compose ps'
                }
            }
            post {
                success { echo '✅ Deployment successful — app is running' }
                failure { echo '❌ Deployment failed'                      }
            }
        }

        /* ══════════════════════════════════════════
           CD — ÉTAPE 3 : HEALTH CHECK
        ══════════════════════════════════════════ */
        stage('Health Check') {
            steps {
                script {
                    echo '🏥 Checking application health...'

                    // Wait a bit more for Spring Boot to fully start
                    sh 'sleep 20'

                    // Check backend health
                    sh '''
                        STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health || echo "000")
                        if [ "$STATUS" = "200" ]; then
                            echo "✅ Backend is UP (HTTP $STATUS)"
                        else
                            echo "⚠️  Backend status: HTTP $STATUS (may still be starting)"
                        fi
                    '''

                    // Check frontend health
                    sh '''
                        STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:4200 || echo "000")
                        if [ "$STATUS" = "200" ]; then
                            echo "✅ Frontend is UP (HTTP $STATUS)"
                        else
                            echo "⚠️  Frontend status: HTTP $STATUS"
                        fi
                    '''
                }
            }
        }

    }

    /* ══════════════════════════════════════════
       POST ACTIONS
    ══════════════════════════════════════════ */
    post {
        success {
            echo '''
            ╔══════════════════════════════════════════╗
            ║   ✅  TTCarburant CI/CD Pipeline SUCCESS  ║
            ║   🌐  Frontend : http://localhost:4200    ║
            ║   🔧  Backend  : http://localhost:8081    ║
            ╚══════════════════════════════════════════╝
            '''
        }
        failure {
            echo '❌ TTCarburant Pipeline FAILED — check logs above'
            // Optional: clean up failed containers
            sh 'docker compose down --remove-orphans || true'
        }
        always {
            // Clean up unused Docker images to save disk space
            sh 'docker image prune -f || true'
        }
    }
}