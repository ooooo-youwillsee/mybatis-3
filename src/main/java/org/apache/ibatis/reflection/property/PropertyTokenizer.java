/**
 *    Copyright 2009-2017 the original author or authors.
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
package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * @author Clinton Begin
 *
 * 这个类用来解析xml中标签<result property=”orders[0].items[0].name” column=”item1”/>
 * 中的属性orders[O].items[O].name 表达式,,
 * 注意这个类试实现了Iterator，所以调用next()能处理多层嵌套表达式
 */
public class PropertyTokenizer implements Iterator<PropertyTokenizer> {

  // 当前表达式的名称
  private String name;
  // 当前表达式的索引名
  private final String indexedName;
  // 索引
  private String index;
  // 子表达式
  private final String children;

  public PropertyTokenizer(String fullname) {
    /*
    * 传入fullname参数，例如 orders[O].items[O].name
    * */
    // 获得'.'的索引
    int delim = fullname.indexOf('.');
    if (delim > -1) {
      // 存在'.'，获得name -=> orders[0]
      name = fullname.substring(0, delim);
      // 获得children --> items[O].name
      children = fullname.substring(delim + 1);
    } else {
      // 不存在'.'，name就是fullname，而children不存在
      name = fullname;
      children = null;
    }
    // indexedName --> orders[0]
    indexedName = name;
    // 获得'['的索引
    delim = name.indexOf('[');
    if (delim > -1) {
      // 存在'[', 获得index --> 0
      index = name.substring(delim + 1, name.length() - 1);
      // 获得name --> orders
      name = name.substring(0, delim);
    }
  }

  public String getName() {
    return name;
  }

  public String getIndex() {
    return index;
  }

  public String getIndexedName() {
    return indexedName;
  }

  public String getChildren() {
    return children;
  }

  @Override
  public boolean hasNext() {
    return children != null;
  }

  @Override
  public PropertyTokenizer next() {
    // 这里传入的children (子表达式)
    return new PropertyTokenizer(children);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
  }
}
