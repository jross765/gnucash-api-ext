#!/bin/bash
#######################################################################

# ---------------------------------------------------------------------
# Before we start...

# Check that we are in the correct directory
CURRDIR=$( pwd )
if [ ! -f "$CURRDIR/build.sh" ]
then
  echo "This build script has to be started in the directory where it is located."
  echo "Aborting."
  exit 1
fi

# ---------------------------------------------------------------------
# Core

echo ""
echo "=============================================================="
echo "Module: 'gnucash-api-ext'"
echo "=============================================================="

# mvn package
mvn package -Dmaven.test.skip.exec
mvn install -Dmaven.test.skip.exec
