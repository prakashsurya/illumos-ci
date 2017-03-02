#!/bin/bash

source ${SH_LIBRARY_PATH}/common.sh
source ${SH_LIBRARY_PATH}/aws.sh

check_env REGION INSTANCE_ID

aws_setup_environment "$REGION"

IMAGE_ID=$(log_must aws ec2 create-image \
    --instance-id "$INSTANCE_ID" \
    --name "$INSTANCE_ID" | \
    jq -M -r .ImageId)

aws_wait_for_image_state "$IMAGE_ID" "available"

log_must echo "$IMAGE_ID"

# vim: tabstop=4 shiftwidth=4 expandtab textwidth=72 colorcolumn=80
