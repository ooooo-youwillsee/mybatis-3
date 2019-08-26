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

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.ibatis.builder.BuilderException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 *
 * xpathParse 用于解析xml配置文件
 */
public class XPathParser {

  // xml document对象
  private final Document document;
  // 是否验证
  private boolean validation;
  // xm中实体解析器，加载dtd和schema
  private EntityResolver entityResolver;
  // mybatis-config.xml中标签<properties>的键值对
  private Properties variables;
  // xpath对象，支持xpath查询一个节点
  private XPath xpath;

  public XPathParser(String xml) {
    commonConstructor(false, null, null);
    this.document = createDocument(new InputSource(new StringReader(xml)));
  }

  public XPathParser(Reader reader) {
    commonConstructor(false, null, null);
    this.document = createDocument(new InputSource(reader));
  }

  public XPathParser(InputStream inputStream) {
    commonConstructor(false, null, null);
    this.document = createDocument(new InputSource(inputStream));
  }

  public XPathParser(Document document) {
    commonConstructor(false, null, null);
    this.document = document;
  }

  public XPathParser(String xml, boolean validation) {
    commonConstructor(validation, null, null);
    this.document = createDocument(new InputSource(new StringReader(xml)));
  }

  public XPathParser(Reader reader, boolean validation) {
    commonConstructor(validation, null, null);
    this.document = createDocument(new InputSource(reader));
  }

  public XPathParser(InputStream inputStream, boolean validation) {
    commonConstructor(validation, null, null);
    this.document = createDocument(new InputSource(inputStream));
  }

  public XPathParser(Document document, boolean validation) {
    commonConstructor(validation, null, null);
    this.document = document;
  }

  public XPathParser(String xml, boolean validation, Properties variables) {
    commonConstructor(validation, variables, null);
    this.document = createDocument(new InputSource(new StringReader(xml)));
  }

  public XPathParser(Reader reader, boolean validation, Properties variables) {
    commonConstructor(validation, variables, null);
    this.document = createDocument(new InputSource(reader));
  }

  public XPathParser(InputStream inputStream, boolean validation, Properties variables) {
    commonConstructor(validation, variables, null);
    this.document = createDocument(new InputSource(inputStream));
  }

  public XPathParser(Document document, boolean validation, Properties variables) {
    commonConstructor(validation, variables, null);
    this.document = document;
  }

  public XPathParser(String xml, boolean validation, Properties variables, EntityResolver entityResolver) {
    commonConstructor(validation, variables, entityResolver);
    this.document = createDocument(new InputSource(new StringReader(xml)));
  }

  public XPathParser(Reader reader, boolean validation, Properties variables, EntityResolver entityResolver) {
    commonConstructor(validation, variables, entityResolver);
    this.document = createDocument(new InputSource(reader));
  }

  public XPathParser(InputStream inputStream, boolean validation, Properties variables, EntityResolver entityResolver) {
    // 公共构造器，设置是否验证、properties属性、实体解析器、xpathFactory
    commonConstructor(validation, variables, entityResolver);
    // 根据你传入的InputStream来得到document
    this.document = createDocument(new InputSource(inputStream));
  }

  public XPathParser(Document document, boolean validation, Properties variables, EntityResolver entityResolver) {
    commonConstructor(validation, variables, entityResolver);
    this.document = document;
  }

  public void setVariables(Properties variables) {
    this.variables = variables;
  }

  public String evalString(String expression) {
    // 根据表达式，来解析相应的值
    return evalString(document, expression);
  }

  public String evalString(Object root, String expression) {
    // 解析的返回值类型为XPathConstants.STRING (字符串)
    String result = (String) evaluate(expression, root, XPathConstants.STRING);
    // 调用PropertyParser#parse()方法，这里传入了variables(标签<properties>) --> 会解析占位符
    result = PropertyParser.parse(result, variables);
    return result;
  }

  public Boolean evalBoolean(String expression) {
    return evalBoolean(document, expression);
  }

