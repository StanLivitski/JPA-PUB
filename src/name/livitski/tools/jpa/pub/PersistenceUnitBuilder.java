/**
 *  Copyright © 2013 Konstantin Livitski
 *
 *  This file is part of JPA PUB. JPA PUB is
 *  licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package name.livitski.tools.jpa.pub;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

/**
 * Creates a <code>persistence.xml</code> file in accordance with
 * Java SE recommendations of JPA specification. 
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes({"javax.persistence.Entity", "javax.persistence.Embeddable", "javax.persistence.MappedSuperclass"})
public class PersistenceUnitBuilder extends AbstractProcessor
{
 @Override
 public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
 {
  if (roundEnv.errorRaised())
   ; // do noting - we have a problem
  else if (roundEnv.processingOver())
   writeDescriptor();
  else
   for (TypeElement type : annotations)
    gatherTypes(roundEnv.getElementsAnnotatedWith(type));
  return false;
 }

 @Override
 public synchronized void init(ProcessingEnvironment processingEnv)
 {
  super.init(processingEnv);
  messager = processingEnv.getMessager();
  filer = processingEnv.getFiler();
  units = new TreeMap<String, PUData>();
  packages = new HashMap<String, PUData>();
  final Map<String,String> options = processingEnv.getOptions();
  String unitsOption = options.get(OPTION_UNITS);
  if (null == unitsOption)
   messager.printMessage(
     Diagnostic.Kind.WARNING, "JPA PUB annotation processor is missing the '"
     + OPTION_UNITS + "' option. No persistence units will be generated.");
  else
   try
   {
    unitsOption = unitsOption.trim();
    for (String unitName : unitsOption.split("\\s*,\\s*"))
    {
     if (units.containsKey(unitName))
     {
      messager.printMessage(Diagnostic.Kind.WARNING, "Duplicate unit name '"
	 + unitName + "' found on the '" + OPTION_UNITS + "' list.");
      continue;
     }
     final String prefix = OPTION_PREFIX + unitName + '.';
     final PUData unitData = new PUData(unitName);
     for (Class<?> optionClass : UNIT_OPTIONS)
     {
      final Constructor<?> constructor = optionClass.getConstructor(String.class);
      PUOption option = (PUOption)constructor.newInstance(unitName);
      String key = prefix + option.getName();
      if (options.containsKey(key))
      {
       option.setValue(options.get(key));
       unitData.addOption(option);
      }
     }
     final String packageName = unitData.getPackageName();
     final PUData conflicting = packages.get(packageName);
     if (null != conflicting)
     {
      messager.printMessage(Diagnostic.Kind.ERROR, "Package '"
     	 + packageName + "' is aready mapped to persistence unit '"
     	 + conflicting.getName() + ". Cannot map it to '"
	 + unitName + "'.");
      continue;
     }
     units.put(unitName, unitData);
     packages.put(packageName, unitData);
    }
   }
   catch (InvocationTargetException internal)
   {
    throw new RuntimeException("Internal error in JPA PUB annotation processor", internal.getCause());
   }
   catch (Exception internal)
   {
    throw new RuntimeException("Internal error in JPA PUB annotation processor", internal);
   }
 }

 @Override
 public Set<String> getSupportedOptions()
 {
  Set<String> supportedOptions = new HashSet<String>();
  for (Map.Entry<String, PUData> entry : units.entrySet())
  {
   final String prefix = OPTION_PREFIX + entry.getKey() + '.';
   for (PUOption option : entry.getValue().getOptions())
   {
    String key = prefix + option.getName();
    supportedOptions.add(key);
   }
  }
  supportedOptions.add(OPTION_UNITS);
  return supportedOptions;
 }

 public static final String EOL = "\n";

 public static final String OPTION_PREFIX = "jpa.pub.unit.";
 public static final String OPTION_UNITS = "jpa.pub.units";

 public static final String PERSISTENCE_FILE = "META-INF/persistence.xml";
 public static final String PERSISTENCE_XML_VERSION="1.0";
 public static final String PERSISTENCE_FILE_ENCODING = "UTF-8";
 public static final String PERSISTENCE_FILE_COMMENT =
  EOL + "    This file has been generated by the JPA-PUB annotation processor."
  + EOL + "    It may be overwritten the next time this project is compiled."
  + EOL + "    For additional information visit https://github.com/StanLivitski/JPA-PUB"
  + EOL;

 public static final String PERSISTENCE_NS_URI = "http://java.sun.com/xml/ns/persistence";
 public static final String PERSISTENCE_ELEMENT = "persistence";
 public static final String XMLSCHEMA_NS_URI = "http://www.w3.org/2001/XMLSchema-instance";
 public static final String XMLSCHEMA_NS_PREFIX = "xsi";
 public static final String XMLSCHEMA_LOCATION_ATTR = "schemaLocation";
 public static final String PERSISTENCE_XMLSCHEMA_LOCATION = PERSISTENCE_NS_URI
  + " http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd";

 public static final String PERSISTENCE_VERSION="1.0";
 public static final String PERSISTENCE_VERSION_ATTR="version";

 public static final Class<?> UNIT_OPTIONS[] = 
 {
  DescriptionOption.class,
  PackageOption.class,
  RecursiveOption.class,
  ProviderOption.class
 };

 private void gatherTypes(Set<? extends Element> types)
 {
  for (Element type : types)
  {
   if (!ElementKind.CLASS.equals(type.getKind()))
    continue;
   final PackageElement packageElement = packageOf(type);
   final String qName = ((TypeElement)type).getQualifiedName().toString();
   if (null == packageElement)
   {
    messager.printMessage(Diagnostic.Kind.WARNING, "Skipped class "
      + qName 
      + " that is not contained in a package.");
    continue;
   }
   final String packageName = packageElement.getQualifiedName().toString();
   PUData unit = packages.get(packageName);
   if (null == unit)
   {
    for (String sup = superPackage(packageName); null != sup; sup = superPackage(sup))
    {
     unit = packages.get(sup);
     if (null != unit && unit.isRecursive())
      break;
     unit = null;
    }
   }
   if (null == unit)
   {
    messager.printMessage(Diagnostic.Kind.NOTE, "Skipped class "
      + qName 
      + " outside of the configured persistence unit packages.");
    continue;
   }
   unit.addPersistentClass(qName);
  }
 }

 private static String superPackage(String pname)
 {
  if (0 == pname.length())
   return null;
  int at = pname.lastIndexOf('.');
  return 0 > at ? "" : pname.substring(0, at);
 }

 private static PackageElement packageOf(Element element)
 {
  while (null != element && ElementKind.PACKAGE != element.getKind())
   element = element.getEnclosingElement();
  return (PackageElement)element;
 }

 private void writeDescriptor()
 {
  OutputStream resourceOut = null;
  XMLStreamWriter xml = null;
  String resourceName = PERSISTENCE_FILE;
  try
  {
   final FileObject resource = filer.createResource(
     StandardLocation.CLASS_OUTPUT, "", PERSISTENCE_FILE);
   resourceName = resource.getName();
   resourceOut = resource.openOutputStream();
   xml = XMLOutputFactory.newFactory().createXMLStreamWriter(
     resourceOut, PERSISTENCE_FILE_ENCODING);
   xml.writeStartDocument(PERSISTENCE_FILE_ENCODING, PERSISTENCE_XML_VERSION);
   xml.writeCharacters(EOL);
   xml.writeComment(PERSISTENCE_FILE_COMMENT);
   xml.writeCharacters(EOL);
   xml.writeStartElement(PERSISTENCE_ELEMENT);
   xml.writeDefaultNamespace(PERSISTENCE_NS_URI);
   xml.writeNamespace(XMLSCHEMA_NS_PREFIX, XMLSCHEMA_NS_URI);
   xml.writeAttribute(
     XMLSCHEMA_NS_URI,
     XMLSCHEMA_LOCATION_ATTR,
     PERSISTENCE_XMLSCHEMA_LOCATION
     );
   xml.writeAttribute(
     PERSISTENCE_VERSION_ATTR,
     PERSISTENCE_VERSION
     );
   for (PUData unit : units.values())
   {
    unit.writeXMLStream(xml);
    messager.printMessage(Diagnostic.Kind.OTHER,
      "Created descriptor for persistence unit " + unit.getName());
   }
   xml.writeCharacters(EOL);
   xml.writeEndElement();
   xml.writeCharacters(EOL);
   xml.writeEndDocument();
  }
  catch (IOException ioerr)
  {
   messager.printMessage(Diagnostic.Kind.ERROR,
     "Error writing " + resourceName + ": " + ioerr.getLocalizedMessage());
  }
  catch (Exception e)
  {
   messager.printMessage(Diagnostic.Kind.ERROR,
     "Error formatting XML for " + resourceName + ": " + e.getLocalizedMessage());
  }
  finally
  {
   if (null != xml)
    try { xml.close(); }
    catch (Exception err)
    {
     messager.printMessage(Diagnostic.Kind.ERROR,
       "Error closing XML output for " + resourceName + ": " + err.getLocalizedMessage());
    }
   if (null != resourceOut)
    try { resourceOut.close(); }
    catch (Exception err)
    {
     messager.printMessage(Diagnostic.Kind.ERROR,
       "Error closing file " + resourceName + ": " + err.getLocalizedMessage());
    }
  }
 }

 private Map<String, PUData> units;
 private Map<String, PUData> packages;
 private Messager messager;
 private Filer filer;
}
