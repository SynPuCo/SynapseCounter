name=Synapse_Counter
imagej=/opt/Fiji.app # or other relevant location
javac "$name.java" MyParticleAnalyzer.java MyParticleAnalyzer3D.java -cp "$imagej"/jars/*:"$imagej"/plugins/*:. -Xlint:unchecked
jar -cf "$name.jar" $name.class MyParticleAnalyzer.class MyParticleAnalyzer3D.class plugins.config
mv "$name.jar" "$imagej/plugins/"
rm *.class
