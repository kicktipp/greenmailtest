
https://github.com/greenmail-mail-test/greenmail/issues/942

Reproduce the error mentioned in the issue:
```
./gradlew build
java -XX:MaxDirectMemorySize=1m -jar build/libs/greenmailtest-0.0.1-SNAPSHOT.jar \
numberOfThreads=150 mailsPerThread=5
```
The memory limit is too low for accpeting large quantaties of mail. The paramter 
should not be set, the default is then maximum heap size.

Another bug which seems to be strange. If we do not set MaxDierctMemorySize but use 
150 Threads, the programm never finishes.

```
./gradlew build
java -jar build/libs/greenmailtest-0.0.1-SNAPSHOT.jar numberOfThreads=150 mailsPerThread=1
```



 
