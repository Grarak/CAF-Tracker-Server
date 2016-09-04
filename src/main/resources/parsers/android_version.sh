#!/usr/bin/env bash

PATH=$1
TAG=$2

git -C $PATH checkout $TAG &> /dev/null

if [ -f $PATH/core/version_defaults.mk ]; then
    awk '/PLATFORM_VERSION :=/{print $3}' $PATH/core/version_defaults.mk
else
    echo "Unknown Android Version"
fi