#!/bin/bash

source ${SH_LIBRARY_PATH}/common.sh
source ${SH_LIBRARY_PATH}/aws.sh

check_env REGION IMAGE_ID

aws_setup_environment "$REGION"

SNAP_ID=$(log_must aws ec2 describe-images --image-ids "$IMAGE_ID" | \
    jq -M -r .Images[0].BlockDeviceMappings[0].Ebs.SnapshotId)
VOL_ID=$(log_must aws ec2 describe-snapshots --snapshot-ids "$SNAP_ID" | \
    jq -M -r .Snapshots[0].VolumeId)

log_must aws ec2 deregister-image --image-id "$IMAGE_ID"
log_must aws ec2 delete-snapshot --snapshot-id "$SNAP_ID"

# vim: tabstop=4 shiftwidth=4 expandtab textwidth=72 colorcolumn=80
