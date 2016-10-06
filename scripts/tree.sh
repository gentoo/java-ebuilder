#!/usr/bin/env bash
# start from the root of a maven artifact and recursively resolve its
# dependencies.

source /etc/java-ebuilder.conf

mkdir -p "${POMDIR}"

gebd() {
    case ${MA} in
        weld-osgi-bundle)
            # 1.1.0.Final no longer exist
            [[ ${MV} = 1.1.0.Final ]] && MV=1.1.33.Final
            ;;
    esac

    local WORKDIR=${PG//./\/}/${MA} MID
    local MID=${PG}:${MA}:${MV}
    # .Final .GA .v20121024 means nothing
    local PV=${MV%.[a-zA-Z]*} PA SLOT

    case ${MA} in
        opengl-api)
            [[ ${MV} = 2.1.1 ]] && MV=gl1.1-android-2.1_r1
            ;;
    esac

    # plexus-container-default 1.0-alpha-9-stable-1
    PV=${PV/-stable-*/}
    PV=${PV/-alpha-/_alpha}
    # wagon-provider-api 1.0-beta-7
    PV=${PV/-beta-/_beta}
    # aopalliance-repackaged 2.5.0-b16
    PV=${PV/-b/_beta}
    # cdi-api 1.0-SP4
    PV=${PV/-SP/_p}
    # javax.xml.stream:stax-api:1.0-2
    PV=${PV//-/.}

    # spark-launcher_2.11 for scala 2.11
    eval $(sed -nr 's,([^_]*)(_(.*))?,PA=\1 SLOT=\3,p' <<< ${MA})
    [[ -z "${SLOT}" ]] && eval $(sed -nr 's,(.*)-(([0-9]+\.)?[0-9]+),PA=\1 SLOT=\2,p' <<< ${MA})
    [[ -z "${SLOT}" ]] && PA=${MA}
    PA=${PA//./-}
    PA=${PA//_/-}

    local M=${MA}-${MV}
    local SRC_URI="http://central.maven.org/maven2/${WORKDIR}/${MV}/${M}-sources.jar"

    if [[ ! -f "${POMDIR}"/${M}.pom ]]; then
        pushd "${POMDIR}"
        wget ${SRC_URI/-sources.jar/.pom}

        # 3rd party plugin not needed here
        # distributionManagement is invalid for maven 3
        # net.sf.jtidy:jtidy:r938 version is not maven-compliant
        sed -e '/<packaging>bundle/d' \
            -e '/<distributionManagement>/,/<\/distributionManagement>/d' \
            -e '/<build>/,/<\/build>/d' \
            -e '/<modules>/,/<\/modules>/d' \
            -e 's,<version>r938</version>,<version>1.0</version>,' \
            -i ${M}.pom
        popd
    fi

    if ! wget -q --spider ${SRC_URI}; then
        SRC_URI=${SRC_URI/-sources.jar/.jar}
        PA=${PA}-bin
    fi
    local P=${PA}-${PV}
    local ebd="${MAVEN_OVERLAY_DIR}"/app-maven/${PA}/${P}.ebuild

    line=app-maven:${PA}:${PV}:${SLOT:-0}::${MID}
    if ! grep -q ${line} "${CACHEDIR}"/maven-cache 2>/dev/null ; then
        pushd "${CACHEDIR}" > /dev/null
        echo ${line} >> maven-cache
        cat cache.{0,1} maven-cache > cache
        popd > /dev/null
    fi

    if [[ ! -f "${ebd}" ]]; then
        mkdir -p $(dirname ${ebd})
        java-ebuilder -p "${POMDIR}"/${M}.pom -e "${ebd}" -g --workdir . \
                      -u ${SRC_URI} --slot ${SLOT:-0} --keywords ~amd64 \
                      --cache-file "${CACHEDIR}"/cache

        # empty parent artifacts
        # FIXME, this should be removed in poms
        sed -e '/app-maven\/jsch-agentproxy-bin/d' \
            -e '/JAVA_GENTOO_CLASSPATH/s|jsch-agentproxy-bin,||' \
            -i "${ebd}"
    fi

    if [[ -z "${MAVEN_NODEP}" ]] && mfill "${ebd}"; then
        java-ebuilder -p "${POMDIR}"/${M}.pom -e "${ebd}" -g --workdir . \
                      -u ${SRC_URI} --slot ${SLOT:-0} --keywords ~amd64 \
                      --cache-file "${CACHEDIR}"/cache

        # empty parent artifacts
        # FIXME, this should be removed in poms
        sed -e '/app-maven\/jsch-agentproxy-bin-[0-9]/d' \
            -e '/JAVA_GENTOO_CLASSPATH/s|jsch-agentproxy-bin,||' \
            -i "${ebd}"
    fi

    [[ ${SRC_URI} = *-sources.jar ]] || sed -i "/inherit/s/java-pkg-simple/java-pkg-binjar/" "${ebd}"
}

mfill() {
    # recursively fill missing dependencies
    arts=$(sed -n -r 's,# (test\? )?(.*)-> !!!.*-not-found!!!,\2,p' < $1)
    if [[ -z "${arts}" ]]; then
        false # no need to java-ebuilder again
    else
        for a in ${arts}; do
            eval $(awk -F":" '{print "PG="$1, "MA="$2, "MV="$3}' <<< ${a})
            gebd
        done
        return
    fi
}

if [[ $1 == *.ebuild ]]; then
    eval $(grep MAVEN_ID $1)
    rm -f $1
else
    MAVEN_ID=$1
fi
eval $(awk -F":" '{print "PG="$1, "MA="$2, "MV="$3}' <<< ${MAVEN_ID})
gebd
