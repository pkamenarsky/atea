<!-- <img src="https://github.com/pkamenarsky/atea/raw/master/resources/icon_128.png" align="right"> -->

# Atea

[Atea](https://github.com/downloads/pkamenarsky/atea/atea-1.0.3.dmg) is a minimalistic text file based menu bar time tracker for MacOS (get it [here](https://github.com/downloads/pkamenarsky/atea/atea-1.0.3.dmg)).

There are a lot of great task managers out there - [Fogbugz](http://www.fogcreek.com/fogbugz/), [Pivotal](http://www.pivotaltracker.com/), [Lighthouse](http://lighthouseapp.com/) and [Trello](https://trello.com/) among others. So why yet another?

If you are like me and find yourself in a situation where you want to *quickly* write down a task, bug or an idea you just thought of, more often than not you end up opening up your favorite text editor and saving a `TODO.txt` somewhere. At first it contains 3 or 4 entries; then it starts to grow - and you have to invent a custom DSL just so you can track priorities, projects or time.

Even though a text file based system doesn't scale well (or at all) beyond a single person, it has one unbeatable advantage over web-interface based task management tools - locally editing and reordering tasks is *much* easier and faster, especially with editors like [vim](http://www.vim.org/) or [Emacs](http://www.gnu.org/software/emacs/).

## Task entry

Entering a new task in Atea is just as easy as entering a new line in a text file and saving:

![](https://github.com/pkamenarsky/atea/raw/master/doc/screens/1.png)

Add more tasks:

![](https://github.com/pkamenarsky/atea/raw/master/doc/screens/2.png)

Now comes the interesting part; prioritizing something is just an empty line away:

![](https://github.com/pkamenarsky/atea/raw/master/doc/screens/3.png)

But what if the need arises to subdivide tasks into projects (or modules)? Just add an optional `[Project]` in front of a task; no qualifier stands for `[Default]`:

![](https://github.com/pkamenarsky/atea/raw/master/doc/screens/4.png)

Done with something? Just delete it:

![](https://github.com/pkamenarsky/atea/raw/master/doc/screens/5.png)

Completing all tasks of a given priority has the beneficial side-effect of pushing up all other tasks.

Lines starting with a whitespace character are ignored; this allows for easy "note taking":

![](https://github.com/pkamenarsky/atea/raw/master/doc/screens/6.png)

## Time tracking

Tracking time allows you to bill your clients more accurately, improve resource allocation by comparing estimates with actual times spent or just get a clear picture of what you have been doing the last couple of months.

To start working on a task, just click it:

![](https://github.com/pkamenarsky/atea/raw/master/doc/screens/7.png)

When you are done, stop working:

![](https://github.com/pkamenarsky/atea/raw/master/doc/screens/8.png)

If you want you can append an optional estimate to any given task:

    Make something to eat - 5m

Minutes (`m`), hours (`h`) and days (`d`) are supported.

Times and estimates are saved in a separate csv file in plain text; this allows for easy data analysis by [combining](http://reallylongword.org/sedawk/) common Unix tools like [awk](http://www.grymoire.com/Unix/Awk.html) or [sed](http://www.ibm.com/developerworks/linux/library/l-sed1/index.html).

## Configuration & files

Atea automatically creates an `~/.atea` file in the user home folder with the following contents:

    {
	    :file "/Users/.../tasks.txt"
    }

This is where the current task file resides; you can change it to whatever you desire. Tracked times are stored in a file named `...-times.csv`, depending on the main tasks file name.

## Dropbox integration

Since tasks are stored in simple text files, [Dropbox](http://www.dropbox.com/) can be used for backup and synchronization.

For example, you can start working on a task on you laptop, then move over to you main machine and have your tasks along with the current worked time automatically updated there; just set up the configuration file to point to a file in your Dropbox folder.

## Multiple projects

When invoked the `tasks.sh` script takes the name of the current directory and updates the configuration to point to `/Users/.../Dropbox/tasks/<current-dir-name>.tasks`.

You may associate your favorite editor to open all files with the `.tasks` extension; when the `-o` option is supplied the script automatically invokes `open` on the current tasks file, i.e. fires up the associated text editor.

Place `tasks.sh` somewhere in yout path (for example `/usr/share/bin`); then, in order to edit and switch to a Dropbox backed per-project tasks file, just type:

    $ tasks.sh -o

## User contributions

Check out [atea-contrib](https://github.com/pkamenarsky/atea-contrib), a separate repository containing user contributions for Atea.

## Building

If you just want to try out Atea, you can grab the pre-packaged `dmg` file [here](https://github.com/downloads/pkamenarsky/atea/atea-1.0.3.dmg).

In order to build, you'll need [Leiningen](https://github.com/technomancy/leiningen), Clojure's build and dependency tool; first you have to make a standalone jar:

    lein deps
	cp native/libtray.jnilib .
	lein uberjar

At this point you can just start the jar and it will work.

If you want a native app, you'll need to use Apple's Jar Bundler to create `atea.app` out of the generated standalone jar (this can be automated though, patches welcome). In order to hide the dock icon, edit the `atea.app/Contents/Info.plist` file and add the following key:

    <key>LSUIElement</key>
    <string>1</string>

Create a `dmg` folder and place the `atea.app` bundle there:

    mkdir dmg
	mv atea.app dmg
	ln -s /Applications dmg/Applications

Update the `create-dmg` submodule:

    git submodule init
	git submodule update

And then finally run the `make-dmg` script:

    ./make-dmg

This should create a deployable `dmg` file.

## License

Copyright (C) 2012 Philip Kamenarsky

Distributed under the Eclipse Public License, the same as Clojure.
