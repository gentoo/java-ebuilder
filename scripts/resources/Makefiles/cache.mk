${CACHE_TIMESTAMP}:
	touch "$@"

${PRE_STAGE1_CACHE}.raw: ${CACHE_TIMESTAMP}
	args=" --refresh-cache";\
	for repo in ${REPOS}; do\
		args="$${args} -t $${repo}";\
	done;\
	java-ebuilder $${args} --cache-file "$@"

${PRE_STAGE1_CACHE}: ${PRE_STAGE1_CACHE}.raw
	${FILL_CACHE} --dst-cache "$@" --src-cache "$^" --LUT "${LUTFILE}"

${POST_STAGE1_CACHE}.raw: ${STAGE2_MAKEFILE}
	args=" --refresh-cache -t ${STAGE1_DIR}";\
	for repo in ${REPOS}; do\
		args="$${args} -t $${repo}";\
	done;\
	java-ebuilder $${args} --cache-file "$@"

${POST_STAGE1_CACHE}: ${POST_STAGE1_CACHE}.raw
	${FILL_CACHE} --dst-cache "$@" --src-cache "$^" --LUT "${LUTFILE}"

clean-cache:
	if [[ -d ${CACHE_DIR} ]]; then touch ${CACHE_DIR}/pseudo; rm ${CACHE_DIR}/* -r; fi
