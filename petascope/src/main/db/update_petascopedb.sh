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
# ./update_petascopedb.sh [--revert] [--cleanup] [--help]
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
#  In case of creation of the database from scratch the update counter will start
#  from $FIRST_COUNTER update file, which is the update file of the last /upgrade/.
#
# PRECONDITIONS
#  1) PostgreSQL server must be running
#  2) etc/petascope.properties should be present, and the metadata_user should
#     have appropriate write rights in postgres.
#  3) share/rasdaman/petascope should contain the SQL update scripts

PROG=$( basename $0 )

# argument name + flags
REVERT_ARG='--revert'
CLEANUP_ARG='--cleanup'
HELP_ARG='--help'

# synopsis message
USAGE="
    usage: $PROG [$REVERT_ARG] [$CLEANUP_ARG] [$HELP_ARG]
    where:
    $REVERT_ARG
        Restore tables to a pre-upgrade backed-up schema (ps_pre_updateX).
    $CLEANUP_ARG
        Drops all schemas in petascopedb, but the public one.
    $HELP_ARG
        Show this message.
"
# In case the usage changes, consequently adjust these values:
MIN_ARGS=0
MAX_ARGS=2

export CODE_OK=0
export CODE_FAIL=255

# petascope settings file
SETTINGS=/home/rasdaman/install/etc/petascope.properties

# petascope updateN.* scripts
export SCRIPT_DIR=/home/rasdaman/install/share/rasdaman/petascope

# petascopedb tables and fields
PS_DBUPDATES=ps_dbupdates
DBUPDATES_ID=id
DBUPDATES_UPDATE=update

# first update counter in case of no existing petascopedb database
FIRST_COUNTER=8 # start from update8.sh for rasdaman 9.0 upgrade

# array for storing backup schema names
declare -a BACKUP_SCHEMAS=''
export BACKUP_SCHEMA_PREFIX='ps_pre_'

# WMS tables : special case, no need for upgrade, just mv (export for update8)
export WMS_SCHEMA_SQL="update5.sql"
declare -a WMS_TABLES='' # bash cannot export arrays -> use function
export WMS_TABLES_CSV='' #

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
}
check_paths() {
  if [ ! -f "$SETTINGS" ]; then
    error "petascope settings not found: $SETTINGS"
  fi
  if [ ! -d "$SCRIPT_DIR" ]; then
    error "SQL update script directory not found: $SCRIPT_DIR"
  fi
}
check_args_number() {
  if [ $1 -lt $MIN_ARGS -o $1 -gt $MAX_ARGS ]
  then
    error "$USAGE"
fi
}
check_ret() {
  if [ "$1" -ne 0 ]; then
    error "FAILED (return value $1)."
  fi
}

