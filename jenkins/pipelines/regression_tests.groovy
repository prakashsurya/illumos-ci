env.ILLUMOSCI_DIRECTORY = 'illumos-ci'
env.ILLUMOSCI_STASH = 'illumos-ci'

env.ILLUMOS_DIRECTORY = 'illumos'
env.ILLUMOS_STASH = 'illumos'

if (!ILLUMOS_COMMIT) {
    error('Empty ILLUMOS_COMMIT parameter.')
}

env.ILLUMOS_COMMIT_SHORT = ILLUMOS_COMMIT.take(7)
currentBuild.displayName = "#${env.BUILD_NUMBER} ${ILLUMOS_REPOSITORY} ${ILLUMOS_COMMIT_SHORT}"

node('master') {
    def misc = null
    stage('checkout and stash') {
        checkout([$class: 'GitSCM', changelog: true, poll: false,
                userRemoteConfigs: [[name: 'origin',
                        url: "https://github.com/${ILLUMOS_REPOSITORY}",
                        refspec: '+refs/pull/*:refs/remotes/origin/pr/*']],
                branches: [[name: ILLUMOS_COMMIT]],
                extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: env.ILLUMOS_DIRECTORY]]])
        stash(name: env.ILLUMOS_STASH, includes: "${env.ILLUMOS_DIRECTORY}/**", useDefaultExcludes: false)

        checkout([$class: 'GitSCM', changelog: false, poll: false,
                userRemoteConfigs: [[name: 'origin', url: "https://github.com/${ILLUMOSCI_REPOSITORY}"]],
                branches: [[name: ILLUMOSCI_BRANCH]],
                extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: env.ILLUMOSCI_DIRECTORY]]])
        stash(name: env.ILLUMOSCI_STASH, includes: "${env.ILLUMOSCI_DIRECTORY}/**")
        misc = load("${env.ILLUMOSCI_DIRECTORY}/jenkins/pipelines/miscellaneous.groovy")
    }

    if (!misc) {
        error('Failure to load miscellaneous.groovy.')
    }

    /*
     * We're overriding the BUILD_URL environment variable to point to the "Blue Ocean" view for this build
     * because that provides a better UI than the "legacy" view (by default, BUILD_URL will point to the
     * "legacy" view). This custom value for BUILD_URL will then be propogated in "Target URL" for the GitHub
     * commit statuses. This way, the links in the commit statuses direct the user directly to the "Blue Ocean"
     * view, rather than the "legacy" view.
     */
    env.BUILD_URL = env.JENKINS_URL + "/blue/organizations/jenkins/" +
        env.JOB_NAME + "/detail/" + env.JOB_NAME + "/" + env.BUILD_NUMBER + "/pipeline"

    def context = env.JOB_NAME.replaceAll('-', ' ')
    def err = null
    try {
        misc.shscript(env.ILLUMOSCI_DIRECTORY, 'github-create-commit-status', false, [
            ['REPOSITORY', ILLUMOS_REPOSITORY],
            ['COMMIT', ILLUMOS_COMMIT],
            ['DESCRIPTION', "${context} for commit ${ILLUMOS_COMMIT_SHORT} pending."],
            ['CONTEXT', context],
            ['STATE', 'pending'],
            ['TARGET_URL', env.BUILD_URL]
        ])

        if (env.SKIP_BUILD == 'yes') {
            return
        }

        stage('create build instance') {
            env.BUILD_INSTANCE_ID = misc.shscript(env.ILLUMOSCI_DIRECTORY, 'aws-run-instances', true, [
                ['REGION', env.REGION],
                ['IMAGE_ID', env.BASE_IMAGE_ID],
                ['INSTANCE_TYPE', env.BUILD_INSTANCE_TYPE],
                ['ADD_DISKS', 'no']
            ]).trim()
        }

        stage('configure build instance') {
            if (!env.BUILD_INSTANCE_ID) {
                error('Empty BUILD_INSTANCE_ID environment variable.')
            }

            misc.shscript(env.ILLUMOSCI_DIRECTORY, 'ansible-deploy-roles', false, [
                ['REGION', env.REGION],
                ['INSTANCE_ID', env.BUILD_INSTANCE_ID],
                ['EXTRA_VARS', "jenkins_slave_name=${env.BUILD_INSTANCE_ID} jenkins_master_url=${env.JENKINS_URL}"],
                ['ROLES', 'illumos.build-slave illumos.jenkins-slave'],
                ['WAIT_FOR_SSH', 'yes']
            ])
        }

        node(env.BUILD_INSTANCE_ID) {
            stage('unstash') {
                unstash(env.ILLUMOS_STASH)
                unstash(env.ILLUMOSCI_STASH)
            }

            /*
             * Unfortunately, the pipeline "sh" mechanism doesn't work well if the script happens to emit
             * millions of lines of output, which is possible for the build to emit (e.g. compilation errors).
             * Thus, to workaround this problem, we simply checkout the code here in the pipeline script,
             * but delegate the work to perform the following steps to non-pipeline jobs which do not suffer
             * the same problem when the script emits millions of lines of output. We capure the workspace that
             * was used to perform the checkout, so that the build (and other steps) can use this same workspace
             * to perform their work (and don't have to perform _another_ checkout of the same code).
             */
            env.BUILD_INSTANCE_WORKSPACE = env.WORKSPACE
        }

        if (!env.BUILD_INSTANCE_WORKSPACE) {
            error('Empty BUILD_INSTANCE_WORKSPACE environment variable.')
        }

        run_build_job(misc, 'nightly-build', [
            [$class: 'StringParameterValue', name: 'BUILD_NONDEBUG', value: env.BUILD_NONDEBUG],
            [$class: 'StringParameterValue', name: 'BUILD_DEBUG', value: env.BUILD_DEBUG],
            [$class: 'StringParameterValue', name: 'RUN_LINT', value: env.RUN_LINT]
        ])

        run_build_job(misc, 'nightly-nits', [
            [$class: 'StringParameterValue', name: 'BASE_COMMIT', value: ILLUMOS_COMMIT_BASE]
        ])

        run_build_job(misc, 'nightly-install', [
            [$class: 'StringParameterValue', name: 'INSTALL_DEBUG', value: INSTALL_DEBUG]
        ])

        stage('stop build instance') {
            misc.shscript(env.ILLUMOSCI_DIRECTORY, 'aws-stop-instances', false, [
                ['REGION', env.REGION],
                ['INSTANCE_ID', env.BUILD_INSTANCE_ID]
            ])
        }

        stage('create build image') {
            env.BUILD_IMAGE_ID = misc.shscript(env.ILLUMOSCI_DIRECTORY, 'aws-create-image', true, [
                ['REGION', env.REGION],
                ['INSTANCE_ID', env.BUILD_INSTANCE_ID]
            ]).trim()
        }

        if (env.SKIP_TESTS == 'yes') {
            return
        }

        stage('run tests') {
            parallel('run libc-tests': {
                run_test_job(misc, 'run-libc-tests', env.LIBC_TEST_INSTANCE_TYPE, 'no', [
                    [$class: 'StringParameterValue', name: 'RUNFILE', value: env.LIBC_TEST_RUNFILE]
                ])
            }, 'run os-tests': {
                run_test_job(misc, 'run-os-tests', env.OS_TEST_INSTANCE_TYPE, 'no', [
                    [$class: 'StringParameterValue', name: 'RUNFILE', value: env.OS_TEST_RUNFILE]
                ])
            }, 'run util-tests': {
                run_test_job(misc, 'run-util-tests', env.UTIL_TEST_INSTANCE_TYPE, 'no', [
                    [$class: 'StringParameterValue', name: 'RUNFILE', value: env.UTIL_TEST_RUNFILE]
                ])
            }, 'run zfs-tests': {
                run_test_job(misc, 'run-zfs-tests', env.ZFS_TEST_INSTANCE_TYPE, 'yes', [
                    [$class: 'StringParameterValue', name: 'RUNFILE', value: env.ZFS_TEST_RUNFILE]
                ])
            }, 'run zloop': {
                run_test_job(misc, 'run-zloop', env.ZLOOP_INSTANCE_TYPE, 'no', [
                    [$class: 'StringParameterValue', name: 'ENABLE_WATCHPOINTS', value: env.ZLOOP_ENABLE_WATCHPOINTS],
                    [$class: 'StringParameterValue', name: 'RUN_TIME', value: env.ZLOOP_RUN_TIME]
                ])
            })
        }
    } catch (e) {
        err = e
    } finally {
        def state = 'success'
        if (err) {
            state = 'failure'
        }

        misc.shscript(env.ILLUMOSCI_DIRECTORY, 'github-create-commit-status', false, [
            ['REPOSITORY', ILLUMOS_REPOSITORY],
            ['COMMIT', ILLUMOS_COMMIT],
            ['DESCRIPTION', "${context} for commit ${ILLUMOS_COMMIT_SHORT} finished."],
            ['CONTEXT', context],
            ['STATE', state],
            ['TARGET_URL', env.BUILD_URL]
        ])

        stage('terminate build instance') {
            if (env.BUILD_INSTANCE_ID) {
                misc.shscript(env.ILLUMOSCI_DIRECTORY, 'aws-terminate-instances', false, [
                    ['REGION', env.REGION],
                    ['INSTANCE_ID', env.BUILD_INSTANCE_ID]
                ])
            }
        }

        stage('delete build image') {
            if (env.SKIP_BUILD_IMAGE_DELETION != 'yes' && env.BUILD_IMAGE_ID) {
                misc.shscript(env.ILLUMOSCI_DIRECTORY, 'aws-delete-image', false, [
                    ['REGION', env.REGION],
                    ['IMAGE_ID', env.BUILD_IMAGE_ID]
                ])
            }
        }

        if (err) {
            throw err
        }
    }
}

