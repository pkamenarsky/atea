#!/bin/bash

if [[ `pwd` =~ .*/(.*) ]]; then
	home=`cd ~; pwd`
	tasks="${home}/Dropbox/times/${BASH_REMATCH[1]}.tasks"
	echo "{:file \"${tasks}\"}" > ~/.atea

	if [[ $1 == -o ]]; then
		open $tasks
	fi
fi