  public Boolean evalBoolean(Object root, String expression) {
    return (Boolean) evaluate(expression, root, XPathConstants.BOOLEAN);
  }

  public Short evalShort(String expression) {
    return evalShort(document, expression);
  }

  public Short evalShort(Object root, String expression) {
    return Short.valueOf(evalString(root, expression));
  }

  public Integer evalInteger(String expression) {
    return evalInteger(document, expression);
  }

  public Integer evalInteger(Object root, String expression) {
    return Integer.valueOf(evalString(root, expression));
  }

  public Long evalLong(String expression) {
    return evalLong(document, expression);
  }

  public Long evalLong(Object root, String expression) {
    return Long.valueOf(evalString(root, expression));
  }

  public Float evalFloat(String expression) {
    return evalFloat(document, expression);
  }

  public Float evalFloat(Object root, String expression) {
    return Float.valueOf(evalString(root, expression));
  }

  public Double evalDouble(String expression) {
    return evalDouble(document, expression);
  }

  public Double evalDouble(Object root, String expression) {
    return (Double) evaluate(expression, root, XPathConstants.NUMBER);
  }

  public List<XNode> evalNodes(String expression) {
    // 解析xnode节点
    return evalNodes(document, expression);
  }

  public List<XNode> evalNodes(Object root, String expression) {
    List<XNode> xnodes = new ArrayList<>();
    // 解析表达式，返回值类型为NODESET （node的集合）
    NodeList nodes = (NodeList) evaluate(expression, root, XPathConstants.NODESET);
    // 遍历添加到xnodes，返回结果
    for (int i = 0; i < nodes.getLength(); i++) {
      xnodes.add(new XNode(this, nodes.item(i), variables));
    }
    return xnodes;
  }

  public XNode evalNode(String expression) {
    // 根据表达式，解析为xNode对象
    return evalNode(document, expression);
  }

  public XNode evalNode(Object root, String expression) {
    // 解析表达式，返回值类型为Node
    Node node = (Node) evaluate(expression, root, XPathConstants.NODE);
    if (node == null) {
      return null;
    }
    // 创建xNode对象，放入variables（标签<properties>中定义的键值对）
    return new XNode(this, node, variables);
  }

  private Object evaluate(String expression, Object root, QName returnType) {
    try {
      // 这里就是根据xpath来解析的，而xpath在xpathParser实例化过程中进行了默认设置
      return xpath.evaluate(expression, root, returnType);
    } catch (Exception e) {
      throw new BuilderException("Error evaluating XPath.  Cause: " + e, e);
    }
  }

  private Document createDocument(InputSource inputSource) {
    // important: this must only be called AFTER common constructor
    try {
      // 获得DocumentBuilderFactory(文档构建工厂)
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      // 设置是否验证
      factory.setValidating(validation);

      factory.setNamespaceAware(false);
      // 忽略注释
      factory.setIgnoringComments(true);
      factory.setIgnoringElementContentWhitespace(false);
      factory.setCoalescing(false);
      factory.setExpandEntityReferences(true);

      // 从工厂中获得DocumentBuilder对象（文档构建对象）
      DocumentBuilder builder = factory.newDocumentBuilder();
      // 设置实体解析
      builder.setEntityResolver(entityResolver);
      // 设置错误解析器
      builder.setErrorHandler(new ErrorHandler() {
        @Override
        public void error(SAXParseException exception) throws SAXException {
          throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
          throw exception;
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
        }
      });
      // 调用parse()方法，返回一个document对象
      return builder.parse(inputSource);
    } catch (Exception e) {
      throw new BuilderException("Error creating document instance.  Cause: " + e, e);
    }
  }

  private void commonConstructor(boolean validation, Properties variables, EntityResolver entityResolver) {
    this.validation = validation;
    this.entityResolver = entityResolver;
    this.variables = variables;
    // 创建xPathFactory
    XPathFactory factory = XPathFactory.newInstance();
    this.xpath = factory.newXPath();
  }

}
