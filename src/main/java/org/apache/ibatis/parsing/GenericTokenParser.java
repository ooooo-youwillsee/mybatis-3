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
package org.apache.ibatis.parsing;

/**
 * @author Clinton Begin
 */
public class GenericTokenParser {

  private final String openToken;
  private final String closeToken;
  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  public String parse(String text) {
    // text为null，或者为空字符，直接返回""
    if (text == null || text.isEmpty()) {
      return "";
    }
    // search open token
    // 找到第一个'${'
    int start = text.indexOf(openToken);
    if (start == -1) {
      // 不存在，返回原值
      return text;
    }
    // 将文本转为为字节数组
    char[] src = text.toCharArray();
    int offset = 0;
    // 创建StringBuilder对象，用来添加解析完的值
    final StringBuilder builder = new StringBuilder();
    StringBuilder expression = null;

    // 如果存在openToken
    while (start > -1) {
      // start大于0，说明openToken出现的位置不是第一个索引，src[start - 1] == '\\' 这个为true，表示为openToken可能已经被转义
      if (start > 0 && src[start - 1] == '\\') {
        // this open token is escaped. remove the backslash and continue.
        // 不添加转义的字符，这里添加到builder中，是不会被解析的
        builder.append(src, offset, start - offset - 1).append(openToken);
        // 偏移offset，以便下次添加字符
        offset = start + openToken.length();
      } else {
        // found open token. let's search close token.
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        // 没有转义字符，直接添加
        builder.append(src, offset, start - offset);
        // 偏移offset，以便下次添加字符
        offset = start + openToken.length();
        // 从offset的位置开始查找第一个closeToken, 例如 '}'
        int end = text.indexOf(closeToken, offset);
        // 如果存在
        while (end > -1) {
          // 下面这个也是在处理closeToken是否存在转义的情况
          if (end > offset && src[end - 1] == '\\') {
            // this close token is escaped. remove the backslash and continue.
            expression.append(src, offset, end - offset - 1).append(closeToken);
            offset = end + closeToken.length();
            // 继续找下一个closeToken
            end = text.indexOf(closeToken, offset);
          } else {
            expression.append(src, offset, end - offset);
            break;
          }
        }
        // end为-1，表示没有找到closeToken
        if (end == -1) {
          // close token was not found.
          // 这里添加到builder中，是不会被解析的
          builder.append(src, start, src.length - start);
          offset = src.length;
        } else {
          // 找到了closeToken，调用handleToken()来解析占位符
          // 这里调用的是 org.apache.ibatis.parsing.PropertyParser.VariableTokenHandler.handleToken()
          builder.append(handler.handleToken(expression.toString()));
          offset = end + closeToken.length();
        }
      }
      // 处理完一个expression后，继续从offset开始查找下一个openToken的位置
      start = text.indexOf(openToken, offset);
    }
    // start为-1， 表示找不到openToken了，直接添加剩余的字符
    if (offset < src.length) {
      builder.append(src, offset, src.length - offset);
    }
    // 返回结果
    return builder.toString();
  }
}
