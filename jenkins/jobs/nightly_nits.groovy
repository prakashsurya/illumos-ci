job('nightly-nits') {
    concurrentBuild(true)
    quietPeriod(0)

    wrappers {
        colorizeOutput('xterm')
    }

    parameters {
        stringParam('ILLUMOSCI_DIRECTORY')
        stringParam('ILLUMOS_DIRECTORY')
        stringParam('BASE_COMMIT')

        stringParam('WORKSPACE')
        nodeParam('NODE')
    }

    customWorkspace('${WORKSPACE}')

    environmentVariables {
        env('SH_LIBRARY_PATH', '${ILLUMOSCI_DIRECTORY}/jenkins/sh/library')
        env('ILLUMOS_DIRECTORY', '${ILLUMOS_DIRECTORY}')
        env('BASE_COMMIT', '${BASE_COMMIT}')
    }

    steps {
        shell('${ILLUMOSCI_DIRECTORY}/jenkins/sh/nightly-nits/nightly-nits.sh')
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab textwidth=112 colorcolumn=120
