pipelineJob('build-install-media') {
    quietPeriod(0)

    parameters {
        // TODO: This should point to the illumos repository, after it exists.
        stringParam('ILLUMOSCI_REPOSITORY', 'prakashsurya/illumos-ci')
        stringParam('ILLUMOSCI_BRANCH', 'master')

        // TODO: This should point to the illumos repository, after it exists.
        stringParam('ILLUMOS_REPOSITORY', 'prakashsurya/openzfs')
        stringParam('ILLUMOS_BRANCH', 'illumos-on-ec2')
    }

    environmentVariables {
        env('REGION', 'us-east-1')
        env('IMAGE_ID', 'ami-f068cee6')
        env('INSTANCE_TYPE', 'c4.xlarge')

        env('MANTA_DIRECTORY_PREFIX', 'install-media')
        env('MEDIA_DIRECTORY', '/rpool/dc/media')
    }

    definition {
        cps {
            script(readFileFromWorkspace('jenkins/pipelines/build_install_media.groovy'))
        }
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab textwidth=112 colorcolumn=120
