#!/bin/bash
#

WORKING_DIR="$PWD/target"
CHANGE_SET="$WORKING_DIR/changeSet"
MODULE_SET="$WORKING_DIR/moduleSet"
REFERENCE_BRANCH='origin/master'

mkdir -p "$WORKING_DIR"

git diff --name-only "$REFERENCE_BRANCH" > "$CHANGE_SET"

mvn milk:aggregator -DchangeSet="$CHANGE_SET" -DoutputFile="$MODULE_SET"

MODULES="$(<"$MODULE_SET")"
if [ ${MODULES:+set} ]; then
  mvn -amd -pl "$(<"$MODULE_SET")" "$@"
fi
