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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static name.livitski.tools.jpa.pub.PersistenceUnitBuilder.EOL;

/**
 * Contains the data about a persistence unit assembled during
 * annotation processing.
 */
public class PUData
{
 public String getName()
 {
  return name;
 }

 public String getPackageName()
 {
  final PUOption packageOption = options.get(PackageOption.class);
  return null == packageOption ? name : packageOption.getValue();
 }

 public boolean isRecursive()
 {
  final RecursiveOption option = (RecursiveOption)options.get(RecursiveOption.class);
  return null == option ? true : option.isRecursive();
 }

 public Iterable<PUOption> getOptions()
 {
  return Collections.unmodifiableCollection(options.values());
 }

 public void addOption(PUOption option)
 {
  options.put(option.getClass(), option);
 }

 public void addPersistentClass(String className)
 {
  classNames.add(className);
 }

 public Iterable<String> getClassNames()
 {
  return Collections.unmodifiableSet(classNames);
 }

 public void writeXMLStream(XMLStreamWriter xml)
  throws XMLStreamException
 {
  xml.writeCharacters(EOL);
  xml.writeStartElement(PERSISTENCE_UNIT_ELEMENT);
  xml.writeAttribute(NAME_ATTR, name);
  if (options.containsKey(DescriptionOption.class))
  {
   xml.writeCharacters(EOL);
   xml.writeStartElement(DESCRIPTION_ELEMENT);
   xml.writeCharacters(EOL);
   xml.writeCharacters(options.get(DescriptionOption.class).getValue());
   xml.writeCharacters(EOL);
   xml.writeEndElement();
  }
  if (options.containsKey(ProviderOption.class))
  {
   xml.writeCharacters(EOL);
   xml.writeStartElement(PROVIDER_ELEMENT);
   xml.writeCharacters(options.get(ProviderOption.class).getValue());
   xml.writeEndElement();
  }
  for (String qName : classNames)
  {
   xml.writeCharacters(EOL);
   xml.writeStartElement(CLASS_ELEMENT);
   xml.writeCharacters(qName);
   xml.writeEndElement();
  }
  xml.writeCharacters(EOL);
  xml.writeEmptyElement(EXCLUDE_UNLISTED_CLASSES_ELEMENT);
  xml.writeCharacters(EOL);
  xml.writeEndElement();
 }

 public PUData(String name)
 {
  this.name = name;
 }

 public static final String PERSISTENCE_UNIT_ELEMENT = "persistence-unit";
 public static final String NAME_ATTR = "name";
 public static final String DESCRIPTION_ELEMENT = "description";
 public static final String PROVIDER_ELEMENT = "provider";
 public static final String CLASS_ELEMENT = "class";
 public static final String EXCLUDE_UNLISTED_CLASSES_ELEMENT = "exclude-unlisted-classes";

 private Map<Class<? extends PUOption>, PUOption> options = 
  new HashMap<Class<? extends PUOption>, PUOption>();
 private Set<String> classNames = new TreeSet<String>();
 private String name;
}
