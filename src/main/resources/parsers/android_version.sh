#!/usr/bin/env bash

REPO=$1
TAG=$2

git -C ${REPO} checkout ${TAG} &> /dev/null

if [ -f ${REPO}/core/version_defaults.mk ]; then
    awk '/PLATFORM_VERSION :=/{print $3}' ${REPO}/core/version_defaults.mk
else
    echo "Unknown Android Version"
fi