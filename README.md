pcml2java-maven-plugin
======================

Generates Java classes from IBM® .PCML-Files (Program Call Markup Language).

## Basic Usage

Include following plugin-block in the pom.xml of your project. Define a sourceFolder where your PCML-Files are located and a packageName for the generated classes.

```
<build>
	<plugins>
		<plugin>
			<groupId>de.twimbee</groupId>
			<artifactId>pcml2java-maven-plugin</artifactId>
			<version>0.0.1-SNAPSHOT</version>
			<configuration>
				<sourceFolder>src/main/resources</sourceFolder>
				<packageName>de.stefanerichsen.pcmlbeans</packageName>
			</configuration>
			<executions>
				<execution>
					<goals>
						<goal>gensrc</goal>
					</goals>
				</execution>
			</executions>
		</plugin>
	</plugins>
</build>
```