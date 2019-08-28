/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection.wrapper;

import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * @author Clinton Begin
 */
public interface ObjectWrapper {

  /**
   * 如果封装的是普通的javaBean对象，则调用相应的getter方法
   * 如果封装的是集合类，则获取key对应的值，或者是下标的值
   * @param prop
   * @return
   */
  Object get(PropertyTokenizer prop);

  /**
   * 如果封装的是普通的javaBean对象，则调用相应的setter方法
   * 如果封装的是集合类，则设置key对应的值，或者是下标的值
   * @param prop
   * @param value
   */
  void set(PropertyTokenizer prop, Object value);

  /**
   * 查找属性表达式的属性，
   * @param name
   * @param useCamelCaseMapping 是否忽略属性表达式的下划线
   * @return
   */
  String findProperty(String name, boolean useCamelCaseMapping);

  String[] getGetterNames();

  String[] getSetterNames();

  Class<?> getSetterType(String name);

  Class<?> getGetterType(String name);

  // 有setter方法
  boolean hasSetter(String name);

  // 有getter方法
  boolean hasGetter(String name);

  /**
   * 为属性表达式中的指定属性，创建MetaObject对象
   * @param name
   * @param prop
   * @param objectFactory
   * @return
   */
  MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

  // 是否为Collection集合
  boolean isCollection();

  // 调用collection#add()方法
  void add(Object element);

  // 调用collection#addAll()方法
  <E> void addAll(List<E> element);

}
