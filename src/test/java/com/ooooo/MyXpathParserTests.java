package com.ooooo;

import org.apache.ibatis.parsing.XPathParserTest;
import org.junit.Test;

/**
 * @author leizhijie
 * @since 2019-08-23 13:48
 *
 * xPathParse 测试类， --> 解析xml文件 例如 mapper.xml 和 mybatis-confi.xml
 *
 */
public class MyXpathParserTests {

  private XPathParserTest xPathParserTest = new XPathParserTest();

  @Test
  public void xPathParserTest_constructorWithInputStreamValidationVariablesEntityResolver() throws Exception {
    xPathParserTest.constructorWithInputStreamValidationVariablesEntityResolver();
  }

}
