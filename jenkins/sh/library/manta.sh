#!/bin/bash

source ${SH_LIBRARY_PATH}/common.sh
source ${SH_LIBRARY_PATH}/vault.sh
source ${SH_LIBRARY_PATH}/ssh.sh

function manta_setup_environment
{
    check_env PWD

    export MANTA_URL="https://us-east.manta.joyent.com"
    export MANTA_HTTP="http://us-east.manta.joyent.com"
    export MANTA_USER=$(vault_read_manta_user)
    export MANTA_KEY_ID=$(vault_read_manta_keyid)

    export HOME="$PWD"
    log_must mkdir -p $HOME/.ssh
    log_must chmod 700 $HOME/.ssh

    log_must rm -f $HOME/.ssh/id_rsa
    log_must vault_read_manta_private_key > $HOME/.ssh/id_rsa
    log_must chmod 400 $HOME/.ssh/id_rsa

    log_must rm -f $HOME/.ssh/id_rsa.pub
    log_must vault_read_manta_public_key > $HOME/.ssh/id_rsa.pub
    log_must chmod 644 $HOME/.ssh/id_rsa.pub
}

function manta_upload_remote_directory
{
    local REMOTE_DIRECTORY="$1"
    local MANTA_FILE="$2"

    check_env HOST REMOTE_DIRECTORY MANTA_FILE

    ssh_fetch_remote_directory "$REMOTE_DIRECTORY" | log_must mput "$MANTA_FILE"
}

# vim: tabstop=4 shiftwidth=4 expandtab textwidth=72 colorcolumn=80
