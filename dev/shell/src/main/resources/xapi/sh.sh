#!/bin/sh 

# Our very own virtual sh;
# It's just a process that loops on stdIn,
# And eval's whatever you send to it.
# VERY DANGEROUS! USE ONLY ON LOCALHOST!
#
# Any 'nix gurus with security tips, 
# email admin@wetheinter.net

debug=$xdebug||0
while true 
do
  read -r command
  if [ $debug > 0 ]; then 
    echo "[RUN] $command" 
  fi
  eval "$command" || exit $errno
  if [ $debug > 1 ]; then
   echo "$USER@$HOSTNAME:$PWD$"
  fi
  sleep 1
done
