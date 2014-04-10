#!/bin/bash
#
# This file is part of rasdaman community.
#
# Rasdaman community is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Rasdaman community is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
#
# Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Peter Baumann /
# rasdaman GmbH.
#
# For more information please see <http://www.rasdaman.org>
# or contact Peter Baumann via <baumann@rasdaman.com>.
#
# ------------------------------------------------------------------------------
#
# SYNOPSIS
# ./petascope_insertdemo.sh
#
# Description
#  Command-line utility for updating the petascope database. The updating is
#  done by importing all appropriate updateX.sql files to postgres. The script
#  determines automatically which updates need to be imported. There are two
#  cases:
#   1. no petascope database is present, in which case the updates start from 0.
#   2. a ps_dbupdates table is present, in this case the updates start from the
#      number saved in the update column.
#
#  Database connection details are read from etc/petascope.properties
#
# PRECONDITIONS
#  1) PostgreSQL server must be running
#  2) etc/petascope.properties should be present, and the metadata_user should
#     have appropriate write rights in postgres.
#  3) share/rasdaman/petascope should contain the SQL update scripts

PROG=`basename $0`

CODE_OK=0
CODE_FAIL=255

# petascope settings file
SETTINGS=/home/rasdaman/install/etc/petascope.properties

# petascope updateN.sql scripts
SCRIPT_DIR=/home/rasdaman/install/share/rasdaman/petascope


# ------------------------------------------------------------------------------
# functions
# ------------------------------------------------------------------------------

#
# logging
#
log() {
  echo "$PROG: $*"
}
logn() {
  echo -n "$PROG: $*"
}
error() {
  echo "$PROG: $*" >&2
  echo "$PROG: exiting." >&2
  exit $CODE_FAIL
}
feedback() {
  if [ $? -ne 0 ]; then
    echo failed.
    echo "$PROG: exiting." >&2
    exit $CODE_FAIL
  else
    echo ok.
  fi
}

#
# checks
#
check_postgres() {
  which psql > /dev/null || error "PostgreSQL missing, please add psql to the PATH."
  pgrep postgres > /dev/null
  if [ $? -ne 0 ]; then
    pgrep postmaster > /dev/null || error "The PostgreSQL service is not started."
  fi
  psql -U $PS_USER -p $PS_PORT --list 2>&1 | egrep "\b$PS_DB\b" > /dev/null
  if [ $? -ne 0 ]; then
    error "no petascope database found, please run update_petascopedb.sh first."
  fi
}
check_rasdaman() {
  which rasql > /dev/null || error "rasdaman not installed, please add rasql to the PATH."
  pgrep rasmgr > /dev/null || error "rasdaman not started, please start with start_rasdaman.sh"

  # set rascontrol login
  if [ -z "$RASLOGIN" ]; then
    export RASLOGIN=rasadmin:d293a15562d3e70b6fdc5ee452eaed40
  fi
  $RASCONTROL -x 'list srv -all' > /dev/null || error "no rasdaman servers started."
}
check_paths() {
  if [ ! -f "$SETTINGS" ]; then
	  error "petascope settings not found: $SETTINGS"
  fi
  if [ ! -d "$SCRIPT_DIR" ]; then
    error "SQL update script directory not found: $SCRIPT_DIR"
  fi
}

#
# import 2D geo-referenced data
#
function import_mst()
{
  local TESTDATA_PATH="$1"
  if [ ! -f "$TESTDATA_PATH/mean_summer_airtemp.tif" ]; then
    error "testdata file $TESTDATA_PATH/mean_summer_airtemp.tif not found"
  fi

  c=$2
  c_colltype='GreySet'
  c_marraytype='GreyImage'
  c_crs='%SECORE_URL%/crs/EPSG/0/4326'

  #
  # START
  #
  $RASERASE --coverage $c > /dev/null 2>&1
  $RASIMPORT -f "${TESTDATA_PATH}/mean_summer_airtemp.tif" \
             --coll $c \
             --coverage-name $c \
             -t  ${c_marraytype}:${c_colltype} \
             --crs-uri  "$c_crs" \
             --crs-order 1:0  > /dev/null || exit $RC_ERROR

  # (re)initialize WMS
  "$DROPWMS" australia_wms > /dev/null 2>&1
  "$INITWMS" australia_wms mean_summer_airtemp EPSG:4326 -l '2:4:8:16' -h localhost -p $PETASCOPE_PORT > /dev/null 2>&1
  if [ $? -ne 0 ]; then
    log "Warning: WMS initialization for mean_summer_airtemp failed."
    log "Reloading the server capabilities might have failed (assuming localhost:${PETASCOPE_PORT}): please try to restart the server and check WMS GetCapabilities."
  fi
  "$FILLPYR" mean_summer_airtemp --tasplit > /dev/null 2>&1
  if [ $? -ne 0 ]; then
    log "Warning: WMS pyramid creation for mean_summer_airtemp failed."
  fi
}

