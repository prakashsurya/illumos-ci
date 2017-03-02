#!/bin/bash

source ${SH_LIBRARY_PATH}/common.sh
source ${SH_LIBRARY_PATH}/aws.sh
source ${SH_LIBRARY_PATH}/manta.sh
source ${SH_LIBRARY_PATH}/ssh.sh

check_env REGION ILLUMOSCI_DIRECTORY INSTANCE_ID REMOTE_DIRECTORY \
	PREFIX REPOSITORY BRANCH

aws_setup_environment "$REGION"
manta_setup_environment

HOST=$(log_must aws ec2 describe-instances --instance-ids "$INSTANCE_ID" | \
    jq -M -r .Reservations[0].Instances[0].PublicIpAddress)

log_must test -d "$ILLUMOSCI_DIRECTORY"
log_must cd "$ILLUMOSCI_DIRECTORY/ansible"
ssh_wait_for inventory.txt playbook.yml

#
# If the directory specified via $REMOTE_DIRECTORY doesn't exist, then the
# download process below will fail. As a result, we check the existence
# of this directory, and proactively fail if it's not found.
#
ssh_log_must test -d "$REMOTE_DIRECTORY"

MANTA_DIRECTORY="/$MANTA_USER/public/$PREFIX/$REPOSITORY/$BRANCH"
log_must mmkdir -p "$MANTA_DIRECTORY"
manta_upload_remote_directory \
    "$REMOTE_DIRECTORY" "$MANTA_DIRECTORY/install-media.tar.xz"

# vim: tabstop=4 shiftwidth=4 noexpandtab textwidth=72 colorcolumn=80
