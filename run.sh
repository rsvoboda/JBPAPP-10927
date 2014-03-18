#!/bin/bash


init_variables() {
	TMP_DIR=/tmp
	PROFILE=default
	export EAP_VERSION=5.3.0.ER2
	export EAP_VERSION_MAJOR=$(echo $EAP_VERSION | awk -F"." '{ print $1 "." $2 }' )

	export EAP_INSTALLATION_ZIP=$ARTIFACTS_DIR/jboss-eap-noauth-${EAP_VERSION}.zip 
	export EAP_CXF_INSTALLATION_ZIP=$ARTIFACTS_DIR/jboss-ep-ws-cxf-${EAP_VERSION}-installer.zip

	export JBOSS_HOME=$(pwd)/jboss-eap-${EAP_VERSION_MAJOR}/jboss-as
}

usage() {
	echo "Usage:"
	echo "$(basename $0) verify ARTIFACTS_DIR (eg. $(basename $0) /home/development/artifacts/JBEAP-5.3.0.GA/) "
	echo " or "
	echo "$(basename $0) install_server EAP_INSTALLATION_ZIP EAP_CXF_INSTALLATION_ZIP (eg. $(basename $0) /home/development/artifacts/JBEAP-5.3.0.GA/jboss-eap-noauth-5.3.0.zip /home/development/artifacts/JBEAP-5.3.0.GA/jboss-ep-ws-cxf-5.3.0-installer.zip)"
}

verify_arguments() {
	EAP_INSTALLATION_ZIP=$1
	if [ ! -f $EAP_INSTALLATION_ZIP ]; then
		usage
		exit 1
	fi
	EAP_CXF_INSTALLATION_ZIP=$2
	if [ ! -f $EAP_CXF_INSTALLATION_ZIP ]; then
		usage
		exit 1
	fi
}

delete_as() {
	EAP_INSTALL_DIR=$(dirname $JBOSS_HOME)
	rm -rf $EAP_INSTALL_DIR
}

install_as() {
	echo "Install $JBOSS_HOME from $EAP_INSTALLATION_ZIP"
	
	EAP_INSTALL_DIR=$(dirname $JBOSS_HOME)
	unzip -q -d $(dirname $EAP_INSTALL_DIR) $EAP_INSTALLATION_ZIP
	
	cp -r  $JBOSS_HOME/server/all/deploy/juddi-service.sar $JBOSS_HOME/server/default/deploy
}

install_cxf() {
	echo "Install CXF stack from $EAP_CXF_INSTALLATION_ZIP to $JBOSS_HOME"
	unzip -q -d $JBOSS_HOME $EAP_CXF_INSTALLATION_ZIP
	(cd $JBOSS_HOME/jbossws-cxf-installer; ant install-noauth)
}

delete_patch() {
	rm -rf $PATCH_DIR
}

patch_prepare() {
	unzip -d $(dirname $PATCH_DIR) $PATCH_ZIP
}


start_as() {
	$JBOSS_HOME/bin/run.sh &
	sleep 25
}

install_server() {
	delete_as
	install_as $EAP_INSTALLATION_ZIP
	install_cxf $EAP_CXF_INSTALLATION_ZIP
}

verify() {
	ARTIFACTS_DIR=$1
	verify_arguments $@
	init_variables
	# install unpatched server
	install_server $EAP_INSTALLATION_ZIP $EAP_CXF_INSTALLATION_ZIP
	# run test to see success
	(cd test; mvn clean verify)
	if [ ! $? == 0 ]; then
		echo "Verification failed - expected test pass"
		exit 1
	fi
	(cd test; mvn clean)
	delete_as
	echo "JBPAPP-10926 was verified"
}


if [[ "$1" == "" ]]; then
	usage
	exit 1
fi

$1 ${*:2} 2>&1 | tee log.txt
