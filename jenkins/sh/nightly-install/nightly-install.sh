#!/bin/bash

source ${SH_LIBRARY_PATH}/common.sh

check_env ILLUMOS_DIRECTORY INSTALL_DEBUG

ILLUMOS_DIRECTORY=$(log_must readlink -f "$ILLUMOS_DIRECTORY")
log_must test -d "$ILLUMOS_DIRECTORY"
log_must cd "$ILLUMOS_DIRECTORY"

ONU="${ILLUMOS_DIRECTORY}/usr/src/tools/scripts/onu"
REPO="${ILLUMOS_DIRECTORY}/packages/i386/nightly"
[[ "$INSTALL_DEBUG" = "yes" ]] || REPO="${REPO}-nd"

log_must sudo "${ONU}" -t "illumos-nightly" -d "${REPO}"

exit 0

# vim: tabstop=4 shiftwidth=4 expandtab textwidth=72 colorcolumn=80
