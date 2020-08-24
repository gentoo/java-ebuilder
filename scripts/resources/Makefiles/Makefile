
MAKEFILE_DIR?=./

include ${MAKEFILE_DIR}/environment.mk
include ${MAKEFILE_DIR}/cache.mk
include ${MAKEFILE_DIR}/stage1.mk
include ${MAKEFILE_DIR}/stage2.mk

#include ${STAGE2_MAKEFILE}

.PHONY: all build clean

all: build

build: stage2 post-stage2

clean: clean-cache clean-stage1 clean-stage2