#
# PostgreSQL utilities
#
get_wms_tables() {
  WMS_TABLES=($( grep -i "CREATE TABLE ps_" "$SCRIPT_DIR/$WMS_SCHEMA_SQL" | grep -io "ps_.*" ))
  WMS_TABLES_CSV=$(echo \'$( echo ${WMS_TABLES[@]} | sed "s/ /','/g" )\')
}
move_wms_tables() {
  # move WMS tables from schema $1 to schema $2 (DEFAULT public).
  if [ -z "$1" ]
  then
      log "missing source and/or destination schema names as argument of ${FUNCNAME[0]}."
  else
      source_schema="$1"
      destination_schema=${2:-public}
      # wms tables back to public
      get_wms_tables # fill WMS_TABLES and WMS_TABLES_CSV arrays
      count_qry="SELECT COUNT(*) FROM pg_tables WHERE tablename ILIKE ANY(ARRAY[${WMS_TABLES_CSV}]) AND schemaname = '$source_schema'"
      wms_schema_tables_count=$( $PSQL -P t -P format=unaligned -c "$count_qry"  2>/dev/null )
      if [ $wms_schema_tables_count -eq ${#WMS_TABLES[@]} ]
      then
          logn "moving WMS tables from '$source_schema' to '$destination_schema' schema... "
          for wms_table in "${WMS_TABLES[@]}"
          do
              $PSQL -c "ALTER TABLE $source_schema.$wms_table SET SCHEMA $destination_schema" > /dev/null 2>&1
          done
          echo "ok."
      fi
  fi
}
drop_postgresql_schema() {
  if [ -z "$1" ]
  then
      log "missing schema name as argument of ${FUNCNAME[0]}."
  else
      # drop specified schema
      logn "dropping PostgreSQL schema '$1'... "
      $PSQL -c "DROP SCHEMA IF EXISTS $1 CASCADE" > /dev/null 2>&1
      echo "ok."
  fi
}
schema_is_empty() {
  # returns true (0) if the specified schema (DEFAULT public) is empty
  petascopedb_schema=${1:-public}
  sql_qry="SELECT COUNT(tablename) FROM pg_tables WHERE schemaname = '$petascopedb_schema' AND tablename ILIKE E'ps\\\\_%'"
  schema_tables_count=$( $PSQL -P t -P format=unaligned -c "$sql_qry" 2>/dev/null )
  [ "$schema_tables_count" -eq 0 ] && return 0 || return 1
}
set_dbupdate() {
  counter="$1"
  $PSQL -c "UPDATE $PS_DBUPDATES SET $DBUPDATES_UPDATE = $counter" > /dev/null 2>&1
  log "updated database to update $(( $counter-1 ))"
}

#
# revert/cleanup
#
get_backup_schema_names() {
  # match schemas with pattern ``ps_pre_*''
  BACKUP_SCHEMAS=($( $PSQL -P t -P format=unaligned -c "\dn ${BACKUP_SCHEMA_PREFIX}*"  2>/dev/null | awk -F '|' '{ print $1 }' ))
}
revert_schema() {
  get_backup_schema_names
  case ${#BACKUP_SCHEMAS[@]} in
  0) log "no backup schemas found: nothing to revert."
     ;;
  1) # revert move of WMS tables from backup schema to public that was done during upgrade
     schema_is_empty "${BACKUP_SCHEMAS[0]}"
     if [ $? -ne 0 ]
     then
         move_wms_tables 'public' "${BACKUP_SCHEMAS[0]}"
         # drop public schema
         drop_postgresql_schema 'public'
         # rename backup schema from backup to public
         logn "renaming '${BACKUP_SCHEMAS[0]}' schema to 'public'... "
         $PSQL -c "ALTER SCHEMA ${BACKUP_SCHEMAS[0]} RENAME TO public" > /dev/null 2>&1
         check_ret $?
         echo "ok."
     else
         error "backup schema '${BACKUP_SCHEMAS[0]}' is empty."
     fi
     ;;
  *) error "more than one backup schema found: '${BACKUP_SCHEMAS[@]}'. Please leave a single backup schema in the database and retry."
  esac
}
cleanup_schemas() {
  # get name of backup schemas
  get_backup_schema_names
  if [ ${#BACKUP_SCHEMAS[@]} -eq 0 ]
  then
      log "no backup schemas found: nothing to cleanup."
  else
      # drop backup schema(s)
      for schema in "${BACKUP_SCHEMAS[@]}"
      do
          drop_postgresql_schema "$schema"
      done
  fi
}
sigint_handler() {
  if [ -n $updated ]
  then
      set_dbupdate $counter
  fi
  error "user interrupt."
}

# exporting functions for child scripts
export -f log
export -f logn
export -f error
export -f feedback
export -f check_ret
export -f schema_is_empty
export -f get_wms_tables
export -f move_wms_tables
export -f drop_postgresql_schema

# trap keyboard interrupt (control-c)
trap sigint_handler SIGINT

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
  if [ -w $HOME ]; then
    echo "$PGPASS_LINE" > $PGPASS_FILE
    chmod 600 $PGPASS_FILE
  fi
else
  grep "$PS_USER" $PGPASS_FILE > /dev/null
  if [ $? -ne 0 ]; then
    echo "$PGPASS_LINE" >> $PGPASS_FILE
  fi
fi

#
# commands
#
export PS_USER
export PS_DB
export PSQL="psql -U $PS_USER -d $PS_DB -p $PS_PORT"
export CREATEDB="createdb -U $PS_USER -p $PS_PORT -O $PS_USER"
export SINGLE_TRANSACTION="--set ON_ERROR_STOP=on --single-transaction"

# print some info
log "postgres settings read from $SETTINGS"
log "  user: $PS_USER"
log "  port: $PS_PORT"
log "  db: $PS_DB"

check_postgres


#
# check command arguments
#
check_args_number $#
while [ $# -gt 0 ]
do
  case "$1" in
  $REVERT_ARG)
      revert_schema
      exit $CODE_OK
      ;;
  $CLEANUP_ARG)
      cleanup_schemas
      exit $CODE_OK
      ;;
  $HELP | *)
      error "$USAGE"
      ;;
  esac
  shift
done

#
# create db if not present
#
psql -U $PS_USER -p $PS_PORT --list 2>&1 | egrep "\b$PS_DB\b" > /dev/null
if [ $? -ne 0 ]
then
  logn "no petascope database found, creating... "
  $CREATEDB $PS_DB > /dev/null
  feedback
fi


#
# some updates are written in PL/pgSQL, so install the extension in postgres
#
createlang plpgsql -U $PS_USER -p $PS_PORT $PS_DB > /dev/null 2>&1
rc=$?
if [ $rc -ne 0 -a $rc -ne 2 ]
then
  error "failed creating PL/pgSQL extension in PostgreSQL, please install it first on your system."
fi


#
# insert the database updates (if present)
#

# determine the update number
$PSQL -c "SELECT * FROM $PS_DBUPDATES" > /dev/null 2>&1
if [ $? -ne 0 ]
then
    # no relations in the database: create from scratch
    counter="$FIRST_COUNTER"
else
    # update from the last update number
    counter=$( $PSQL -c "SELECT $DBUPDATES_UPDATE FROM $PS_DBUPDATES" | head -n 3 | tail -n 1 | awk '{print $1}' )
    if [ "$counter" -ne "$counter" ] 2>/dev/null; then
        error "couldn't determine last update from the $PS_DBUPDATES table"
    else
        log "database last update number is $(($counter-1))"
  fi
fi

update_files=$( ls "$SCRIPT_DIR" | grep "update[0-9]*\\." | wc -l )
log "detected $update_files updates in $SCRIPT_DIR... "
log "starting from update counter n.$counter"


while [ -f "$SCRIPT_DIR"/update${counter}.sql -o -f "$SCRIPT_DIR"/update${counter}.sh ]
do
    logn "  update $counter... "

    declare -a UPDATE_FILE=($( ls $SCRIPT_DIR/update${counter}.*  2>/dev/null ))
    EXTENSION=${UPDATE_FILE##*.}

    if [ ${#UPDATE_FILE[@]} -gt 1 ]
    then
       error "multiple update files detected (${UPDATE_FILE[@]}) for counter $counter: please remove duplicates."
    fi

    if [ "$EXTENSION" = "sql" ]
    then
        # Note for future .sql files: do not add manual BEGIN/COMMIT transactions, they are handled by psql
        $PSQL $SINGLE_TRANSACTION -f "$UPDATE_FILE" > /dev/null 2>&1
    elif [ "$EXTENSION" = "sh" ]
    then
        # run shell script that handles the update
        "$UPDATE_FILE"
    else
        error "unsupported update-file extension '$EXTENSION': must be 'sql' or 'sh'."
    fi

    # Check if the update was successful
    ret=$?
    if [ "$ret" -ne 0 ]; then
        echo "FAILED (return value $ret)."
        error_found=1
	break # So that ps_dbupdates is updated to its partial update anyway.
    else
        echo ok.
    fi

    counter=$(( $counter+1 ))
    updated=1
done

#
# save the last update number
#
if [ -n $updated ]
then
    set_dbupdate $counter
fi

#
# closing log
#
if [ ! -z "$error_found" ]
then
    log "database update was interrupted."
else
    if [ -z $updated ]
    then
        log "database already up-to-date, nothing to update."
    else
        log "database updated: no further update was found in ${SCRIPT_DIR}."
    fi
fi

#
# done
#
echo
log "done, exiting."
exit $CODE_OK