# ------------------------------------------------------------------------------
# work
# ------------------------------------------------------------------------------

check_paths

#
# postgres connection details
#
PS_USER=`grep metadata_user "$SETTINGS" | awk -F "=" '{print $2}'`
PS_USER="${PS_USER#"${PS_USER%%[![:space:]]*}"}"
PS_PASS=`grep metadata_pass "$SETTINGS" | awk -F "=" '{print $2}'`
PS_PASS="${PS_PASS#"${PS_PASS%%[![:space:]]*}"}"
PS_DB=`grep metadata_url "$SETTINGS" | awk -F "/" '{print $4}' | tr -d '\n'`
PS_PORT=`grep metadata_url "$SETTINGS" | awk -F ":|/" '{print $6}' | tr -d '\n'`

# add user/pass to the .pgpass file
PGPASS_LINE="$HOSTNAME:*:*:$PS_USER:$PS_PASS"
PGPASS_FILE="$HOME/.pgpass"
if [ ! -f $PGPASS_FILE ]; then
  echo "$PGPASS_LINE" > $PGPASS_FILE
  chmod 600 $PGPASS_FILE
else
  grep "$PS_USER" $PGPASS_FILE > /dev/null
  if [ $? -ne 0 ]; then
    echo "$PGPASS_LINE" >> $PGPASS_FILE
  fi
fi

#
# postgress commands
#
PSQL="psql -U $PS_USER -d $PS_DB -p $PS_PORT"

# print some info
log "postgres settings read from $SETTINGS"
log "  user: $PS_USER"
log "  port: $PS_PORT"
log "  db: $PS_DB"

check_postgres

#
# rasdaman connection details
#
RAS_USER=`grep rasdaman_admin_user "$SETTINGS" | awk -F "=" '{print $2}' | tr -d '\n'`
RAS_PASS=`grep rasdaman_admin_pass "$SETTINGS" | awk -F "=" '{print $2}' | tr -d '\n'`
RAS_DB=`grep rasdaman_database "$SETTINGS" | awk -F "=" '{print $2}' | tr -d '\n'`
RAS_SERVER=`grep rasdaman_url "$SETTINGS" | awk -F ":|/" '{print $4}' | tr -d '\n'`
RAS_PORT=`grep rasdaman_url "$SETTINGS" | awk -F ":|/" '{print $5}' | tr -d '\n'`
PETASCOPE_PORT=8080

#
# rasdaman commands
#
RASQL="/home/rasdaman/install/bin/rasql --user $RAS_USER --passwd $RAS_PASS -d $RAS_DB -s $RAS_SERVER -p $RAS_PORT"
RASCONTROL="/home/rasdaman/install/bin/rascontrol"
RASIMPORT="/home/rasdaman/install/bin/rasimport"
RASERASE="/home/rasdaman/install/bin/raserase"
INITWMS="/home/rasdaman/install/bin/init_wms.sh"
FILLPYR="/home/rasdaman/install/bin/fill_pyramid.sh"
DROPWMS="/home/rasdaman/install/bin/drop_wms.sh"

# print some info
log "rasdaman settings"
log "  user: $RAS_USER"
log "  host: $RAS_SERVER"
log "  port: $RAS_PORT"
log "  db: $RAS_DB"

check_rasdaman

#
# insert sample data to rasdaman
#
logn "inserting \`mean_summer_airtemp' 2D demo dataset for WC*S and WMS services... "
import_mst "$SCRIPT_DIR" mean_summer_airtemp
echo "Ok."