def run_build_job(misc, job_name, job_parameters) {
    def context = job_name.replaceAll('-', ' ')

    stage(job_name) {
        misc.shscript(env.ILLUMOSCI_DIRECTORY, 'github-create-commit-status', false, [
            ['REPOSITORY', ILLUMOS_REPOSITORY],
            ['COMMIT', ILLUMOS_COMMIT],
            ['DESCRIPTION', "${context} for commit ${ILLUMOS_COMMIT_SHORT} pending."],
            ['CONTEXT', context],
            ['STATE', 'pending']
        ])

        def job = build(job: job_name, propagate: false, wait: true, parameters: job_parameters + [
            [$class: 'StringParameterValue', name: 'ILLUMOSCI_DIRECTORY', value: env.ILLUMOSCI_DIRECTORY],
            [$class: 'StringParameterValue', name: 'ILLUMOS_DIRECTORY', value: env.ILLUMOS_DIRECTORY],
            [$class: 'StringParameterValue', name: 'WORKSPACE', value: env.BUILD_INSTANCE_WORKSPACE],
            [$class: 'NodeParameterValue', name: 'NODE', labels: [env.BUILD_INSTANCE_ID],
                nodeEligibility: [$class: 'AllNodeEligibility']],
        ])

        def state = 'success'
        if (job.result != 'SUCCESS') {
            state = 'failure'
        }

        misc.shscript(env.ILLUMOSCI_DIRECTORY, 'github-create-commit-status', false, [
            ['REPOSITORY', ILLUMOS_REPOSITORY],
            ['COMMIT', ILLUMOS_COMMIT],
            ['DESCRIPTION', "${context} for commit ${ILLUMOS_COMMIT_SHORT} finished."],
            ['CONTEXT', context],
            ['STATE', state],
            ['TARGET_URL', job.rawBuild.environment.BUILD_URL + "consoleFull"]
        ])

        if (job.result != 'SUCCESS') {
            error("Build job '${job_name}' did not succeed.")
        }
    }
}

