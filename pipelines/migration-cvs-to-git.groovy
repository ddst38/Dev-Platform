pipeline {
    agent any

    environment {
        CVS_ROOT = ':pserver:jenkins@cvs:/cvsroot'
        CVS_MODULE = 'SSA-CHISTO/R0_J'
        PROJECT_NAME = 'R0_J'
        ANT2MAVEN_JAR = '/opt/tools/ant2maven-1.0.0-SNAPSHOT.jar'
        GITLAB_URL = 'http://gitlab:8083'
        GITLAB_REPO = 'damsi/dgd/usine/migrations-projets'
        GITLAB_CREDENTIAL_ID = 'gitlab-token'
    }

    stages {
        stage('Checkout CVS') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'cvs-jenkins', usernameVariable: 'CVS_USER', passwordVariable: 'CVS_PASS')]) {
                    sh '''#!/bin/bash
                        echo "=== Login CVS ==="
                        export CVS_PASSFILE=$(mktemp)
                        echo "${CVS_PASS}" | cvs -d :pserver:${CVS_USER}@cvs:/cvsroot login

                        echo "=== Checkout depuis CVS ==="
                        rm -rf ${PROJECT_NAME} ${PROJECT_NAME}-maven SSA-CHISTO
                        cvs -d :pserver:${CVS_USER}@cvs:/cvsroot checkout ${CVS_MODULE}
                        mv SSA-CHISTO/R0_J ${PROJECT_NAME} || mv ${CVS_MODULE} ${PROJECT_NAME} || true
                        rm -rf SSA-CHISTO
                    '''
                }
            }
        }

        stage('Migration Ant to Maven') {
            steps {
                sh '''
                    echo "=== Migration avec ant2maven ==="
                    java -jar ${ANT2MAVEN_JAR} \
                        -p ${PROJECT_NAME} \
                        -o ${PROJECT_NAME}-maven \
                        --auto-fix \
                        -v
                '''
            }
        }

        stage('Push to GitLab') {
            steps {
                withCredentials([string(credentialsId: "${GITLAB_CREDENTIAL_ID}", variable: 'GITLAB_TOKEN')]) {
                    sh '''
                        echo "=== Push vers GitLab ==="
                        cd ${PROJECT_NAME}-maven

                        BRANCH_NAME="migration/${PROJECT_NAME}-$(date +%Y%m%d-%H%M%S)"

                        git init
                        git config user.email "jenkins@dev-platform.local"
                        git config user.name "Jenkins Migration"

                        git add .
                        git commit -m "Migration ${PROJECT_NAME} de CVS vers Maven"

                        git remote add origin http://oauth2:${GITLAB_TOKEN}@gitlab:8083/${GITLAB_REPO}.git
                        git checkout -b ${BRANCH_NAME}
                        git push -u origin ${BRANCH_NAME}

                        echo "=== Migration terminee ==="
                        echo "Branche: ${BRANCH_NAME}"
                        echo "URL: ${GITLAB_URL}/${GITLAB_REPO}/-/tree/${BRANCH_NAME}"
                    '''
                }
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: '**/migration-report.html', allowEmptyArchive: true
        }
        always {
            cleanWs()
        }
    }
}
