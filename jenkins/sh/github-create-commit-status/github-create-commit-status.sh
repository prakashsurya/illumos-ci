#!/bin/bash

source ${SH_LIBRARY_PATH}/common.sh
source ${SH_LIBRARY_PATH}/github.sh

check_env REPOSITORY COMMIT STATE CONTEXT DESCRIPTION

DIR=$(dirname ${BASH_SOURCE[0]})
NAME=$(basename -s ".sh" ${BASH_SOURCE[0]})

github_setup_environment

log_must ruby "${DIR}/${NAME}.rb" \
    --netrc-file netrc-file \
    --repository "$REPOSITORY" \
    --sha "$COMMIT" \
    --state "$STATE" \
    --context "$CONTEXT" \
    --description "$DESCRIPTION" \
    --target-url "$TARGET_URL"

# vim: tabstop=4 shiftwidth=4 expandtab textwidth=72 colorcolumn=80
