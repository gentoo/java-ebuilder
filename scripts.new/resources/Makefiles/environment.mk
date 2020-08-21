# Variables to drive tree.sh

# determine EROOT
# ${EROOT} with '\' to deal with spaces
EROOT=$(shell printf "%q\n" \
	"$(shell python -c "import portage;print(portage.root)")")
# quoted verion of ${EROOT}
EROOT_SH="$(shell python -c "import portage;print(portage.root)")"

# java-ebuilder.conf
CONFIG?=${EROOT}/etc/java-ebuilder.conf
include ${CONFIG}

# Aritifact whose dependency to be fill
MAVEN_OVERLAY_DIR?=${EROOT}/var/lib/java-ebuilder/maven
POMDIR?=${EROOT}/var/lib/java-ebuilder/poms
CACHE_DIR=$(shell printf "%q\n" ${CACHEDIR})

# helpers
TSH=${EROOT}/usr/lib/java-ebuilder/bin/tree.sh
TSH_WRAPPER=${EROOT}/usr/lib/java-ebuilder/bin/tree-wrapper.sh
FILL_CACHE=${EROOT}/usr/lib/java-ebuilder/bin/fill-cache

# stage
STAGE1_DIR?=${EROOT}/var/lib/java-ebuilder/stage1/
STAGE2_MAKEFILE?=${EROOT}/var/lib/java-ebuilder/stage1/stage2.mk

# PORTAGE REPOS
## grab all the repositories installed on this system
REPOS?=$(shell portageq get_repo_path ${EROOT_SH}\
	$(shell portageq get_repos ${EROOT_SH}))
REPOS+=${MAVEN_OVERLAY_DIR}

# cache
LUTFILE?=${EROOT}/usr/lib/java-ebuilder/resources/LUT
CACHE_TIMESTAMP?=${CACHE_DIR}/cache.stamp
PRE_STAGE1_CACHE?=${CACHE_DIR}/pre-stage1-cache
POST_STAGE1_CACHE?=${CACHE_DIR}/post-stage1-cache
