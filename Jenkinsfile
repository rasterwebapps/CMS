pipeline {
    agent any

    // ── Tool versions configured in Manage Jenkins → Tools ──────────────────
    tools {
        nodejs 'NodeJS-22'   // Name must match what you set in Jenkins NodeJS plugin
        // Gradle managed via wrapper (gradlew), no tool config needed
    }

    // ── Environment variables ────────────────────────────────────────────────
    environment {
        // Nexus
        NEXUS_URL             = 'http://<nexus-ip>:8081'                              // ← change
        NEXUS_RELEASES_REPO   = 'cms-releases'
        NEXUS_SNAPSHOTS_REPO  = 'cms-snapshots'
        NEXUS_DOCKER_REPO     = 'cms-docker'                                          // if using Nexus Docker registry
        NEXUS_CREDENTIALS     = credentials('nexus-credentials')                      // Jenkins credential ID

        // Docker image names (adjust registry prefix if needed)
        BACKEND_IMAGE         = "${NEXUS_URL}/${NEXUS_DOCKER_REPO}/cms-backend"
        FRONTEND_IMAGE        = "${NEXUS_URL}/${NEXUS_DOCKER_REPO}/cms-frontend"

        // App version derived from Git tag or branch+build
        APP_VERSION           = "${env.BRANCH_NAME == 'main' ? env.BUILD_NUMBER : env.BRANCH_NAME + '-SNAPSHOT'}"

        // Deployment target (adjust to your server)
        DEPLOY_HOST           = '<deploy-server-ip>'                                  // ← change
        DEPLOY_USER           = 'deploy'
        DEPLOY_SSH_KEY        = credentials('deploy-ssh-key')                         // add SSH key credential
    }

    // ── Build options ────────────────────────────────────────────────────────
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    // ── Triggers ─────────────────────────────────────────────────────────────
    triggers {
        githubPush()          // fires on push via webhook
    }

    stages {

        // ── 1. Checkout ──────────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
                echo "Building branch: ${env.BRANCH_NAME} | commit: ${env.GIT_COMMIT?.take(8)}"
            }
        }

        // ── 2. Backend: Build + Test ─────────────────────────────────────────
        stage('Backend — Build & Test') {
            steps {
                dir('backend') {
                    sh 'chmod +x gradlew'
                    // Build JAR + run tests + enforce 95% JaCoCo coverage
                    sh './gradlew clean check jacocoTestReport --no-daemon'
                }
            }
            post {
                always {
                    // Publish JUnit test results
                    junit 'backend/build/test-results/test/*.xml'
                    // Publish JaCoCo coverage report
                    publishHTML([
                        reportDir:   'backend/build/reports/jacoco/test/html',
                        reportFiles: 'index.html',
                        reportName:  'JaCoCo Coverage Report',
                        keepAll:     true
                    ])
                }
            }
        }

        // ── 3. Frontend: Install + Build ─────────────────────────────────────
        stage('Frontend — Build') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run build -- --configuration production'
                }
            }
        }

        // ── 4. Docker: Build images ──────────────────────────────────────────
        stage('Docker — Build Images') {
            steps {
                script {
                    // Backend image
                    docker.build("${BACKEND_IMAGE}:${APP_VERSION}", '-f backend/Dockerfile backend/')
                    docker.build("${BACKEND_IMAGE}:latest",         '-f backend/Dockerfile backend/')

                    // Frontend image
                    docker.build("${FRONTEND_IMAGE}:${APP_VERSION}", '-f frontend/Dockerfile frontend/')
                    docker.build("${FRONTEND_IMAGE}:latest",         '-f frontend/Dockerfile frontend/')
                }
            }
        }

        // ── 5. Nexus: Publish JAR via Gradle maven-publish plugin ───────────────
        // Gradle's maven-publish plugin publishes the bootJar to Nexus using
        // Maven repository layout (groupId/artifactId/version). This is standard
        // for Gradle → Nexus publishing — no Maven build tool is involved.
        stage('Nexus — Publish JAR') {
            steps {
                dir('backend') {
                    withEnv([
                        "NEXUS_USER=${env.NEXUS_CREDENTIALS_USR}",
                        "NEXUS_PASS=${env.NEXUS_CREDENTIALS_PSW}"
                    ]) {
                        sh './gradlew publish --no-daemon'
                    }
                }
            }
        }

        // ── 6. Nexus: Push Docker images ─────────────────────────────────────
        stage('Nexus — Push Docker Images') {
            steps {
                script {
                    // Login to Nexus Docker registry
                    sh "echo '${env.NEXUS_CREDENTIALS_PSW}' | docker login ${NEXUS_URL} -u ${env.NEXUS_CREDENTIALS_USR} --password-stdin"

                    // Push backend
                    docker.image("${BACKEND_IMAGE}:${APP_VERSION}").push()
                    docker.image("${BACKEND_IMAGE}:latest").push()

                    // Push frontend
                    docker.image("${FRONTEND_IMAGE}:${APP_VERSION}").push()
                    docker.image("${FRONTEND_IMAGE}:latest").push()
                }
            }
        }

        // ── 7. Deploy (main branch only) ─────────────────────────────────────
        stage('Deploy — Production') {
            when {
                branch 'main'
            }
            steps {
                script {
                    sshagent(['deploy-ssh-key']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} \\
                            "cd /opt/cms && \\
                             export NEXUS_URL=${NEXUS_URL} && \\
                             export APP_VERSION=${APP_VERSION} && \\
                             docker-compose pull && \\
                             docker-compose up -d --remove-orphans"
                        """
                    }
                }
            }
        }
    }

    // ── Post actions ─────────────────────────────────────────────────────────
    post {
        success {
            echo "✅ Build ${env.BUILD_NUMBER} succeeded — version: ${APP_VERSION}"
            // Uncomment to enable Slack/email notifications:
            // slackSend channel: '#deployments', message: "✅ CMS ${APP_VERSION} deployed"
        }
        failure {
            echo "❌ Build ${env.BUILD_NUMBER} FAILED"
            // emailext to: 'team@yourorg.com', subject: "BUILD FAILED: ${env.JOB_NAME}"
        }
        always {
            // Clean up dangling Docker images on the Jenkins agent
            sh 'docker image prune -f || true'
            cleanWs()
        }
    }
}

