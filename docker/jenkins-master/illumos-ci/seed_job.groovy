job('seed-job') {
    scm {
        /*
         * TODO: This should point to the illumos repository.
         */
        github('prakashsurya/illumos-ci', 'master', 'https')
    }

    triggers {
        scm('@hourly')
    }

    steps {
        dsl {
            external('jenkins/jobs/*.groovy')
            removeAction('DELETE')
        }
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab textwidth=112 colorcolumn=120
