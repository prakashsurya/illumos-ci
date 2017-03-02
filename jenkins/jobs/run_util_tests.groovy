job('run-util-tests') {
    concurrentBuild(true)
    quietPeriod(0)

    wrappers {
        colorizeOutput('xterm')
    }

    parameters {
        stringParam('ILLUMOSCI_REPOSITORY')
        stringParam('ILLUMOSCI_BRANCH')
        stringParam('ILLUMOSCI_DIRECTORY')
        stringParam('RUNFILE')

        nodeParam('NODE')
    }

    scm {
        git {
            remote {
                name('origin')
                github('${ILLUMOSCI_REPOSITORY}')
            }

            branch('${ILLUMOSCI_BRANCH}')

            extensions {
                relativeTargetDirectory('${ILLUMOSCI_DIRECTORY}')
            }
        }
    }

    environmentVariables {
        env('SH_LIBRARY_PATH', '${ILLUMOSCI_DIRECTORY}/jenkins/sh/library')
        env('RUNFILE', '${RUNFILE}')
    }

    steps {
        shell('${ILLUMOSCI_DIRECTORY}/jenkins/sh/run-util-tests/run-util-tests.sh')
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab textwidth=112 colorcolumn=120
