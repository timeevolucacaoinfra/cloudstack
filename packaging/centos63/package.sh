#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

function usage() {
 echo ""
 echo "usage: ./package.sh [-t|--tag] [-h|--help] [ARGS]"
 echo ""

 echo "Examples: ./package.sh -t|--tag 4.2.0-201402261200"
 exit 1
}


function packaging() {
	tag_from_arg=$1
	
	echo "Cheking out to tag: ${tag_from_arg}"
	git checkout $tag_from_arg > /dev/null 2>&1
	[[ $? -ne 0 ]] && echo -e "\nInvalid tag, plese check it (${tag_from_arg})\n" && exit 1
	[[ $tag_from_arg =~ ([0-9]+\.[0-9]+\.[0-9]+)\-([0-9]+) ]] &&  tag_version=${BASH_REMATCH[1]} tag_release=${BASH_REMATCH[2]}

    # source ~/.virtualenvs/cloudstack/bin/activate

	echo "Getting version..."
	VERSION=`(cd ../../; mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version) | grep '^[0-9]\.'`
	[[ "$tag_version" != "$VERSION" ]] && echo "Tag parameter version (${tag_version}) is not the same as git tag version (${VERSION}), fix it!" && exit 1

	CWD=`pwd`
	RPMDIR=$CWD/../../dist/rpmbuild
	PACK_PROJECT=cloudstack	

	if echo $VERSION | grep SNAPSHOT ; then
	  REALVER=`echo $VERSION | cut -d '-' -f 1`
	  DEFVER="-D_ver $REALVER"
	  DEFPRE="-D_prerelease 1"
	  DEFREL="-D_rel SNAPSHOT"
	else
	  REALVER=$VERSION
	  DEFVER="-D_ver $REALVER"
	  DEFREL="-D_rel $tag_release"
	fi

    echo Preparing to package Apache CloudStack ${VERSION}

	mkdir -p $RPMDIR/SPECS
	mkdir -p $RPMDIR/BUILD
	mkdir -p $RPMDIR/SRPMS
	mkdir -p $RPMDIR/RPMS
	mkdir -p $RPMDIR/SOURCES/$PACK_PROJECT-$VERSION

    echo ". preparing source tarball"

	(cd ../../; tar -c --exclude .git --exclude dist  .  | tar -C $RPMDIR/SOURCES/$PACK_PROJECT-$VERSION -x )
	(cd $RPMDIR/SOURCES/; tar -czf $PACK_PROJECT-$VERSION.tgz $PACK_PROJECT-$VERSION)

    echo ". executing rpmbuild"

	cp cloud.spec $RPMDIR/SPECS

	(cd $RPMDIR; rpmbuild --define "_topdir $RPMDIR" "${DEFVER}" "${DEFREL}" ${DEFPRE+${DEFPRE}} -ba SPECS/cloud.spec)

    echo "Done"

	exit
}

if [ $# -lt 1 ] ; then
	usage
elif [ $# -gt 0 ] ; then

	SHORTOPTS="ht:"
	LONGOPTS="help,tag:"

	ARGS=$(getopt -s bash -u -a --options $SHORTOPTS  --longoptions $LONGOPTS --name $0 -- "$@" )
	eval set -- "$ARGS"

	while [ $# -gt 0 ] ; do
	case "$1" in
	-h | --help)
		usage
		exit 0
		;;
	-t | --tag)
		echo "Doing CloudStack Packaging tag: $2..."
		packaging $2
		;;
	*)
		shift
		;;
	esac
	done

else
	echo "Incorrect choice.  Nothing to do." >&2
	echo "Please, execute ./package.sh --help for more help"
fi
