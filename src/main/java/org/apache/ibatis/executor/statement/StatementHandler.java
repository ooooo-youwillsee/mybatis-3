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
package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.ResultHandler;

/**
 * @author Clinton Begin
 *
 *  Mybatis核心类： 创建Statement对象，为sql语句绑定实参，
 *  执行insert、select、update、delete多种类型的sql语句，批量执行sql语句
 *
 */
public interface StatementHandler {

  // 从connection中获取一个statement
  Statement prepare(Connection connection, Integer transactionTimeout)
      throws SQLException;

  // 给statement绑定实参
  void parameterize(Statement statement)
      throws SQLException;

  // 批量执行
  void batch(Statement statement)
      throws SQLException;

  // update
  int update(Statement statement)
      throws SQLException;

  // query list
  <E> List<E> query(Statement statement, ResultHandler resultHandler)
      throws SQLException;

  // query cursor
  <E> Cursor<E> queryCursor(Statement statement)
      throws SQLException;

  // 获得sql语句
  BoundSql getBoundSql();

  // 参数处理器
  ParameterHandler getParameterHandler();

}
