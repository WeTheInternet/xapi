#!/bin/sh 

# A process which maintains a running shell, and executes whatever you send on stdin.
# This eval's whatever you send to it!
# VERY DANGEROUS! Use with great discretion.
#
# Defaults to /bin/sh, but you can execute it via any posix shell, such as `/bin/bash repl.sh`

# when invoked from a tty, we'll print the user's PS1 prompt if it is set
set -x
expand_ps1() {
    # shellcheck disable=SC3043
    local ps1 2>/dev/null || true
    ps1="${PS1:-$}"

    # Gather data for escapes
    current_user=$(whoami)
    short_host=$(hostname | cut -d. -f1)
    full_host=$(hostname)
    current_dir=$PWD
    base_dir=$(basename "$PWD")
    current_time=$(date '+%H:%M:%S')
    current_date=$(date '+%a %b %d')
    NL=$(printf '\n')

    # Escape double quotes in ps1 for eval
    ps1_escaped=$(printf '%s' "$ps1" | sed 's/"/\\"/g')

    # Use eval to expand variables and command substitutions inside PS1
    eval "expanded=\"$ps1_escaped\""

    # Now run sed to handle backslash-coded prompt escapes.
    # Using single quotes for the script and inserting variables outside the quotes.
    expanded=$(printf '%s' "$expanded" | sed '
        s/\\\[//g;
        s/\\\]//g;
        s/\\u/'"$current_user"'/g;
        s/\\h/'"$short_host"'/g;
        s/\\H/'"$full_host"'/g;
        s/\\w/'"$current_dir"'/g;
        s/\\W/'"$base_dir"'/g;
        s/\\t/'"$current_time"'/g;
        s/\\d/'"$current_date"'/g;
        s/\\\$/$/g;
        s/\\n/'"$NL"'/g
    ')

    printf '%s\n' "$expanded"
}

debug=${xdebug:-0}
while true 
do
  if [ -t 1 ]; then
      # the repl is connected to a terminal instead of a pipe.
      # print the PS1 prompt
      if [ -n "$PS1" ]; then
        expand_ps1
#        printf "%s" "$PS1"
      fi
  fi
  read -r command
  if [ $debug -gt 0 ]; then
    echo "[RUN] $command" 
  fi
  eval "$command" || {
    code=$?
    echo "[ERROR] Command failed w/ code $code: $command"
    exit $code
  }
  if [ $debug -gt 1 ]; then
   echo "$USER@$HOSTNAME:$PWD$"
  fi
  sleep 0.1
done
