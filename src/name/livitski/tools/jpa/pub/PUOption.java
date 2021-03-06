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

public abstract class PUOption
{
 public String getValue()
 {
  return null == value ? "" : value;
 }

 public void setValue(String value)
 {
  this.value = value;
 }

 public String getName()
 {
  return name;
 }

 public String getUnit()
 {
  return unit;
 }

 public PUOption(String unit, String name)
 {
  this.unit = unit;
  this.name = name;
 }

 private String unit, name, value;
}