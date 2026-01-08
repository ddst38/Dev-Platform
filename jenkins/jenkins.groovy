pipeline {
    agent any
    tools {
        jdk 'jdk21'
    }
    /*****************************************************************
     * PARAMÈTRES JENKINS (Interface utilisateur)
     *****************************************************************/
    parameters {
        string(
            name: 'PROJECT_NAME',
            defaultValue: 'PRF1_A',
            description: 'Nom du projet (nom du dépôt GitLab + dossier final)'
        )

        string(
            name: 'CVS_MODULE',
            defaultValue: 'PRF1_A',
            description: 'Chemin CVS du module (ex: SSA-CHISTO/R0_J)'
        )

        string(
            name: 'GITLAB_NAMESPACE',
            defaultValue: 'damsi/dgd/usine/migrations-projets',
            description: 'Namespace GitLab dans lequel créer le projet'
        )

        string(
            name: 'URL_REPORTUI',
            defaultValue: 'http://report-ui:8090',
            description: 'URL du service REST ReportUI'
        )

        booleanParam(
            name: 'AUTO_FIX',
            defaultValue: true,
            description: 'Active l’option --auto-fix dans ant2maven'
        )

        booleanParam(
            name: 'PUBLISH_REPORT',
            defaultValue: true,
            description: 'Publier le rapport de migration vers ReportUI'
        )

        booleanParam(
            name: 'PUSH_GITLAB',
            defaultValue: true,
            description: 'Créer le dépôt GitLab et pousser le projet'
        )
    }

    /*****************************************************************
     * VARIABLES D’ENVIRONNEMENT
     *****************************************************************/
    environment {
        // CVS
        CVS_ROOT = ':pserver:jenkins@cvs:/cvsroot'

        // Ant2Maven
        ANT2MAVEN_JAR = '/opt/tools/ant2maven-1.0.0-SNAPSHOT.jar'

        // Report UI
        REPORT_UI_UPLOAD = 'http://migration-reports/upload'

        // GitLab
        GITLAB_URL = 'http://gitlab:8083'
        GITLAB_API = 'http://gitlab:8083/api/v4'
        GITLAB_CREDENTIAL_ID = 'gitlab-token'
    }

    stages {

        /*************************************************************
         * CHECKOUT CVS
         *************************************************************/
         stage('Checkout CVS') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'cvs-jenkins',
                        usernameVariable: 'CVS_USER',
                        passwordVariable: 'CVS_PASS'
                    )
                ]) {
                    sh '''#!/bin/bash
                        set -e

                        echo "=== CVS LOGIN ==="
                        export CVS_PASSFILE=$(mktemp)
                        echo "${CVS_PASS}" | cvs -d ${CVS_ROOT} login

                        echo "=== CVS CHECKOUT ==="
                        rm -rf ${PROJECT_NAME} ${PROJECT_NAME}-maven

                        cvs -d ${CVS_ROOT} checkout -d ${PROJECT_NAME} ${CVS_MODULE}

                        echo "✔ CVS checkout terminé → ${PROJECT_NAME}"
                    '''
                }
            }
        }

        /*************************************************************
         * MIGRATION ANT → MAVEN
         *************************************************************/
        stage('Migration Ant to Maven') {
            steps {
                sh '''#!/bin/bash
                    set -e

                    echo "=== ANT → MAVEN ==="

                    AUTO_FIX_FLAG=""
                    if [ "${AUTO_FIX}" = "true" ]; then
                        AUTO_FIX_FLAG="--auto-fix"
                    fi

                    REPORTUI_FLAG=""
                    if [ "${PUBLISH_REPORT}" = "true" ]; then
                        REPORTUI_FLAG="--report-ui-url ${URL_REPORTUI}"
                    fi

                    java -jar ${ANT2MAVEN_JAR} \
                        -p ${PROJECT_NAME} \
                        -o ${PROJECT_NAME}-maven \
                        --lib-provided /opt/tools/lib-provided \
                        ${AUTO_FIX_FLAG} \
                        ${REPORTUI_FLAG} \
                        -v

                    echo "✔ Migration terminée"
                '''
            }
        }


        /*************************************************************
         * CRÉATION DU PROJET GITLAB
         *************************************************************/
        stage('Creation Projet GitLab') {
            when {
                expression { params.PUSH_GITLAB }
            }
            steps {
                withCredentials([
                    string(credentialsId: "${GITLAB_CREDENTIAL_ID}", variable: 'GITLAB_TOKEN')
                ]) {
                    sh '''#!/bin/bash
                        set -e

                        PROJECT_PATH="${GITLAB_NAMESPACE}/${PROJECT_NAME}"
                        ENCODED_PATH=$(echo "$PROJECT_PATH" | sed 's|/|%2F|g')
                        ENCODED_NAMESPACE=$(echo "$GITLAB_NAMESPACE" | sed 's|/|%2F|g')

                        echo "=== Vérification du dépôt GitLab ==="

                        STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
                          -H "PRIVATE-TOKEN: ${GITLAB_TOKEN}" \
                          ${GITLAB_API}/projects/${ENCODED_PATH})

                        CREATE_PROJECT=false

                        if [ "$STATUS" = "404" ]; then
                            CREATE_PROJECT=true
                        else
                            # Vérifier si le projet est en suppression programmée
                            MARKED_FOR_DELETION=$(curl -s \
                              -H "PRIVATE-TOKEN: ${GITLAB_TOKEN}" \
                              "${GITLAB_API}/projects/${ENCODED_PATH}" | grep -o '"marked_for_deletion_at":"[^"]*"' || echo "")

                            if [ -n "$MARKED_FOR_DELETION" ]; then
                                echo "⚠ Projet en suppression programmée, suppression définitive..."
                                curl -s -X DELETE \
                                  -H "PRIVATE-TOKEN: ${GITLAB_TOKEN}" \
                                  "${GITLAB_API}/projects/${ENCODED_PATH}?permanently_remove=true"
                                sleep 2
                                CREATE_PROJECT=true
                            else
                                echo "✔ Dépôt GitLab déjà existant"
                            fi
                        fi

                        if [ "$CREATE_PROJECT" = "true" ]; then
                            echo "➡ Récupération du namespace ${GITLAB_NAMESPACE}"

                            NAMESPACE_ID=$(curl -s \
                              -H "PRIVATE-TOKEN: ${GITLAB_TOKEN}" \
                              "${GITLAB_API}/groups/${ENCODED_NAMESPACE}" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

                            if [ -z "$NAMESPACE_ID" ]; then
                                echo "❌ Namespace introuvable : ${GITLAB_NAMESPACE}"
                                exit 1
                            fi

                            echo "➡ Création du dépôt GitLab ${PROJECT_NAME} dans namespace ${NAMESPACE_ID}"

                            curl -s -X POST ${GITLAB_API}/projects \
                              -H "PRIVATE-TOKEN: ${GITLAB_TOKEN}" \
                              -H "Content-Type: application/json" \
                              -d "{
                                \\"name\\": \\"${PROJECT_NAME}\\",
                                \\"path\\": \\"${PROJECT_NAME}\\",
                                \\"namespace_id\\": ${NAMESPACE_ID},
                                \\"visibility\\": \\"internal\\"
                              }"

                            echo "✔ Dépôt GitLab créé"
                        fi
                    '''
                }
            }
        }

        /*************************************************************
         * PUSH GITLAB
         *************************************************************/
        stage('Push vers GitLab') {
            when {
                expression { params.PUSH_GITLAB }
            }
            steps {
                withCredentials([
                    string(credentialsId: "${GITLAB_CREDENTIAL_ID}", variable: 'GITLAB_TOKEN')
                ]) {
                    sh '''#!/bin/bash
                        set -e

                        echo "=== PUSH GITLAB ==="
                        cd ${PROJECT_NAME}-maven

                        git init
                        git checkout -b migration
                        git config user.email "jenkins@dev-platform.local"
                        git config user.name "Jenkins Migration"

                        # Création du .gitignore pour exclure les fichiers non désirés
                        cat > .gitignore << 'EOF'
liblocale/
localMvnRepository/
.mvn/
mvnw
mvnw.cmd
report.tar.gz
EOF

                        git add .
                        git commit -m "Migration Initiale ${PROJECT_NAME} Ant2maven"

                        git remote add origin \
                          http://oauth2:${GITLAB_TOKEN}@gitlab:8083/${GITLAB_NAMESPACE}/${PROJECT_NAME}.git

                        git push -u origin migration --force

                        echo "✔ Projet poussé"
                        echo "➡ ${GITLAB_URL}/${GITLAB_NAMESPACE}/${PROJECT_NAME}"
                    '''
                }
            }
        }

        stage('Archive Artefact') {
            steps {
                archiveArtifacts artifacts: '**/migration-report.html', allowEmptyArchive: true
            }
        }
    }

    /*****************************************************************
     * POST ACTIONS
     *****************************************************************/
    post {
        always {
            cleanWs()
        }
    }
}