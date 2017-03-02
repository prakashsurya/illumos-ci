#!/bin/bash

source ${SH_LIBRARY_PATH}/common.sh
source ${SH_LIBRARY_PATH}/aws.sh

check_env REGION IMAGE_ID INSTANCE_TYPE ADD_DISKS

aws_setup_environment "$REGION"

if [[ "$ADD_DISKS" == 'yes' ]]; then
    log_must cat > block-device-mappings.json <<-EOF
	[{
	    "DeviceName": "/dev/xvdb",
	    "Ebs": {
	        "VolumeSize": 8,
	        "DeleteOnTermination": true,
	        "VolumeType": "gp2",
	        "Encrypted": false
	    }
	}, {
	    "DeviceName": "/dev/xvdc",
	    "Ebs": {
	        "VolumeSize": 8,
	        "DeleteOnTermination": true,
	        "VolumeType": "gp2",
	        "Encrypted": false
	    }
	}, {
	    "DeviceName": "/dev/xvdd",
	    "Ebs": {
	        "VolumeSize": 8,
	        "DeleteOnTermination": true,
	        "VolumeType": "gp2",
	        "Encrypted": false
	    }
	}]
	EOF
else
    log_must cat > block-device-mappings.json <<-EOF
	[]
	EOF
fi

#
# We want to cat the contents of this file such that it'll wind up in
# the Jenkins console log, but we need to be careful not to output the
# context to stdout, since we need to reserve that for returning the
# INSTANCE_ID (and _only_ the INSTANCE_ID).
#
log_must cat block-device-mappings.json >&2

INSTANCE_ID=$(log_must aws ec2 run-instances \
    --image-id "$IMAGE_ID" \
    --count 1 \
    --instance-type "$INSTANCE_TYPE" \
    --block-device-mappings file://block-device-mappings.json \
    --associate-public-ip-address | \
    jq -M -r .Instances[0].InstanceId)

#
# This is a hack, but we've seen instances where after calling
# "run-instances" above, the instance returned won't immediately be
# found when calling "aws_wait_for_instance_state" below, which results
# in failure (i.e. "aws_wait_for_instance_state" fails in that scenario).
# By waiting here, we seem to avoid this race condition.
#
log_must sleep 10

aws_wait_for_instance_state "$INSTANCE_ID" "running"

log_must echo "$INSTANCE_ID"

# vim: tabstop=4 shiftwidth=4 expandtab textwidth=72 colorcolumn=80
