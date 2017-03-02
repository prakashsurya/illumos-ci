#!/bin/bash

source ${SH_LIBRARY_PATH}/common.sh

check_env RUNFILE
log_must sudo /opt/libc-tests/bin/libctest -c $RUNFILE

# vim: tabstop=4 shiftwidth=4 expandtab textwidth=72 colorcolumn=80