def run_test_job(misc, job_name, instance_type, disks, job_parameters) {
    if (!env.BUILD_IMAGE_ID) {
        error('Empty BUILD_IMAGE_ID environment variable.')
    }

    def context = job_name.replaceAll('-', ' ')
    def instance_id = null
    try {
        /*
         * When we run "shscript" below, we need to be careful to ensure that if the scripts are executed in
         * parallel, they don't overwrite the data in the workspace that another script happens to be using.
         *
         * When the scripts are executed without running on a seperate "node", they will end up sharing the same
         * workspace. Thus, if a script is executed in parallel, the two invocations can easily "corrupt" the
         * workspace by each invocation writing to the same file at (more or less) the same time. To workaround
         * this, we use "ws" to ensure a unique workspace is provided for each script that's invoked.
         *
         * Additionally, since "ws" will allocate a new workspace, we then need to "unstash" the illumos CI
         * directory, so the underlying shell script is available to be executed by "shscript". Even though the
         * repository was checked out in the beginning of the job, that copy won't be present in the workspace
         * allocated by "ws".
         */
        ws {
            unstash(env.ILLUMOSCI_STASH)

            instance_id = misc.shscript(env.ILLUMOSCI_DIRECTORY, 'aws-run-instances', true, [
                ['REGION', env.REGION],
                ['IMAGE_ID', env.BUILD_IMAGE_ID],
                ['INSTANCE_TYPE', instance_type],
                ['ADD_DISKS', disks]
            ]).trim()

            if (!instance_id) {
                error('Unable to create instance.')
            }

            misc.shscript(env.ILLUMOSCI_DIRECTORY, 'ansible-deploy-roles', false, [
                ['REGION', env.REGION],
                ['INSTANCE_ID', instance_id],
                ['EXTRA_VARS',
                    "jenkins_slave_name=${instance_id} jenkins_master_url=${env.JENKINS_URL}"],
                ['ROLES', 'illumos.build-slave illumos.jenkins-slave'],
                ['WAIT_FOR_SSH', 'yes']
            ])

            misc.shscript(env.ILLUMOSCI_DIRECTORY, 'github-create-commit-status', false, [
                ['REPOSITORY', ILLUMOS_REPOSITORY],
                ['COMMIT', ILLUMOS_COMMIT],
                ['DESCRIPTION', "${context} for commit ${ILLUMOS_COMMIT_SHORT} pending."],
                ['CONTEXT', context],
                ['STATE', 'pending']
            ])
        }

        def job = build(job: job_name, propagate: false, wait: true, parameters: job_parameters + [
            [$class: 'StringParameterValue', name: 'ILLUMOSCI_REPOSITORY', value: ILLUMOSCI_REPOSITORY],
            [$class: 'StringParameterValue', name: 'ILLUMOSCI_BRANCH', value: ILLUMOSCI_BRANCH],
            [$class: 'StringParameterValue', name: 'ILLUMOSCI_DIRECTORY', value: env.ILLUMOSCI_DIRECTORY],
            [$class: 'NodeParameterValue', name: 'NODE', labels: [instance_id],
                nodeEligibility: [$class: 'AllNodeEligibility']],
        ])

        ws {
            unstash(env.ILLUMOSCI_STASH)

            def state = 'success'
            if (job.result != 'SUCCESS') {
                state = 'failure'
            }

            misc.shscript(env.ILLUMOSCI_DIRECTORY, 'github-create-commit-status', false, [
                ['REPOSITORY', ILLUMOS_REPOSITORY],
                ['COMMIT', ILLUMOS_COMMIT],
                ['DESCRIPTION', "${context} for commit ${ILLUMOS_COMMIT_SHORT} finished."],
                ['CONTEXT', context],
                ['STATE', state],
                ['TARGET_URL', job.rawBuild.environment.BUILD_URL + "consoleFull"]
            ])

            misc.shscript(env.ILLUMOSCI_STASH, 'download-remote-directory', false, [
                ['REGION', env.REGION],
                ['INSTANCE_ID', instance_id],
                ['REMOTE_DIRECTORY', '/var/tmp/test_results'],
                ['LOCAL_FILE', "${job_name}.tar.xz"]
            ])

            archive(includes: "${job_name}.tar.xz")
        }

        /*
         * In order to archive the "test_results" directory even when the test job fails, we need to set
         * "propogate" to "false" when running the test job. We still want failures of the test job to fail the
         * regression test as a whole, though, so we explicitly check the result here, after we've had a change
         * to download and archive the results of the test.
         */
        if (job.result != 'SUCCESS') {
            error("Test job '${job_name}' did not succeed.")
        }
    } finally {
        if (instance_id) {
            ws {
                unstash(env.ILLUMOSCI_STASH)
                misc.shscript(env.ILLUMOSCI_DIRECTORY, 'aws-terminate-instances', false, [
                    ['REGION', env.REGION],
                    ['INSTANCE_ID', instance_id]
                ])
            }
        }
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab textwidth=112 colorcolumn=120
