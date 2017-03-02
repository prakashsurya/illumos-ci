def ILLUMOSCI_DIRECTORY = 'illumos-ci'
def ILLUMOSCI_STASH = 'illumos-ci'

def ILLUMOS_STASH = 'illumos'
def ILLUMOS_DIRECTORY = 'illumos'

node('master') {
    def misc = null
    stage('checkout and stash') {
        checkout([$class: 'GitSCM', changelog: true, poll: false,
                userRemoteConfigs: [[name: 'origin',
                        url: "https://github.com/${ILLUMOS_REPOSITORY}",
                        refspec: '+refs/pull/*:refs/remotes/origin/pr/*']],
                branches: [[name: ILLUMOS_BRANCH]],
                extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: ILLUMOS_DIRECTORY]]])
        stash(name: ILLUMOS_STASH, includes: "${ILLUMOS_DIRECTORY}/**", useDefaultExcludes: false)

        checkout([$class: 'GitSCM', changelog: false, poll: false,
                userRemoteConfigs: [[name: 'origin', url: "https://github.com/${ILLUMOSCI_REPOSITORY}"]],
                branches: [[name: ILLUMOSCI_BRANCH]],
                extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: ILLUMOSCI_DIRECTORY]]])
        stash(name: ILLUMOSCI_STASH, includes: "${ILLUMOSCI_DIRECTORY}/**")
        misc = load("${ILLUMOSCI_DIRECTORY}/jenkins/pipelines/miscellaneous.groovy")
    }

    if (!misc) {
        error('Failure to load miscellaneous.groovy.')
    }

    try {
        stage('create instance') {
            env.INSTANCE_ID = misc.shscript(ILLUMOSCI_DIRECTORY, 'aws-run-instances', true, [
                ['REGION', env.REGION],
                ['IMAGE_ID', env.IMAGE_ID],
                ['INSTANCE_TYPE', env.INSTANCE_TYPE],
                ['ADD_DISKS', 'no']
            ]).trim()
        }

        stage('configure instance') {
            if (!env.INSTANCE_ID) {
                error('Empty INSTANCE_ID environment variable.')
            }

            misc.shscript(ILLUMOSCI_DIRECTORY, 'ansible-deploy-roles', false, [
                ['REGION', env.REGION],
                ['INSTANCE_ID', env.INSTANCE_ID],
                ['EXTRA_VARS', "jenkins_slave_name=${env.INSTANCE_ID} jenkins_master_url=${env.JENKINS_URL}"],
                ['ROLES', 'illumos.build-slave illumos.jenkins-slave'],
                ['WAIT_FOR_SSH', 'yes']
            ])
        }

        node(env.INSTANCE_ID) {
            stage('unstash') {
                unstash(ILLUMOS_STASH)
                unstash(ILLUMOSCI_STASH)
            }

            stage('build illumos') {
                misc.shscript(ILLUMOSCI_DIRECTORY, 'nightly-build', false, [
                    ['ILLUMOS_DIRECTORY', ILLUMOS_DIRECTORY],
                    ['BUILD_NONDEBUG', 'yes'],
                    ['BUILD_DEBUG', 'no'],
                    ['RUN_LINT', 'no']
                ])
            }

            stage('build iso') {
                misc.shscript(ILLUMOSCI_DIRECTORY, 'nightly-iso-build', false, [
                    ['ILLUMOS_DIRECTORY', ILLUMOS_DIRECTORY],
                    ['INSTALL_DEBUG', 'no']
                ])
            }
        }
        env.INSTANCE_ID = 'i-09639fc6ecf8f3bee'

        stage('upload media') {
            retry(count: 3) {
                misc.shscript(ILLUMOSCI_DIRECTORY, 'manta-upload-remote-install-media', false, [
                    ['REGION', REGION],
                    ['INSTANCE_ID', env.INSTANCE_ID],
                    ['REMOTE_DIRECTORY', env.MEDIA_DIRECTORY],
                    ['PREFIX', env.MANTA_DIRECTORY_PREFIX],
                    ['REPOSITORY', ILLUMOS_REPOSITORY],
                    ['BRANCH', ILLUMOS_BRANCH]
                ])
            }
        }
    } finally {
        stage('terminate instance') {
            if (env.INSTANCE_ID) {
                misc.shscript(ILLUMOSCI_DIRECTORY, 'aws-terminate-instances', false, [
                    ['REGION', env.REGION],
                    ['INSTANCE_ID', env.INSTANCE_ID]
                ])
            }
        }
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab textwidth=112 colorcolumn=120
