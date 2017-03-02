job('nightly-install') {
    concurrentBuild(true)
    quietPeriod(0)

    wrappers {
        colorizeOutput('xterm')
    }

    parameters {
        stringParam('ILLUMOSCI_DIRECTORY')
        stringParam('ILLUMOS_DIRECTORY')
        stringParam('INSTALL_DEBUG')

        stringParam('WORKSPACE')
        nodeParam('NODE')
    }

    customWorkspace('${WORKSPACE}')

    environmentVariables {
        env('SH_LIBRARY_PATH', '${ILLUMOSCI_DIRECTORY}/jenkins/sh/library')
        env('ILLUMOS_DIRECTORY', '${ILLUMOS_DIRECTORY}')
        env('INSTALL_DEBUG', '${INSTALL_DEBUG}')
    }

    steps {
        shell('${ILLUMOSCI_DIRECTORY}/jenkins/sh/nightly-install/nightly-install.sh')
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab textwidth=112 colorcolumn=120
