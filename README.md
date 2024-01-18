# step-wrapper

Creates an executable that wraps the main script inside step containers to handle logs and status information

## Purpose

This executable is supposed to operate as entry point in docker images used as steps in k-pipe pipelines.
It executes a specified main script/command (passing commandline arguments) as external process and monitors it. 

## Usage



The wrapper performs the following tasks:

 * create directory /workspace/output if it does not exist yet
 * create file /workdir/state with one line "RUNNING"
 * create a log file under /workspace/output/log, the lines of this log file
   are marked with prefixes "OUT> ", "ERROR> ", "WRAPPER> " to indicate the origin of the log line
 * log start time
 * pass output from the process out/err streams to stdout/stderr
 * write lines from the process out/err stream with prefix and optional timestamp to log file
 * write lines from the process err stream to /workdir/output/error (no prefix)
 * run a watchdog thread that checks every minute if the last output of the process is older
   than a specified timeout. If yes, the process is terminated (forcibly if needed).
 * when process has terminated create empty file /workdir/done
 * log termination time
 * write "SUCCESS" or "ERROR" or "KILLED" into file /workdir/state (depending on process exit code and watchdog state)
 * if non zero exit code: log returned exit code
 * return exit code


## Purpose

The usage is as follows:
java -jar [options] LogWrapper.jar [command] [arguments]

A directory "/workspace" must exist (usually this is a volume inside a docker container
that will be mounted from outside the docker container).

The wrapper performs the following tasks:
* create directory /workspace/output if it does not exist yet
* create file /workdir/state with one line "RUNNING"
* create a log file under /workspace/output/log, the lines of this log file
  are marked with prefixes "OUT> ", "ERROR> ", "WRAPPER> " to indicate the origin of the log line
* log start time
* pass output from the process out/err streams to stdout/stderr
* write lines from the process out/err stream with prefix and optional timestamp to log file
* write lines from the process err stream to /workdir/output/error (no prefix)
* run a watchdog thread that checks every minute if the last output of the process is older
  than a specified timeout. If yes, the process is terminated (forcibly if needed).
* when process has terminated create empty file /workdir/done
* log termination time
* write "SUCCESS" or "ERROR" or "KILLED" into file /workdir/state (depending on process exit code and watchdog state)
* if non zero exit code: log returned exit code
* return exit code

The following options may be used:
-DTIME_FORMAT=SECONDS_SINCE_START add relative time stamp (seconds since start of process)
-DTIME_FORMAT=ISO add ISO 8601 absolute time stamp
-DTIME_FORMAT=<pattern> add absolute time stamp in custom format
-DTIMEOUT_MINUTES=<number> the timeout in minutes, process is stopped if no output to stdout/stderr occurs for this period

