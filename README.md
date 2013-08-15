<!--
 |    Copyright © 2013 Konstantin Livitski
 | 
 |    This file is part of JPA PUB. JPA PUB is
 |    licensed under the Apache License, Version 2.0 (the "License");
 |    you may not use this file except in compliance with the License.
 |    You may obtain a copy of the License at
 | 
 |      http://www.apache.org/licenses/LICENSE-2.0
 | 
 |    Unless required by applicable law or agreed to in writing, software
 |    distributed under the License is distributed on an "AS IS" BASIS,
 |    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 |    See the License for the specific language governing permissions and
 |    limitations under the License.
 -->

<a name="sec-about"> </a>
What is JPA PUB?
================
JPA PUB stands for JPA Persistence Unit Builder. Throughout this document,
we call it simply "the PUB". The PUB creates a `META-INF/persistence.xml`
file from `javax.persistence` annotations in your code when you compile
a Java SE project that uses JPA.

The PUB enumerates JPA-managed classes by looking at their annotations
at the compile time and lists those classes in the `persistence.xml` file.
It helps you make a Java SE project compliant with the following
requirement of the [JPA specification][jpa-spec]:

> _To insure the portability of a Java SE application, it is necessary_
> _to explicitly list the managed persistence classes that are included_
> _in the persistence unit using the `class` element of the_
> _`persistence.xml` file._

With the PUB, you can avoid scanning your project's classpath for
`javax.persistence` annotations when building or updating the schema
for your database. Instead, your code may read the `<class>` elements from
the `META-INF/persistence.xml` resource.

<a name="sec-download"> </a>
Downloading the binary
======================

The compiled binary of the PUB is currently available at:

 - <https://github.com/StanLivitski/JPA-PUB/wiki/Download>

<a name="sec-use"> </a>
Using the PUB
=============

The PUB is an _annotation processor_ that works as a plug-in for
the [Java compiler][javac] in Java SE 6 and newer versions. The easiest
way to use it with the compiler is by adding the PUB's `jar` file
to the compiler's classpath. That will work **unless** you supply a
`-processorpath` or `-proc:none` option. 

     javac -cp $CLASSPATH:/opt/jpa-pub/jpa-pub.jar -d build @sources

> *NOTE:*
> Throughout this document, we assume that you have properly configured
> `JAVA_HOME` and `ANT_HOME` environment variables and that you can run `ant`,
> `javac`, and `java` without prefixes on the command line. You may have to
> tweak the examples if you have a different configuration.

If you do not want to make the PUB's classes visible to your source
code or you are using the `-processorpath` option, add the PUB's `jar`
file to `-processorpath`:

     javac -processorpath /opt/jpa-pub/jpa-pub.jar -d build @sources

If you provide an explicit list of annotation processors to the compiler,
use the `name.livitski.tools.jpa.pub.PersistenceUnitBuilder` class name
to activate the PUB. Make sure that class is listed **before** any other
processors that may claim the JPA class annotations.

     javac -processorpath /opt/jpa-pub/jpa-pub.jar:...
      -processor name.livitski.tools.jpa.pub.PersistenceUnitBuilder,...
      -d build @sources

Note that the PUB will not claim any annotations. Thus, any other
processors you use can act upon all annotations in your sources when
the PUB finishes its work.

<a name="sec-use"> </a>
The PUB's parameters
--------------------

The commands shown above run the PUB without any configuration. The tool
will not generate any persistence units unless you configure it as
follows. Standard Java SDK compiler passes options prefixed with
`-A` on its command line to annotation processors.
To name the persistence units you want the PUB to create, pass it the
`jpa.pub.units` option. The value is a comma-separated list of persistence
unit names:

     javac -Ajpa.pub.units=unit1,unit2 ...

You can assign arbitrary names to persistence units as long as each unit
has a unique name within its scope. For Java SE applications, the scope is
usually the application's `jar` file. If you use a package name as a
persistence unit name, managed classes from that package and its descendants
will be included in that persistence unit. This requires no further
configuration. For example:

     javac -Ajpa.pub.units=com.my-domain.project.schema ...

may generate the following `persistence.xml` file for your project:

>     <?xml version="1.0" encoding="UTF-8"?>
>     <!--
>         This file has been generated by the JPA-PUB annotation processor.
>         It may be overwritten the next time this project is compiled.
>     -->
>     <persistence xmlns="http://java.sun.com/xml/ns/persistence"
>      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
>      xsi:schemaLocation="http://java.sun.com/xml/ns/persistence
>       http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd" version="1.0">
>     <persistence-unit name="com.my-domain.project.schema">
>     <class>com.my-domain.project.schema.MappedParent</class>
>     <class>com.my-domain.project.schema.p1.EntityA</class>
>     <class>com.my-domain.project.schema.p2.EntityB</class>
>     <exclude-unlisted-classes/>
>     </persistence-unit>
>     </persistence>

You may want to further customize the generated file by passing
additional options to the PUB. These options apply to specific persistence
units on the `jpa.pub.units` list. Each option is prefixed with
`jpa.pub.unit.` and the name of a persistence unit it applies to.
That name may contain dots. There must be another dot after the unit
name, followed by the option suffix. Known option suffixes are listed
in the table below:

<table border="1" cellspacing="0" width="90%">
<tr>
<th width="30%">Suffix</th>
<th width="70%">Option</th>
</tr>
<tr>
<td><code>package</code></td>
<td>Assigns the package name to a persistence unit. There may be no
more than one unit pointing to any specific package. An empty value
will associate its persistence unit with the default package.
<strong>Do not</strong> use this option with package-named
persistence units.</td>
</tr>
<tr>
<td><code>recursive</code></td>
<td>Tells the PUB whether to treat descendant packages as parts
of the package's persistence unit. Default value of this option
is <strong><code>true</code></strong>.</td>
</tr>
<tr>
<td><code>description</code></td>
<td>Provides a description for the persistence unit. The value will
be copied into the unit's <code>&lt;description&gt;</code> element
as text. The description may contain newline characters. You should
escape those characters properly when passing them to the compiler.
</td>
</tr>
<tr>
<td><code>provider</code></td>
<td>Sets up an explicit provider name for a persistence unit. The
value will be copied into the unit's <code>&lt;provider&gt;</code>
element as text.</td>
</tr>
</table>

Note that for purposes of persistence unit mapping all packages
are considered descendants of the default package. Thus, options

	 -Ajpa.pub.units=default
	 -Ajpa.pub.unit.default.package=

will cause all persistent classes in your project included in the
`default` persistence unit. If you want that unit to contain only
classes from the default package, configure it as a non-recursive
unit:

	 -Ajpa.pub.units=default
	 -Ajpa.pub.unit.default.recursive=false
	 -Ajpa.pub.unit.default.package=

The only elements you can currently customize in `persistence.xml` files
generated by the PUB are `<description>` and `<provider>`. If you want
to customize generated `persistence.xml` files further, please
[contact the project's team](#sec-contact) and request an enhancement,
or submit an update to the PUB's code.

<a name="sec-repo"> </a>
About this repository
=====================

This repository contains the source code of the PUB. Its top-level components are:

        src/           		The PUB's source files
        LICENSE		        Document that describes the project's licensing terms
        NOTICE   	        A summary of license terms that apply to the PUB
        build.xml      		Configuration file for the tool (Ant) that builds
                       		 the PUB's binary
        .classpath     		Eclipse configuration file for the project
        .project       		Eclipse configuration file for the project
        README.md			This document

<a name="sec-building"> </a>
Building the PUB
================

To build the binary from this repository, you need:

   - A **Java SDK**, also known as JDK, Standard Edition (SE), version 6 or
   later, available from OpenJDK <http://openjdk.java.net/> or Oracle
   <http://www.oracle.com/technetwork/java/javase/downloads/index.html>.

   Even though a Java runtime may already be installed on your machine
   (check that by running `java --version`), the build will fail if you
   don't have a complete JDK (check that by running `javac`).

   - **Apache Ant** version 1.7.1 or newer, available from the Apache Software
   Foundation <http://ant.apache.org/>.

To build the PUB, go to the directory containing a working copy of the PUB
and run:

     ant

The result is a file named `jpa-pub.jar` in the same directory. 

<a name="sec-contact"> </a>
Contacting the project's team
=============================

You can send a message to the project's team via the
[Contact page](http://www.livitski.com/contact) at <http://www.livitski.com/>
or via *GitHub*. We will be glad to hear from you!

   [jpa-spec]: http://download.oracle.com/otndocs/jcp/persistence-2_1-fr-eval-spec/index.html
   [javac]: http://www.oracle.com/technetwork/java/javase/tech/javac-137034.html