/**
 *    Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.scripting.xmltags;

/**
 * @author Clinton Begin
 *
 * 解析动态节点的接口
 */
public interface SqlNode {

  /**
   * apply方法会解析该SqlNode所记录的动态SQL节点，
   * 并调用DynamicContext.appendSql()方法将解析后的SQL片段追加到DynamicContext.sqlBuilder中
   * @param context 用户传入参数上下文
   * @return
   */
  boolean apply(DynamicContext context);
}
