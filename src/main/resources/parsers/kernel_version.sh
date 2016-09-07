#!/usr/bin/env bash

REPO=$1
TAG=$2

git -C ${REPO} checkout ${TAG} &> /dev/null

if [ -f ${REPO}/Makefile ]; then
    version=`awk '/^VERSION =/{print $3}' ${REPO}/Makefile`
    patchlevel=`awk '/^PATCHLEVEL =/{print $3}' ${REPO}/Makefile`
    sublevel=`awk '/^SUBLEVEL =/{print $3}' ${REPO}/Makefile`
    echo ${version}.${patchlevel}.${sublevel}
else
    echo "Unknown Kernel Version"
fi