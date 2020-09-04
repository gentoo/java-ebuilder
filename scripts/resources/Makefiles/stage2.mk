.PHONY: stage2 clean-stage2 force-stage2 post-stage2

stage2: ${STAGE2_MAKEFILE} ${POST_STAGE1_CACHE}
	make -f ${STAGE2_MAKEFILE} all -j`nproc`

force-stage2: ${STAGE2_MAKEFILE} ${POST_STAGE1_CACHE}
	make -f ${STAGE2_MAKEFILE} all -j`nproc` -B

post-stage2:
	for dir in ${MAVEN_OVERLAY_DIR}/*; do \
		find $${dir} -type f -name \*.ebuild | grep . || continue;\
		pushd $${dir} > /dev/null;\
		parallel ebuild '$$(echo {}/*.ebuild | cut -d\  -f1)' digest ::: *;\
		popd > /dev/null;\
	done

clean-stage2:
	# just to make sure "${MAVEN_OVERLAY_DIR}" points to an overlay
	if [[ -f ${MAVEN_OVERLAY_DIR}/profiles/repo_name ]]; then\
		find ${MAVEN_OVERLAY_DIR} -type f \
		\( -name \*.ebuild \
		-o  -name Manifest \)\
		-delete;\
		find ${MAVEN_OVERLAY_DIR} -type d \
		-empty -delete;\
	fi
