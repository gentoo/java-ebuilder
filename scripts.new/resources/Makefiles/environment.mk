# Variables to drive tree.sh

# SH is bad
SHELL=bash

# determine EROOT.
# thanks to hprefixify, I do not need to call "python3 -c ..."
EROOT_SH="$(shell dirname /etc)"
EROOT=$(shell printf "%q\n" ${EROOT_SH})

# java-ebuilder.conf
CONFIG?=${EROOT}/etc/java-ebuilder.conf
include ${CONFIG}

# Aritifact whose dependency to be fill
MAVEN_OVERLAY_DIR?=${EROOT}/var/lib/java-ebuilder/maven
POMDIR?=${EROOT}/var/lib/java-ebuilder/poms

# helpers
TSH=${EROOT}/usr/lib/java-ebuilder/bin/tree.sh
TSH_WRAPPER=${EROOT}/usr/lib/java-ebuilder/bin/tree-wrapper.sh
FILL_CACHE=${EROOT}/usr/lib/java-ebuilder/bin/fill-cache

# stage
STAGE1_DIR?=${EROOT}/var/lib/java-ebuilder/stage1/
STAGE2_MAKEFILE?=${EROOT}/var/lib/java-ebuilder/stage1/stage2.mk

# PORTAGE REPOS
## grab all the repositories installed on this system
REPOS?=$(shell portageq get_repo_path ${EROOT}\
	$(shell portageq get_repos ${EROOT}))
REPOS+=${MAVEN_OVERLAY_DIR}

# where is the LookUp Table
LUTFILE?=${EROOT}/usr/lib/java-ebuilder/resources/LUT

# cache, redefine CACHE_DIR to make it work with GNU Make
CACHE_DIR=$(shell printf "%q\n" ${CACHEDIR})
CACHE_TIMESTAMP?=${CACHE_DIR}/cache.stamp
PRE_STAGE1_CACHE?=${CACHE_DIR}/pre-stage1-cache
POST_STAGE1_CACHE?=${CACHE_DIR}/post-stage1-cache
