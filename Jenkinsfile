//"Jenkins Pipeline is a suite of plugins which supports implementing and integrating continuous delivery pipelines into Jenkins. Pipeline provides an extensible set of tools for modeling delivery pipelines "as code" via the Pipeline DSL."
//More information can be found on the Jenkins Documentation page https://jenkins.io/doc/
library 'github-utils-shared-library@master'

@Library('github.com/connexta/cx-pipeline-library@master') _

pipeline {
    agent { label 'linux-small' }
    options {
        buildDiscarder(logRotator(numToKeepStr:'25'))
        disableConcurrentBuilds()
        timestamps()
    }
    triggers {
        /*
          Restrict nightly builds to master branch, all others will be built on change only.
          Note: The BRANCH_NAME will only work with a multi-branch job using the github-branch-source
        */
        cron(BRANCH_NAME == "master" ? "H H(17-19) * * *" : "")
    }
    environment {
        DISABLE_DOWNLOAD_PROGRESS_OPTS = '-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn '
        LINUX_MVN_RANDOM = '-Djava.security.egd=file:/dev/./urandom'
        COVERAGE_EXCLUSIONS = '**/test/**/*,**/itests/**/*,**/*Test*'
        SONAR_PROJECT_KEY = 'acdebugger'
        GITHUB_USERNAME = 'codice'
        GITHUB_REPONAME = 'acdebugger'
    }
    stages {
        stage('Setup') {
            steps {
                dockerd {}
                slackSend color: 'good', message: "STARTED: ${JOB_NAME} ${BUILD_NUMBER} ${BUILD_URL}"
                withCredentials([usernameColonPassword(credentialsId: 'cxbot', variable: 'GITHUB_TOKEN')]) {
                    postCommentIfPR("Internal build has been started. Your results will be available at completion. See build progress in [legacy Jenkins UI](${BUILD_URL}) or in [Blue Ocean UI](${BUILD_URL}display/redirect).", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${GITHUB_TOKEN}")
                    script {
                        //Check if the current HEAD is a Merge commit https://stackoverflow.com/questions/3824050/telling-if-a-git-commit-is-a-merge-revert-commit/3824122#3824122
                        try  {
                            sh(script: 'if [ `git cat-file -p HEAD | head -n 3 | grep parent | wc -l` -gt 1 ]; then exit 1; else exit 0; fi')
                            //No error was thrown -> we called exit 0 -> HEAD is not a merge commit/doesn't have multiple parents
                            env.PR_COMMIT = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                        } catch (err) {
                            //An error was thrown -> we called exit 1 -> HEAD is a merge commit/has multiple parents
                            env.PR_COMMIT = sh(returnStdout: true, script: 'git rev-parse HEAD~1').trim()
                        }
                        //Clear existing status checks
                        def jsonBlob = getGithubStatusJsonBlob("pending", "${BUILD_URL}display/redirect", "Linux Build In Progress...", "jenkins/build/linux")
                        postStatusToHash("${jsonBlob}", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${env.PR_COMMIT}", "${GITHUB_TOKEN}")
                        // The post/comment steps only work during linux builds at the moment, will add this back in after the step is platform independent
                        //jsonBlob = getGithubStatusJsonBlob("pending", "${BUILD_URL}display/redirect", "Windows Build In Progress...", "jenkins/build/windows")
                        //postStatusToHash("${jsonBlob}", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${env.PR_COMMIT}", "${GITHUB_TOKEN}")
                        jsonBlob = getGithubStatusJsonBlob("pending", "${BUILD_URL}display/redirect", "Sonar In Progress...", "jenkins/static-analysis/sonar")
                        postStatusToHash("${jsonBlob}", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${env.PR_COMMIT}", "${GITHUB_TOKEN}")
                        jsonBlob = getGithubStatusJsonBlob("pending", "${BUILD_URL}display/redirect", "OWASP In Progress...", "jenkins/static-analysis/owasp")
                        postStatusToHash("${jsonBlob}", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${env.PR_COMMIT}", "${GITHUB_TOKEN}")
                    }
                }
            }
        }
        // The incremental build will be triggered only for PRs. It will build the differences between the PR and the target branch
        stage('Incremental Build') {
            when {
                allOf {
                    expression { env.CHANGE_ID != null }
                    expression { env.CHANGE_TARGET != null }
                }
            }
            parallel {
                stage ('Linux') {
                    steps {
                        withMaven(maven: 'maven-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LINUX_MVN_RANDOM}') {
                              sh 'mvn install -B -DskipStatic=true -DskipTests=true $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                              sh 'mvn clean install -B -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/$CHANGE_TARGET $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                        }
                    }
                    post {
                        success {
                            withCredentials([usernameColonPassword(credentialsId: 'cxbot', variable: 'GITHUB_TOKEN')]) {
                                script {
                                    def jsonBlob = getGithubStatusJsonBlob("success", "${BUILD_URL}display/redirect", "Linux Build Succeeded!", "jenkins/build/linux")
                                    postStatusToHash("${jsonBlob}", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${env.PR_COMMIT}", "${GITHUB_TOKEN}")
                                }
                            }
                        }
                        failure {
                            catchError{ junit '**/target/surefire-reports/*.xml' }
                            catchError{ junit '**/target/failsafe-reports/*.xml' }
                            catchError{ zip zipFile: 'PaxExamRuntimeFolder.zip', archive: true, glob: '**/target/exam/**/*' }
                            withCredentials([usernameColonPassword(credentialsId: 'cxbot', variable: 'GITHUB_TOKEN')]) {
                                script {
                                    def jsonBlob = getGithubStatusJsonBlob("failure", "${BUILD_URL}display/redirect", "Linux Build Failed!", "jenkins/build/linux")
                                    postStatusToHash("${jsonBlob}", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${env.PR_COMMIT}", "${GITHUB_TOKEN}")
                                }
                            }
                        }
                    }
                }
                // stage ('Windows') {
                //     agent { label 'server-2016-small' }
                //     steps {
                //         withMaven(maven: 'maven-latest', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings') {
                //               bat 'mvn install -B -DskipStatic=true -DskipTests=true %DISABLE_DOWNLOAD_PROGRESS_OPTS%'
                //               bat 'mvn clean install -B -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/%CHANGE_TARGET% %DISABLE_DOWNLOAD_PROGRESS_OPTS%'
                //         }
                //     }
                // }
            }
        }
        stage('Full Build') {
            when { expression { env.CHANGE_ID == null } }
            parallel {
                stage ('Linux') {
                    steps {
                        withMaven(maven: 'maven-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LINUX_MVN_RANDOM}') {
                              sh 'mvn clean install -B $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                        }
                    }
                    post {
                        success {
                            withCredentials([usernameColonPassword(credentialsId: 'cxbot', variable: 'GITHUB_TOKEN')]) {
                                script {
                                    def jsonBlob = getGithubStatusJsonBlob("success", "${BUILD_URL}display/redirect", "Linux Build Succeeded!", "jenkins/build/linux")
                                    postStatusToHash("${jsonBlob}", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${env.PR_COMMIT}", "${GITHUB_TOKEN}")
                                }
                            }
                        }
                        failure {
                            catchError{ junit '**/target/surefire-reports/*.xml' }
                            catchError{ junit '**/target/failsafe-reports/*.xml' }
                            catchError{ zip zipFile: 'PaxExamRuntimeFolder.zip', archive: true, glob: '**/target/exam/**/*' }
                            withCredentials([usernameColonPassword(credentialsId: 'cxbot', variable: 'GITHUB_TOKEN')]) {
                                script {
                                    def jsonBlob = getGithubStatusJsonBlob("failure", "${BUILD_URL}display/redirect", "Linux Build Failed!", "jenkins/build/linux")
                                    postStatusToHash("${jsonBlob}", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${env.PR_COMMIT}", "${GITHUB_TOKEN}")
                                }
                            }
                        }
                    }
                }
                // stage ('Windows') {
                //     agent { label 'server-2016-small'}
                //     steps {
                //         withMaven(maven: 'maven-latest', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings') {
                //               bat 'mvn clean install -B %DISABLE_DOWNLOAD_PROGRESS_OPTS%'
                //         }
                //     }
                // }
            }
        }
        /*
          Deploy stage will only be executed for deployable branches. These include master and any patch branch matching M.m.x format (i.e. 2.10.x, 2.9.x, etc...).
          It will also only deploy in the presence of an environment variable JENKINS_ENV = 'prod'. This can be passed in globally from the jenkins master node settings.
        */
        stage('Deploy') {
            when {
                allOf {
                    expression { env.CHANGE_ID == null }
                    expression { env.BRANCH_NAME ==~ /((?:\d*\.)?\d*\.x|master)/ }
                    environment name: 'JENKINS_ENV', value: 'prod'
                }
            }
            steps{
                withMaven(maven: 'maven-latest', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LINUX_MVN_RANDOM}') {
                    sh 'mvn deploy -B -DskipStatic=true -DskipTests=true -DretryFailedDeploymentCount=10 $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                }
            }
        }
        stage('Quality Analysis') {
            parallel {
                stage ('Owasp') {
                    steps {
                        withMaven(maven: 'maven-latest', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LINUX_MVN_RANDOM}') {
                            script {
                                // If this build is not a pull request, run full owasp scan. Otherwise run incremental scan
                                if (env.CHANGE_ID == null) {
                                    sh 'mvn install -q -B -Powasp -DskipTests=true -DskipStatic=true $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                                } else {
                                    sh 'mvn install -q -B -Powasp -DskipTests=true -DskipStatic=true -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/$CHANGE_TARGET $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                                }
                            }
                        }
                    }
                    post {
                        success {
                            script {
                                withCredentials([usernameColonPassword(credentialsId: 'cxbot', variable: 'GITHUB_TOKEN')]) {
                                    def jsonBlob = getGithubStatusJsonBlob("success", "${BUILD_URL}display/redirect", "OWASP Succeeded!", "jenkins/static-analysis/owasp")
                                    postStatusToHash("${jsonBlob}", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${env.PR_COMMIT}", "${GITHUB_TOKEN}")
                                }
                            }
                        }
                        failure {
                            script {
                                withCredentials([usernameColonPassword(credentialsId: 'cxbot', variable: 'GITHUB_TOKEN')]) {
                                    def jsonBlob = getGithubStatusJsonBlob("failure", "${BUILD_URL}display/redirect", "OWASP Failed!", "jenkins/static-analysis/owasp")
                                    postStatusToHash("${jsonBlob}", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${env.PR_COMMIT}", "${GITHUB_TOKEN}")
                                }
                            }
                        }
                    }
                }
                stage ('SonarCloud') {
                    steps {
                        withMaven(maven: 'maven-latest', jdk: 'jdk11', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LINUX_MVN_RANDOM}') {
                            withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                                script {
                                    // If this build is not a pull request, run sonar scan. otherwise run incremental scan
                                    if (env.CHANGE_ID == null) {
                                        sh 'mvn -q -B -Dcheckstyle.skip=true org.jacoco:jacoco-maven-plugin:prepare-agent sonar:sonar -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=$SONAR_TOKEN  -Dsonar.organization=codice -Dsonar.projectKey=${SONAR_PROJECT_KEY} -Dsonar.exclusions=${COVERAGE_EXCLUSIONS} $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                                    } else {
                                        sh 'mvn -q -B -Dcheckstyle.skip=true org.jacoco:jacoco-maven-plugin:prepare-agent sonar:sonar -Dsonar.pullrequest.provider=github -Dsonar.pullrequest.github.repository=${GITHUB_USERNAME}/${GITHUB_REPONAME} -Dsonar.pullrequest.github.endpoint=https://api.github.com/ -Dsonar.pullrequest.branch=${BRANCH_NAME} -Dsonar.pullrequest.key=${CHANGE_ID} -Dsonar.pullrequest.base=${CHANGE_TARGET} -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=$SONAR_TOKEN -Dsonar.organization=codice -Dsonar.projectKey=${SONAR_PROJECT_KEY} -Dsonar.exclusions=${COVERAGE_EXCLUSIONS} -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/$CHANGE_TARGET $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                                    }
                                }
                            }
                        }
                    }
                    post {
                        success {
                            script {
                                withCredentials([usernameColonPassword(credentialsId: 'cxbot', variable: 'GITHUB_TOKEN')]) {
                                    def jsonBlob = getGithubStatusJsonBlob("success", "${BUILD_URL}display/redirect", "Sonar Succeeded!", "jenkins/static-analysis/sonar")
                                    postStatusToHash("${jsonBlob}", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${env.PR_COMMIT}", "${GITHUB_TOKEN}")
                                }
                            }
                        }
                        failure {
                            script {
                                withCredentials([usernameColonPassword(credentialsId: 'cxbot', variable: 'GITHUB_TOKEN')]) {
                                    def jsonBlob = getGithubStatusJsonBlob("failure", "${BUILD_URL}display/redirect", "Sonar Failed!", "jenkins/static-analysis/sonar")
                                    postStatusToHash("${jsonBlob}", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${env.PR_COMMIT}", "${GITHUB_TOKEN}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    post {
        success {
            slackSend color: 'good', message: "SUCCESS: ${JOB_NAME} ${BUILD_NUMBER}"
            withCredentials([usernameColonPassword(credentialsId: 'cxbot', variable: 'GITHUB_TOKEN')]) {
                postCommentIfPR("Build success! See the job results in [legacy Jenkins UI](${BUILD_URL}) or in [Blue Ocean UI](${BUILD_URL}display/redirect).", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${GITHUB_TOKEN}")
            }
        }
        failure {
            slackSend color: '#ea0017', message: "FAILURE: ${JOB_NAME} ${BUILD_NUMBER}. See the results here: ${BUILD_URL}"
            withCredentials([usernameColonPassword(credentialsId: 'cxbot', variable: 'GITHUB_TOKEN')]) {
                postCommentIfPR("Build failure. See the job results in [legacy Jenkins UI](${BUILD_URL}) or in [Blue Ocean UI](${BUILD_URL}display/redirect).", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${GITHUB_TOKEN}")
            }
        }
        unstable {
            slackSend color: '#ffb600', message: "UNSTABLE: ${JOB_NAME} ${BUILD_NUMBER}. See the results here: ${BUILD_URL}"
            withCredentials([usernameColonPassword(credentialsId: 'cxbot', variable: 'GITHUB_TOKEN')]) {
                postCommentIfPR("Build unstable. See the job results in [legacy Jenkins UI](${BUILD_URL}) or in [Blue Ocean UI](${BUILD_URL}display/redirect).", "${GITHUB_USERNAME}", "${GITHUB_REPONAME}", "${GITHUB_TOKEN}")
            }
        }
    }
}
