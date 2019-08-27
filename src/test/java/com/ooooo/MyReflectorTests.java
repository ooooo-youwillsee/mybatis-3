package com.ooooo;

import org.apache.ibatis.reflection.ReflectorTest;
import org.junit.Test;

/**
 * @author leizhijie
 * @since 2019-08-26 14:02
 */
public class MyReflectorTests {

  private final ReflectorTest reflectorTest = new ReflectorTest();

  /**
   * setter 测试
   */
  @Test
  public void reflectorTest_shouldResolveSetterParam() {
    reflectorTest.shouldResolveSetterParam();
  }


  /**
   * list 泛型测试
   */
  @Test
  public void reflectorTest_shouldResolveParameterizedSetterParam() {
    reflectorTest.shouldResolveParameterizedSetterParam();
  }


  /**
   * T[] 数组测试
   */
  @Test
  public void reflectorTest_shouldResolveArraySetterParam(){
    reflectorTest.shouldResolveArraySetterParam();
  }

}
