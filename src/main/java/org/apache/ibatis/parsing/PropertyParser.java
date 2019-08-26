/**
 *    Copyright 2009-2016 the original author or authors.
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

import java.util.Properties;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class PropertyParser {

  private static final String KEY_PREFIX = "org.apache.ibatis.parsing.PropertyParser.";
  /**
   * The special property key that indicate whether enable a default value on placeholder.
   * <p>
   *   The default value is {@code false} (indicate disable a default value on placeholder)
   *   If you specify the {@code true}, you can specify key and default value on placeholder (e.g. {@code ${db.username:postgres}}).
   * </p>
   * @since 3.4.2
   */
  public static final String KEY_ENABLE_DEFAULT_VALUE = KEY_PREFIX + "enable-default-value";

  /**
   * The special property key that specify a separator for key and default value on placeholder.
   * <p>
   *   The default separator is {@code ":"}.
   * </p>
   * @since 3.4.2
   */
  public static final String KEY_DEFAULT_VALUE_SEPARATOR = KEY_PREFIX + "default-value-separator";

  private static final String ENABLE_DEFAULT_VALUE = "false";
  private static final String DEFAULT_VALUE_SEPARATOR = ":";

  private PropertyParser() {
    // Prevent Instantiation
  }

  public static String parse(String string, Properties variables) {
    // 根据properties属性来创建VariableTokenHandler
    VariableTokenHandler handler = new VariableTokenHandler(variables);
    // 创建通用的tokenHander，并设置开头是'${',结尾是'}',
    GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
    // 调用parse()方法来解析字符串
    return parser.parse(string);
  }

  private static class VariableTokenHandler implements TokenHandler {
    private final Properties variables;
    private final boolean enableDefaultValue;
    private final String defaultValueSeparator;

    private VariableTokenHandler(Properties variables) {
      this.variables = variables;
      this.enableDefaultValue = Boolean.parseBoolean(getPropertyValue(KEY_ENABLE_DEFAULT_VALUE, ENABLE_DEFAULT_VALUE));
      this.defaultValueSeparator = getPropertyValue(KEY_DEFAULT_VALUE_SEPARATOR, DEFAULT_VALUE_SEPARATOR);
    }

    private String getPropertyValue(String key, String defaultValue) {
      return (variables == null) ? defaultValue : variables.getProperty(key, defaultValue);
    }

    @Override
    public String handleToken(String content) {
      // 如果properties不为null
      if (variables != null) {
        String key = content;
        // 是否启用默认值
        if (enableDefaultValue) {
          // 获得默认的分隔符的索引
          final int separatorIndex = content.indexOf(defaultValueSeparator);
          String defaultValue = null;
          // 如果存在默认分隔符
          if (separatorIndex >= 0) {
            // 获得要解析的key
            key = content.substring(0, separatorIndex);
            // 获得默认值
            defaultValue = content.substring(separatorIndex + defaultValueSeparator.length());
          }
          // 如果默认值不为null
          if (defaultValue != null) {
            // 调用getProperty()方法来获取对应的值，如果对应的值不存在，就是用默认值
            return variables.getProperty(key, defaultValue);
          }
        }
        // 存在key，就直接获取，如果不存在，就直接返回 "${" + content + "}"
        if (variables.containsKey(key)) {
          return variables.getProperty(key);
        }
      }
      // 如果properties为null，就不会解析，直接返回结果
      return "${" + content + "}";
    }
  }

}
