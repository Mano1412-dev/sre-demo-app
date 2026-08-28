pipeline {

    agent any

    environment {

        // ====================================================
        // Azure
        // ====================================================

        AZURE_SUBSCRIPTION_ID = 'aeaca9a3-889f-4e38-8d9b-34e9ec3e36aa'

        // IMPORTANT:
        // Store this in Jenkins credentials/environment.
        // Do not hard-code the tenant/client secret here.
        AZURE_TENANT_ID = credentials('azure-tenant-id')


        // ====================================================
        // ACR
        // ====================================================

        ACR_NAME = 'sreplatformacr2026'

        ACR_LOGIN_SERVER = 'sreplatformacr2026.azurecr.io'

        IMAGE_NAME = 'test-project'


        // ====================================================
        // AKS
        // ====================================================

        AKS_RESOURCE_GROUP = 'rg-sre-prod-lab'

        AKS_CLUSTER_NAME = 'aks-sre-prod'


        // ====================================================
        // Helm
        // ====================================================

        HELM_RELEASE_NAME = 'nammabazaar'

        HELM_NAMESPACE = 'nammabazaar'

        HELM_CHART_PATH = 'infrastructure/helm/nammabazaar'


        // ====================================================
        // Docker image tag
        // Jenkins build number = image tag
        // Example: Jenkins build #13 -> image :13
        // ====================================================

        IMAGE_TAG = "${BUILD_NUMBER}"


        // ====================================================
        // Application repository
        // ====================================================

        APP_REPO = 'git@github.com:Mano1412-dev/sre-demo-app.git'

        APP_BRANCH = 'main'


        // ====================================================
        // Infrastructure repository
        // ====================================================

        INFRA_REPO = 'git@github.com:Mano1412-dev/sre-azure-platform.git'

        INFRA_BRANCH = 'main'
    }


    stages {


        // ====================================================
        // 1. CHECKOUT APPLICATION
        // ====================================================

        stage('Checkout Application') {

            steps {

                echo '=========================================='
                echo 'Checking out application source'
                echo '=========================================='

                dir('application') {

                    deleteDir()

                    git credentialsId: 'github-ssh',
                        url: "${APP_REPO}",
                        branch: "${APP_BRANCH}"
                }
            }
        }


        // ====================================================
        // 2. CHECKOUT INFRASTRUCTURE
        // ====================================================

        stage('Checkout Infrastructure') {

            steps {

                echo '=========================================='
                echo 'Checking out infrastructure repository'
                echo '=========================================='

                dir('infrastructure') {

                    deleteDir()

                    git credentialsId: 'github-ssh',
                        url: "${INFRA_REPO}",
                        branch: "${INFRA_BRANCH}"
                }
            }
        }


        // ====================================================
        // 3. TOOL VERSIONS
        // ====================================================

        stage('Tool Versions') {

            steps {

                sh '''
                    set -e

                    echo "=========================================="
                    echo "Tool Versions"
                    echo "=========================================="

                    echo ""
                    echo "Git:"
                    git --version

                    echo ""
                    echo "Java:"
                    java -version

                    echo ""
                    echo "Maven:"
                    mvn -version

                    echo ""
                    echo "Docker:"
                    docker --version

                    echo ""
                    echo "Azure CLI:"
                    az version

                    echo ""
                    echo "kubectl:"
                    kubectl version --client

                    echo ""
                    echo "Helm:"
                    helm version
                '''
            }
        }


        // ====================================================
        // 4. BUILD APPLICATION
        // ====================================================

        stage('Build Application') {

            steps {

                dir('application') {

                    sh '''
                        set -e

                        echo "=========================================="
                        echo "Building Application"
                        echo "=========================================="

                        mvn clean package -DskipTests

                        echo ""
                        echo "Generated JAR:"
                        ls -lh target/*.jar
                    '''
                }
            }
        }


        // ====================================================
        // 5. AZURE AUTHENTICATION
        // ====================================================

        stage('Azure Authentication') {

            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: 'azure-sp-jenkins',
                        usernameVariable: 'ARM_CLIENT_ID',
                        passwordVariable: 'ARM_CLIENT_SECRET'
                    )
                ]) {

                    sh '''
                        set -e

                        echo "=========================================="
                        echo "Azure Authentication"
                        echo "=========================================="

                        az login \
                          --service-principal \
                          --username "$ARM_CLIENT_ID" \
                          --password "$ARM_CLIENT_SECRET" \
                          --tenant "$AZURE_TENANT_ID" \
                          --output none

                        az account set \
                          --subscription "$AZURE_SUBSCRIPTION_ID"

                        echo ""
                        echo "Azure authentication successful."

                        az account show \
                          --query "{subscription:id,tenant:tenantId}" \
                          -o table
                    '''
                }
            }
        }


        // ====================================================
        // 6. LOGIN TO ACR
        // ====================================================

        stage('ACR Login') {

            steps {

                sh '''
                    set -e

                    echo "=========================================="
                    echo "Logging into Azure Container Registry"
                    echo "=========================================="

                    az acr login \
                      --name "$ACR_NAME"

                    echo ""
                    echo "ACR login successful."
                '''
            }
        }


        // ====================================================
        // 7. BUILD DOCKER IMAGE
        // ====================================================

        stage('Build Docker Image') {

            steps {

                dir('application') {

                    sh '''
                        set -e

                        echo "=========================================="
                        echo "Building Docker Image"
                        echo "=========================================="

                        echo "Image:"
                        echo "$ACR_LOGIN_SERVER/$IMAGE_NAME:$IMAGE_TAG"

                        docker build \
                          -t "$ACR_LOGIN_SERVER/$IMAGE_NAME:$IMAGE_TAG" \
                          .

                        echo ""
                        echo "Docker image created successfully."

                        docker images \
                          "$ACR_LOGIN_SERVER/$IMAGE_NAME"
                    '''
                }
            }
        }


        // ====================================================
        // 8. PUSH IMAGE TO ACR
        // ====================================================

        stage('Push Image to ACR') {

            steps {

                sh '''
                    set -e

                    echo "=========================================="
                    echo "Pushing Docker Image to ACR"
                    echo "=========================================="

                    docker push \
                      "$ACR_LOGIN_SERVER/$IMAGE_NAME:$IMAGE_TAG"

                    echo ""
                    echo "Docker image pushed successfully."

                    az acr repository show \
                      --name "$ACR_NAME" \
                      --image "$IMAGE_NAME:$IMAGE_TAG" \
                      --output table
                '''
            }
        }


        // ====================================================
        // 9. AKS AUTHENTICATION
        // ====================================================

        stage('AKS Authentication') {

            steps {

                sh '''
                    set -e

                    echo "=========================================="
                    echo "Connecting to AKS"
                    echo "=========================================="

                    az aks get-credentials \
                      --resource-group "$AKS_RESOURCE_GROUP" \
                      --name "$AKS_CLUSTER_NAME" \
                      --overwrite-existing

                    echo ""
                    echo "AKS credentials obtained."

                    echo ""
                    echo "Current context:"
                    kubectl config current-context

                    echo ""
                    echo "AKS nodes:"
                    kubectl get nodes
                '''
            }
        }


        // ====================================================
        // 10. HELM LINT
        // ====================================================

        stage('Helm Lint') {

            steps {

                dir('infrastructure') {

                    sh '''
                        set -e

                        echo "=========================================="
                        echo "Helm Lint"
                        echo "=========================================="

                        helm lint "$HELM_CHART_PATH"

                        echo ""
                        echo "Helm chart validation successful."
                    '''
                }
            }
        }


        // ====================================================
        // 11. HELM TEMPLATE VALIDATION
        // ====================================================

        stage('Helm Template Validation') {

            steps {

                dir('infrastructure') {

                    sh '''
                        set -e

                        echo "=========================================="
                        echo "Helm Template Validation"
                        echo "=========================================="

                        helm template "$HELM_RELEASE_NAME" \
                          "$HELM_CHART_PATH" \
                          --namespace "$HELM_NAMESPACE" \
                          --set image.repository="$ACR_LOGIN_SERVER/$IMAGE_NAME" \
                          --set image.tag="$IMAGE_TAG" \
                          > /tmp/nammabazaar-rendered.yaml

                        echo ""
                        echo "Helm template generated successfully."

                        echo ""
                        echo "Image configured in rendered manifest:"
                        grep -n "image:" /tmp/nammabazaar-rendered.yaml || true
                    '''
                }
            }
        }


        // ====================================================
        // 12. HELM DEPLOYMENT
        // ====================================================

        stage('Helm Deploy') {

            steps {

                dir('infrastructure') {

                    sh '''
                        set -e

                        echo "=========================================="
                        echo "Deploying NammaBazaar"
                        echo "=========================================="

                        echo ""
                        echo "Release:"
                        echo "$HELM_RELEASE_NAME"

                        echo ""
                        echo "Namespace:"
                        echo "$HELM_NAMESPACE"

                        echo ""
                        echo "Image:"
                        echo "$ACR_LOGIN_SERVER/$IMAGE_NAME:$IMAGE_TAG"

                        helm upgrade --install \
                          "$HELM_RELEASE_NAME" \
                          "$HELM_CHART_PATH" \
                          --namespace "$HELM_NAMESPACE" \
                          --create-namespace \
                          --set image.repository="$ACR_LOGIN_SERVER/$IMAGE_NAME" \
                          --set image.tag="$IMAGE_TAG" \
                          --wait \
                          --timeout 10m

                        echo ""
                        echo "Helm deployment completed successfully."
                    '''
                }
            }
        }


        // ====================================================
        // 13. VERIFY DEPLOYMENT
        // ====================================================

        stage('Verify Deployment') {

            steps {

                sh '''
                    set -e

                    echo "=========================================="
                    echo "Kubernetes Deployment Status"
                    echo "=========================================="

                    echo ""
                    echo "Pods:"
                    kubectl get pods \
                      -n "$HELM_NAMESPACE" \
                      -o wide

                    echo ""
                    echo "Services:"
                    kubectl get svc \
                      -n "$HELM_NAMESPACE"

                    echo ""
                    echo "Deployments:"
                    kubectl get deployment \
                      -n "$HELM_NAMESPACE"

                    echo ""
                    echo "StatefulSets:"
                    kubectl get statefulset \
                      -n "$HELM_NAMESPACE"

                    echo ""
                    echo "Helm Releases:"
                    helm list \
                      -n "$HELM_NAMESPACE"

                    echo ""
                    echo "Deployment verification completed."
                '''
            }
        }
    }


    // ========================================================
    // POST ACTIONS
    // ========================================================

    post {

        success {

            echo '''
            ==========================================
            APPLICATION DEPLOYMENT SUCCESSFUL
            ==========================================
            '''
        }

        failure {

            echo '''
            ==========================================
            APPLICATION DEPLOYMENT FAILED
            ==========================================
            '''
        }

        always {

            sh '''
                echo "Cleaning Docker image from Jenkins agent..."

                docker rmi \
                  "$ACR_LOGIN_SERVER/$IMAGE_NAME:$IMAGE_TAG" \
                  2>/dev/null || true
            '''
        }
    }
}
