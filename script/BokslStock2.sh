#!/bin/bash

# The directory in which your application is installed
APPLICATION_DIR="."
# The Java command to use to launch the application (must be java 8+)
JAVA=java

# ***********************************************
OUT_FILE="${APPLICATION_DIR}"/out.log
RUNNING_PID="${APPLICATION_DIR}"/BokslStock2.pid
# ***********************************************

# colors
red='\e[0;31m'
green='\e[0;32m'
yellow='\e[0;33m'
reset='\e[0m'

echoRed() { echo -e "${red}$1${reset}"; }
echoGreen() { echo -e "${green}$1${reset}"; }
echoYellow() { echo -e "${yellow}$1${reset}"; }

# Check whether the application is running.
# The check is pretty simple: open a running pid file and check that the process
# is alive.
isrunning() {
  # Check for running app
  if [ -f "$RUNNING_PID" ]; then
    proc=$(cat $RUNNING_PID);
    if /bin/ps --pid $proc 1>&2 >/dev/null;
    then
      return 0
    fi
  fi
  return 1
}

start() {
  if isrunning; then
    echoYellow "The BokslStock2 application is already running"
    return 0
  fi

  nohup $JAVA -Dlogging.config=../conf/logback-spring.xml -Dspring.profiles.active=local -jar ../lib/BokslStock2-0.0.1.jar --spring.config.location=file:../conf/BokslStock2.yml 1> /dev/null 2>&1 &
  echo $! > ${RUNNING_PID}

  if isrunning; then
    echoGreen "BokslStock2 Application started"
    exit 0
  else
    echoRed "The BokslStock2 Application has not started - check log"
    exit 3
  fi
}

console() {
  $JAVA -Dlogging.config=../conf/logback-spring.xml -Dspring.profiles.active=local -jar ../lib/BokslStock2-0.0.1.jar --spring.config.location=file:../conf/BokslStock2.yml
}

restart() {
  echo "Restarting BokslStock2 Application"
  stop
  sleep 5
  start
}

stop() {
  echoYellow "Stopping BokslStock2 Application"
  if isrunning; then
    kill -9 `cat $RUNNING_PID`
    rm $RUNNING_PID
  fi
}

status() {
  if isrunning; then
    echoGreen "BokslStock2 Application is running"
  else
    echoRed "BokslStock2 Application is either stopped or inaccessible"
  fi
}

startNotRunning() {
  if isrunning; then
    echoGreen "[$(date +"%F %T")] BokslStock2 Application is running"
  else
    echoRed "[$(date +"%F %T")] BokslStock2 Application is not running. start"
    start
  fi
}

case "$1" in
start)
    start
;;

startNotRunning)
  startNotRunning
;;

console)
    console
;;

status)
   status
   exit 0
;;

stop)
    if isrunning; then
	stop
	exit 0
    else
	echoRed "Application not running"
	exit 3
    fi
;;

restart)
    stop
    start
;;

*)
    echo "Usage: $0 {status|start|startNotRunning|console|stop|restart}"
    exit 1

esac
