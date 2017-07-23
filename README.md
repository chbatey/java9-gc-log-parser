# Java 9 G1 Log streamer

A start at parsing the new log format for Java 9 and exposing metrics 
basic HTTP endpoint.

Enable GC logging (only works for Java 9) by adding the following lines:

```
-Xlog:gc*:file=gc.log

```

Uses akka streams and actors stream from a log file and then watch it for new GC events. Very basic so far as just prints out:

```
{"fullGcs":0,"youngGcs":1,"heapSize":{"size":7,"total":256}}
{"fullGcs":0,"youngGcs":2,"heapSize":{"size":28,"total":256}}
{"fullGcs":0,"youngGcs":3,"heapSize":{"size":62,"total":256}}
{"fullGcs":0,"youngGcs":4,"heapSize":{"size":97,"total":256}}
{"fullGcs":0,"youngGcs":5,"heapSize":{"size":101,"total":256}}
{"fullGcs":0,"youngGcs":6,"heapSize":{"size":159,"total":256}}
{"fullGcs":0,"youngGcs":7,"heapSize":{"size":150,"total":256}}
{"fullGcs":0,"youngGcs":8,"heapSize":{"size":194,"total":256}}
{"fullGcs":0,"youngGcs":9,"heapSize":{"size":222,"total":256}}
{"fullGcs":0,"youngGcs":10,"heapSize":{"size":255,"total":256}}
{"fullGcs":1,"youngGcs":10,"heapSize":{"size":228,"total":256}}
{"fullGcs":2,"youngGcs":10,"heapSize":{"size":240,"total":256}}
{"fullGcs":2,"youngGcs":11,"heapSize":{"size":248,"total":256}}
{"fullGcs":3,"youngGcs":11,"heapSize":{"size":247,"total":256}}
{"fullGcs":4,"youngGcs":11,"heapSize":{"size":247,"total":256}}
{"fullGcs":4,"youngGcs":12,"heapSize":{"size":247,"total":256}}
{"fullGcs":5,"youngGcs":12,"heapSize":{"size":4,"total":14}}
```

Next plan is to include more log events to show allocation rate, promotion rate and to stream over HTTP using server side events
to allow real time graphing.

## Running

To build:

```
sbt assembly
```

To run:

```
java -jar target/scala-2.12/gc-log-parser-assembly-0.1.0-SNAPSHOT.jar
```

It expects a `gc.log` in the CWD. CL args are coming :)


