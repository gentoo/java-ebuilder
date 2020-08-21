#!/bin/bash

[[ -z "${CUR_STAGE_DIR}" ]] && exit 1
[[ -z "${CONFIG}" ]] && exit 1

source "${CONFIG}"

CUR_STAGE_DIR=$(python -c "print('${CUR_STAGE_DIR}')")

mkdir -p "${POMDIR}"
mkdir -p "${CUR_STAGE_DIR}"

for artifact in $MAVEN_ARTS; do
    $TSH $artifact
    if [[ $? -ne 0 ]]; then
        echo [!] While processing $artifact, TSH returned an error
        exit 1
    fi
done
