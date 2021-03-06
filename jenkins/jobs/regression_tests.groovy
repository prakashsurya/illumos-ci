pipelineJob('regression-tests') {
    concurrentBuild(true)
    quietPeriod(0)

    parameters {
        // TODO: This should point to the illumos repository, after it exists.
        stringParam('ILLUMOSCI_REPOSITORY', 'prakashsurya/illumos-ci')
        stringParam('ILLUMOSCI_BRANCH', 'master')

        // TODO: This should point to the illumos repository, after it exists.
        stringParam('ILLUMOS_REPOSITORY', 'prakashsurya/openzfs')
        stringParam('ILLUMOS_COMMIT')
        stringParam('ILLUMOS_COMMIT_BASE')
    }

    environmentVariables {
        env('REGION', 'us-east-1')
        env('BASE_IMAGE_ID', 'ami-f068cee6')

        // Skipping the build implies skipping the tests as well.
        env('SKIP_BUILD', 'no')
        env('SKIP_TESTS', 'no')

        /*
         * Keep in mind, the Amazon image (AMI), snapshot, and volume generated by the by this job isn't free.
         * Thus, if deletion of the image is skipped, it will have to be deleted using some other out-of-band
         * mechanism (e.g. manually via the web UI) and the cost of the image, snapshot, and volume will
         * continue to be charged until they are all removed.
         */
        env('SKIP_BUILD_IMAGE_DELETION', 'no')

        env('BUILD_INSTANCE_TYPE', 'c4.xlarge')
        env('BUILD_NONDEBUG', 'yes')
        env('BUILD_DEBUG', 'yes')
        env('RUN_LINT', 'yes')
        env('INSTALL_DEBUG', 'yes')

        env('LIBC_TEST_INSTANCE_TYPE', 'm4.xlarge')
        env('LIBC_TEST_RUNFILE', '/opt/libc-tests/runfiles/default.run')

        env('OS_TEST_INSTANCE_TYPE', 'm4.xlarge')
        env('OS_TEST_RUNFILE', '/opt/os-tests/runfiles/default.run')

        env('UTIL_TEST_INSTANCE_TYPE', 'm4.xlarge')
        env('UTIL_TEST_RUNFILE', '/opt/util-tests/runfiles/default.run')

        env('ZLOOP_INSTANCE_TYPE', 'm4.xlarge')
        env('ZLOOP_RUN_TIME', '9000')
        env('ZLOOP_ENABLE_WATCHPOINTS', 'no')

        env('ZFS_TEST_INSTANCE_TYPE', 'm4.xlarge')
        env('ZFS_TEST_RUNFILE', '/opt/zfs-tests/runfiles/delphix.run')
    }

    definition {
        cps {
            script(readFileFromWorkspace('jenkins/pipelines/regression_tests.groovy'))
        }
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab textwidth=112 colorcolumn=120
