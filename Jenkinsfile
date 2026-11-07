@Library('my-shared-library') _

pipeline {
    agent any
    
    triggers {
        // trigger the pipeline on GitHub push events      
        githubPush()
    }
    
    environment {
        APP_NAME = "interview-application"
        ARTIFACT_NAME = "${APP_NAME}-${env.BUILD_NUMBER}.tar.gz"
        REPO_URL = "http://192.168.1.100:8081/repository/my-repo"
    }
    
    stages {
        stage('Prepare Environment') {
            steps {
                script {
                    // uses shared library 
                    prepareEnv()
                }
            }
        }
        
        stage('Build Artifact') {
            steps {
                echo "Building artifact for commit ${env.SCM_COMMIT_ID} triggered by ${env.SCM_COMMITTER}"
                
                sh 'echo "Simulating build process..."'
                sh 'mkdir -p build-output'
                sh 'echo "dummy binary data" > build-output/app.bin'
            }
        }
        
        stage('Pack Artifact') {
            steps {
                echo "Packing the built artifact to ${ARTIFACT_NAME}..."
                sh "tar -czvf ${ARTIFACT_NAME} ./build-output"
            }
        }
        
        stage('Upload to Repo') {
            steps {
                echo "Uploading ${ARTIFACT_NAME} to artifact repository..."
                withCredentials([usernamePassword(credentialsId: 'artifactory-creds', passwordVariable: 'REPO_PASS', usernameVariable: 'REPO_USER')]) {
                    sh """
                        curl -u ${REPO_USER}:${REPO_PASS} \
                             -X PUT \
                             -T ${ARTIFACT_NAME} \
                             ${REPO_URL}/${ARTIFACT_NAME}
                    """
                }
            }
        }
    }
    
    post {
        always {
            echo "Cleaning up workspace..."
            cleanWs() 
        }
        success {
            echo "Pipeline completed successfully!"
        }
    }
}