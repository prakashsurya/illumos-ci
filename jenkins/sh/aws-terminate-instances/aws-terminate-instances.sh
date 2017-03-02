#!/bin/bash

source ${SH_LIBRARY_PATH}/common.sh
source ${SH_LIBRARY_PATH}/aws.sh

check_env REGION INSTANCE_ID

aws_setup_environment "$REGION"

log_must aws ec2 terminate-instances --instance-ids "$INSTANCE_ID"
log_must aws_wait_for_instance_state "$INSTANCE_ID" "terminated"

# vim: tabstop=4 shiftwidth=4 expandtab textwidth=72 colorcolumn=80
