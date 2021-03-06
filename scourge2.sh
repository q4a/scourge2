JAVA=java
OS=`uname`
case "${OS}" in
	Linux) PLATFORM=linux;;
	Darwin) PLATFORM=macosx; JAVA=/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Commands/java;;
	Solaris) PLATFORM=solaris;;
	*) PLATFORM=windows;;
esac	

CP=bin:scourge2.jar
for jar in lib/*.jar; do
	CP=$CP:$jar
done
for jar in lib/lwjgl/*.jar; do
	CP=$CP:$jar
done
$JAVA -Xms256m -Xmx512m -dsa -da -cp $CP -Djava.library.path=./lib/lwjgl/native/$PLATFORM org.scourge.Main
