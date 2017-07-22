# Java 9 G1 Log streamer

A start at parsing the new log format for Java 9 and exposing metrics 
basic HTTP endpoint.

Enable GC logging (only works for Java 9) by adding the following lines:

```
-Xlog:gc*:file=gc.log

```

## Running

To build:

```
sbt assembly
```

To run:

```
java -jar target/scala-2.12/gc-log-parser-assembly-0.1.0-SNAPSHOT.jar
```

It expects a `gc.log` in the CWD.


## Unparsed lines

This is a WIP if you see a log like like:

```xslt
[WARN] [07/22/2017 10:50:45.070] [GCParser-akka.actor.default-dispatcher-8] [akka://GCParser/user/$d] Line unparsed, please report bug: [86.375s][info][gc,marking     ] GC(29) Concurrent Mark From Roots
```

Please raise a PR

## Details



