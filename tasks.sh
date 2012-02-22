#!/bin/bash

localtask=`ls *.tasks 2> /dev/null | head -n 1`

if [[ -n $localtask ]]; then
	echo "{:file \"`pwd`/${localtask}\"}" > ~/.atea
	exit
fi

if [[ `pwd` =~ .*/(.*) ]]; then
	home=`cd ~; pwd`
	tdir="${home}/Dropbox/tasks"

	mkdir -p $tdir

	# update .atea configuration
	tasks="${tdir}/${BASH_REMATCH[1]}.tasks"
	echo "{:file \"${tasks}\"}" > ~/.atea

	if [[ $1 == -o ]]; then
		touch $tasks
		open $tasks
	fi
fi

