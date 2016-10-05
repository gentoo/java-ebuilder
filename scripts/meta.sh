#!/usr/bin/env bash
# read in cache from java-ebuilder and find out the groupId,
# artifactId and version.

# cache is by default at $HOME/.java-ebuilder/cache
# example:
# ( echo 1.0; tail -n +2 cache | parallel -j -2 meta.sh; ) > cache.1

pkg=$(awk -F ":" '{print $1"/"$2"-"$3}' <<< $1)
spkg=$(cut -d: -f2 <<< $1)
sver=$(cut -d: -f3 <<< $1)
case ${spkg} in
    guava) 
        echo $1:com.google.guava:${spkg}:${sver%%-*}
        exit 0
        ;;
    json)
        echo $1:org.json:${spkg}:${sver%%-*}
        exit 0
        ;;
    xerces)
        echo $1:xerces:${spkg}:${sver%%-*}
        echo $1:xerces:xercesImpl:${sver%%-*}
        echo $1:xerces:xmlParserAPIs:${sver%%-*}
        exit 0
        ;;
    commons-*|classworlds|jdom)
        echo $1:${spkg}:${spkg}:${sver%%-*}
        exit 0
        ;;
    velocity)
        echo $1:org.apache.${spkg}:${spkg}:${sver%%-*}
        exit 0
        ;;
    jtidy)
        echo $1:net.sf.${spkg}:${spkg}:${sver%%-*}
        exit 0
        ;;
    saxpath)
        echo $1:${spkg}:${spkg}:${sver%%-*}-FCS # maven version
        echo $1:org.jdom:${spkg}:${sver%%-*}-FCS
        exit 0
        ;;
    ant*)
        [[ ${spkg} = ant-core ]] && { spkg=ant; echo $1:ant:${spkg}:${sver%%-*}; }
        [[ ${spkg} = ant-ivy ]] && spkg=ivy
        echo $1:org.apache.ant:${spkg}:${sver%%-*}
        exit 0
        ;;
    bcpg|bcprov)
        echo $1:org.bouncycastle:${spkg}-jdk12:130
        for jv in 1{4,5,6}; do
            echo $1:org.bouncycastle:${spkg}-jdk${jv}:${sver%%-*}
        done
        exit 0
        ;;
    asm)
        echo $1:org.ow2.asm:${spkg}:${sver%%-*}
        echo $1:org.ow2.asm:${spkg}-all:${sver%%-*}
        echo $1:org.ow2.asm:${spkg}-debug-all:${sver%%-*}
        exit 0
esac

grep -q ${pkg} <bebd <bpom && exit 0

ebd=$(equery w ${pkg} 2>/dev/null)
if [[ -z "${ebd}" ]]; then
    echo $1:${pkg} >> bebd
    exit 0
fi

# java-utils-2.eclass:java-pkg_needs-vm()
export JAVA_PKG_NV_DEPEND="nothing"

if ! ebuild ${ebd} unpack >/dev/null 2>&1; then
    echo $1:${pkg} >> bebd
    exit 0
fi

bad_pom="yes"
for subd in /dev/shm/portage/${pkg}/work/*; do
    [[ -f ${subd}/pom.xml ]] || continue
    bad_pom=""
    pushd ${subd} > /dev/null
    poms=$(mvn -q --also-make exec:exec -Dexec.executable="pwd" 2> /dev/null | grep ^/)
    popd > /dev/null
    for pd in ${poms}; do
        ppom=$(xml2 < ${pd}/pom.xml | egrep '(groupId|artifactId|version)=')
        PG=$(echo "${ppom}" | sed -n -r -e 's,/project/groupId=(.*),\1,p')
        [[ -z ${PG} ]] && PG=$(echo "${ppom}" | sed -n -r -e 's,/project/parent/groupId=(.*),\1,p')
        PA=$(echo "${ppom}" | sed -n -r -e 's,/project/artifactId=(.*),\1,p')
        PV=$(echo "${ppom}" | sed -n -r -e 's,/project/version=(.*),\1,p')
        [[ -z ${PV} ]] && PV=$(echo "${ppom}" | sed -n -r -e 's,/project/parent/version=(.*),\1,p')
        echo $1:${PG}:${PA}:${PV/-SNAPSHOT/}
    done
done
if [[ -n "${bad_pom}" ]]; then
    echo $1:${pkg} >> bpom
fi

ebuild ${ebd} clean >/dev/null 2>&1
