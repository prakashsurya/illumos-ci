job('nightly-build') {
    concurrentBuild(true)
    quietPeriod(0)

    wrappers {
        colorizeOutput('xterm')
    }

    parameters {
        stringParam('ILLUMOSCI_DIRECTORY')
        stringParam('ILLUMOS_DIRECTORY')
        stringParam('BUILD_NONDEBUG')
        stringParam('BUILD_DEBUG')
        stringParam('RUN_LINT')

        stringParam('WORKSPACE')
        nodeParam('NODE')
    }

    customWorkspace('${WORKSPACE}')

    environmentVariables {
        env('SH_LIBRARY_PATH', '${ILLUMOSCI_DIRECTORY}/jenkins/sh/library')
        env('ILLUMOS_DIRECTORY', '${ILLUMOS_DIRECTORY}')
        env('BUILD_NONDEBUG', '${BUILD_NONDEBUG}')
        env('BUILD_DEBUG', '${BUILD_DEBUG}')
        env('RUN_LINT', '${RUN_LINT}')
    }

    steps {
        shell('${ILLUMOSCI_DIRECTORY}/jenkins/sh/nightly-build/nightly-build.sh')
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab textwidth=112 colorcolumn=120
