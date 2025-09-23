#!/bin/bash
#######################################################################

# ---------------------------------------------------------------------
# Before we start...

# Check that we are in the correct directory
CURRDIR=$( pwd )
if [ ! -f "$CURRDIR/test_prep.sh" ]
then
  echo "This test script has to be started in the directory where it is located."
  echo "Aborting."
  exit 1
fi


######################################################################
######################################################################
######################################################################
#                       F U N C T I O N S
######################################################################
######################################################################
######################################################################

printUsage()
{
  echo "test_prep"
}

######################################################################

main()
{
  # 2) Generate standard "target" directory tree
  echo "=============="
  echo "Check for / Generate Directory"
  echo "=============="

  echo -n "Check for 'target' directory: "
  if [ -d "target" ]
  then
    echo "[ OK ]"
  else
    echo "[ NOT OK ]"
    echo -n "Generate it: "
    mvn test > /dev/null 2>&1
    # Cant' check for return code 0 here, because
    # this command will *always* return 1.
    # if [ "$?" = "0" ]
    # then
    #   echo "[ OK ]"
    # else
    #   echo "[ NOT OK ]"
    #   echo "Can't generate standard 'target' directory tree"
    #   echo "Something is wrong."
    #   exit 1
    # fi
    echo "[ OK ]"
  fi
  
  # 2) Set the symlink to the test data file in module "API".
  echo "=============="
  echo "Check for / Set SymLink"
  echo "=============="

  echo -n "Check for link to test data file: "
  local linkExists=0
  readlink -s "target/test-classes/test.xml" > /dev/null 2>&1
  if [ "$?" = "0" ]
  then
    linkExists=1
  fi
  local linkNotBroken=0
  if  [ -e "target/test-classes/test.xml" ]
  then
    linkNotBroken=1
  fi

  if [ "$linkExists" = "1" ] &&
     [ "$linkNotBroken" = "1" ]
  then
    echo "[ OK ]"
  else
    echo "[ NOT OK ]"
    echo -n "Generate it: "
    cd target/test-classes
    ln -sf ../../../gnucash-api/src/test/resources/test.xml
    cd ../..
    echo "[ OK ]"
  fi
}


######################################################################
######################################################################
######################################################################
#               C O M M A N D   L I N E   A R G S
######################################################################
######################################################################
######################################################################

if [ "$1" = "-h" ] ||
   [ "$1" = "--help" ]
then
  printUsage
  exit 0
fi


######################################################################
######################################################################
######################################################################
#                   C O N F I G U R A T I O N
######################################################################
######################################################################
######################################################################

# ::EMPTY


######################################################################
######################################################################
######################################################################
#                     M A I N   P R O G R A M
######################################################################
######################################################################
######################################################################

main
