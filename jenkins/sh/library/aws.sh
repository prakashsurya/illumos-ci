#!/bin/bash

source ${SH_LIBRARY_PATH}/common.sh
source ${SH_LIBRARY_PATH}/vault.sh

function aws_setup_environment
{
    local REGION="$1"
    check_env PWD REGION

    export HOME="$PWD"
    log_must mkdir -p $HOME/.aws

    #
    # The heredoc requires tabs, not spaces.
    #
    log_must cat > $HOME/.aws/credentials <<-EOF
	[default]
	aws_access_key_id = $(vault_read_aws_access_key)
	aws_secret_access_key = $(vault_read_aws_secret_key)
	region = $REGION
	EOF
}

function aws_get_instance_state
{
    local INSTANCE_ID="$1"
    check_env INSTANCE_ID

    log_must aws ec2 describe-instances --instance-ids "$INSTANCE_ID" | \
        jq -M -r .Reservations[0].Instances[0].State.Name
}

function aws_wait_for_instance_state
{
    local INSTANCE_ID="$1"
    local DESIRED_STATE="$2"
    check_env INSTANCE_ID DESIRED_STATE

    for i in {1..40}; do
        CURRENT_STATE=$(aws_get_instance_state "$INSTANCE_ID")
        [[ "$CURRENT_STATE" == "$DESIRED_STATE" ]] && return 0
        sleep 15
    done

    return 1
}

function aws_get_image_state
{
    local IMAGE_ID="$1"
    check_env IMAGE_ID

    log_must aws ec2 describe-images --image-ids "$IMAGE_ID" | \
        jq -M -r .Images[0].State
}

function aws_wait_for_image_state
{
    local IMAGE_ID="$1"
    local DESIRED_STATE="$2"
    check_env IMAGE_ID DESIRED_STATE

    for i in {1..60}; do
        CURRENT_STATE=$(aws_get_image_state "$IMAGE_ID")
        [[ "$CURRENT_STATE" == "$DESIRED_STATE" ]] && return 0
        sleep 60
    done

    return 1
}

# vim: tabstop=4 shiftwidth=4 expandtab textwidth=72 colorcolumn=80
