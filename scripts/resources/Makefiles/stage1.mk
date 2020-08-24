
.PHONY: stage1 clean-stage1

${STAGE2_MAKEFILE}: ${PRE_STAGE1_CACHE}
	mkdir -p ${STAGE1_DIR}
	mkdir -p "$(shell dirname "$@")"
	CUR_STAGE_DIR="$(shell echo ${STAGE1_DIR})" CUR_STAGE=stage1\
		CACHE_TIMESTAMP="$(shell echo ${CACHE_TIMESTAMP})"\
		GENTOO_CACHE="$(shell echo ${PRE_STAGE1_CACHE})"\
		TARGET_MAKEFILE="$@"\
		TSH=${TSH} CONFIG=${CONFIG}\
		${TSH_WRAPPER}
	touch "$@"

stage1: ${STAGE2_MAKEFILE}

clean-stage1:
	if [[ -f ${STAGE2_MAKEFILE} ]]; then rm ${STAGE2_MAKEFILE}; fi
	if [[ -d ${STAGE1_DIR} ]]; then rm ${STAGE1_DIR} -r; fi
	if [[ -d ${POMDIR} ]]; then touch ${POMDIR}/pseudo; rm ${POMDIR}/* -r; fi
