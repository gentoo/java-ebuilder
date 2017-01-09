#!/bin/bash
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
    local PV=${MV} PA SLOT

    case ${MA} in
        opengl-api)
            [[ ${MV} = 2.1.1 ]] && MV=gl1.1-android-2.1_r1
            ;;
    esac

    # com.github.lindenb:jbwa:1.0.0_ppc64
    PV=${PV/_/.}
    # plexus-container-default 1.0-alpha-9-stable-1
    PV=${PV/-stable-/.}
    PV=$(sed -r 's/[.-]?alpha[-.]?/_alpha/' <<< ${PV})
    # wagon-provider-api 1.0-beta-7
    # com.google.cloud.datastore:datastore-v1beta3-proto-client:1.0.0-beta.2
    # com.google.cloud.datastore:datastore-v1beta3-protos:1.0.0-beta
    PV=$(sed -r 's/[.-]?beta[-.]?/_beta/' <<< ${PV})
    # aopalliance-repackaged 2.5.0-b16
    PV=${PV/-b/_beta}
    # com.google.auto.service:auto-service:1.0-rc2
    PV=${PV/-rc/_rc}
    # cdi-api 1.0-SP4
    PV=${PV/-SP/_p}
    # org.seqdoop:cofoja:1.1-r150
    PV=${PV/-rev/_p}
    PV=${PV/-r/_p}
    PV=${PV/.v/_p}
    # javax.xml.stream:stax-api:1.0-2
    PV=${PV//-/.}
    # .Final .GA -incubating means nothing
    PV=${PV%.[a-zA-Z]*}
    # com.google.cloud.genomics:google-genomics-dataflow:v1beta2-0.15 -> 1.2.0.15
    # plexus-container-default 1.0-alpha-9-stable-1 -> 1.0.9.1
    PV=$(sed -r 's/_(rc|beta|alpha|p)(.*\..*)/.\2/' <<< ${PV})
    # remove all non-numeric charactors before _
    # org.scalamacros:quasiquotes_2.10:2.0.0-M8
    if [[ ${PV} = *_* ]]; then
	PV=$(sed 's/[^.0-9]//g' <<< ${PV/_*/})_${PV/*_/}
    else
	PV=$(sed 's/[^.0-9]//g' <<< ${PV})
    fi

    # spark-launcher_2.11 for scala 2.11
    eval $(sed -nr 's,([^_]*)(_(.*))?,PA=\1 SLOT=\3,p' <<< ${MA})
    [[ -z "${SLOT}" ]] && eval $(sed -nr 's,(.*)-(([0-9]+\.)?[0-9]+),PA=\1 SLOT=\2,p' <<< ${MA})
    [[ -z "${SLOT}" ]] && PA=${MA}
    PA=${PA//./-}
    PA=${PA//_/-}

    local M=${MA}-${MV}
    local SRC_URI="http://central.maven.org/maven2/${WORKDIR}/${MV}/${M}-sources.jar"

    if [[ ! -f "${POMDIR}"/${M}.pom ]]; then
        pushd "${POMDIR}" > /dev/null
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
    fi

    if [[ -z "${MAVEN_NODEP}" ]] && mfill "${ebd}"; then
        java-ebuilder -p "${POMDIR}"/${M}.pom -e "${ebd}" -g --workdir . \
                      -u ${SRC_URI} --slot ${SLOT:-0} --keywords ~amd64 \
                      --cache-file "${CACHEDIR}"/cache
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
