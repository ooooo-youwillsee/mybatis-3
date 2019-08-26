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
package org.apache.ibatis.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.ReflectPermission;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ibatis.reflection.invoker.AmbiguousMethodInvoker;
import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.invoker.SetFieldInvoker;
import org.apache.ibatis.reflection.property.PropertyNamer;

/**
 * This class represents a cached set of class definition information that
 * allows for easy mapping between property names and getter/setter methods.
 *
 * @author Clinton Begin
 */
public class Reflector {

  // 反射对应的class类型
  private final Class<?> type;

  // 可读属性的名称集合，可读属性就是存在getter方法的
  private final String[] readablePropertyNames;

  // 可写属性的名称集合，可写属性就是存在setter方法的
  private final String[] writablePropertyNames;

  // 存放所有的setter方法的集合，key --> 属性名， value --> setter方法对应的Invoker对象
  private final Map<String, Invoker> setMethods = new HashMap<>();

  // 存放所有的getter方法的结合，key --> 属性名， value --> getter方法对应的Invoker对象
  private final Map<String, Invoker> getMethods = new HashMap<>();

  // setter方法的参数类型集合
  private final Map<String, Class<?>> setTypes = new HashMap<>();

  // getter方法的返回值类型
  private final Map<String, Class<?>> getTypes = new HashMap<>();

  // 默认的构造器
  private Constructor<?> defaultConstructor;

  // 忽略大小写的属性Map
  private Map<String, String> caseInsensitivePropertyMap = new HashMap<>();

  // Reflector 构造器
  public Reflector(Class<?> clazz) {
    // 反射对应的class
    type = clazz;
    // 添加默认构造器
    addDefaultConstructor(clazz);
    // 添加getter方法
    addGetMethods(clazz);
    // 添加setter方法
    addSetMethods(clazz);
    // 添加field
    addFields(clazz);
    // 从getMethods中获取可读属性名称集合
    readablePropertyNames = getMethods.keySet().toArray(new String[0]);
    // 从setMethods中获取可写属性名称集合
    writablePropertyNames = setMethods.keySet().toArray(new String[0]);
    // 遍历可读属性名称集合，添加到caseInsensitivePropertyMap，统一转为大写
    for (String propName : readablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
    // 遍历可写属性名称集合，添加到caseInsensitivePropertyMap，统一转为大写
    for (String propName : writablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
  }

  private void addDefaultConstructor(Class<?> clazz) {
    // 获得所有声明的构造器
    Constructor<?>[] constructors = clazz.getDeclaredConstructors();
    // 过滤出一个参数类型个数为0的构造器，如果存在就赋值给defaultConstructor属性
    Arrays.stream(constructors).filter(constructor -> constructor.getParameterTypes().length == 0)
      .findAny().ifPresent(constructor -> this.defaultConstructor = constructor);
  }

  private void addGetMethods(Class<?> clazz) {
    // 可能子类和父类都存在这个getter方法
    Map<String, List<Method>> conflictingGetters = new HashMap<>();
    // 获得所有的getter方法，包含本类和父类的
    Method[] methods = getClassMethods(clazz);
    /*
    * 对所有的方法进行过滤，过滤条件为 参数类型个数为0，是getter方法 （就是以get或者is开头的方法）
    * addMethodConflict()  --> 按照属性名称，添加到对应的Method集合，如果子类和父类都存在这个getter方法，那么这两个方法都会存放在List<Method>集合中
    * PropertyNamer.methodToProperty()  --> 获得getter对应的属性名， 例如：getAbc()  --> abc
    * */
    Arrays.stream(methods).filter(m -> m.getParameterTypes().length == 0 && PropertyNamer.isGetter(m.getName()))
      .forEach(m -> addMethodConflict(conflictingGetters, PropertyNamer.methodToProperty(m.getName()), m));
    // 解决冲突getter方法
    resolveGetterConflicts(conflictingGetters);
  }

  private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
    // 遍历
    for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {
      // 定义默认选择的方法，就是List<Method>集合中第一项
      Method winner = null;
      // propName为属性名称
      String propName = entry.getKey();
      boolean isAmbiguous = false;
      // 对每一个method遍历
      for (Method candidate : entry.getValue()) {
        if (winner == null) {
          winner = candidate;
          continue;
        }
        // winner的返回类型
        Class<?> winnerType = winner.getReturnType();
        // 候选方法的返回类型
        Class<?> candidateType = candidate.getReturnType();
        // 返回类型类型一样
        if (candidateType.equals(winnerType)) {
          // 候选类型不为boolean原生类型
          if (!boolean.class.equals(candidateType)) {
            // 标记这个属性的getter方法不确定
            isAmbiguous = true;
            break;
          } else if (candidate.getName().startsWith("is")) {
            // 处理is方法，也就是优先使用Is开头的方法
            winner = candidate;
          }
        } else if (candidateType.isAssignableFrom(winnerType)) {
          // OK getter type is descendant
        } else if (winnerType.isAssignableFrom(candidateType)) {
          // winnerType是candidateType的父类，重新选择method
          winner = candidate;
        } else {
          // 标记这个属性的getter方法不确定
          isAmbiguous = true;
          break;
        }
      }
      // 添加getMethod，就是根据isAmbiguous变量来在创建Invoker对象，然后在添加到getMethods集合和getTypes集合
      addGetMethod(propName, winner, isAmbiguous);
    }
  }

  private void addGetMethod(String name, Method method, boolean isAmbiguous) {
    MethodInvoker invoker = isAmbiguous
        ? new AmbiguousMethodInvoker(method, MessageFormat.format(
            "Illegal overloaded getter method with ambiguous type for property ''{0}'' in class ''{1}''. This breaks the JavaBeans specification and can cause unpredictable results.",
            name, method.getDeclaringClass().getName()))
        : new MethodInvoker(method);
    // 添加到getMethods中，key-->属性名
    getMethods.put(name, invoker);
    // 解析getter方法的返回值类型
    Type returnType = TypeParameterResolver.resolveReturnType(method, type);
    // 添加到getTypes中，key-->属性名
    getTypes.put(name, typeToClass(returnType));
  }

  private void addSetMethods(Class<?> clazz) {
    // conflictingSetters 用来存放冲突的方法， 可能子类和父类都存在该setter方法
    Map<String, List<Method>> conflictingSetters = new HashMap<>();
    // 获得所有的方法
    Method[] methods = getClassMethods(clazz);
    /*
    * 对方法进行过滤，过滤条件 --> 参数类型个数为1，是setter方法（以set开头）
    * PropertyNamer.methodToProperty() --> 获得属性名，例如 setAbc() --> abc
    * */
    Arrays.stream(methods).filter(m -> m.getParameterTypes().length == 1 && PropertyNamer.isSetter(m.getName()))
      .forEach(m -> addMethodConflict(conflictingSetters, PropertyNamer.methodToProperty(m.getName()), m));
    // 解析冲突的setter方法
    resolveSetterConflicts(conflictingSetters);
  }

  private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
    // 该属性名是有效的属性名称
    if (isValidPropertyName(name)) {
      // 如果没有这个属性名称，就创建List<Method>集合，然后来添加改方法，如果存在改属性名称，添加到对应的Method集合中
      List<Method> list = conflictingMethods.computeIfAbsent(name, k -> new ArrayList<>());
      list.add(method);
    }
  }

  private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
    // 遍历conflictingSetters
    for (String propName : conflictingSetters.keySet()) {
      // 获得所有的setter方法
      List<Method> setters = conflictingSetters.get(propName);
      // 在getTypes集合中获得指定的getter方法的返回值类型
      Class<?> getterType = getTypes.get(propName);
      // 判断该属性的getter方法是否是AmbiguousMethodInvoker（不确定的getter方法）
      boolean isGetterAmbiguous = getMethods.get(propName) instanceof AmbiguousMethodInvoker;
      boolean isSetterAmbiguous = false;
      Method match = null;
      // 遍历所有的setter
      for (Method setter : setters) {
        // 如果没有不确定的getter方法，并且setter方法的参数类型是getter的返回值类型，说明找到了，
        if (!isGetterAmbiguous && setter.getParameterTypes()[0].equals(getterType)) {
          // should be the best match
          match = setter;
          break;
        }
        if (!isSetterAmbiguous) {
          // 对每一个setter方法，进行比较，
          match = pickBetterSetter(match, setter, propName);
          isSetterAmbiguous = match == null;
        }
      }
      if (match != null) {
        // 添加setter方法，就是创建Invoker对象，然后添加到setMethods集合和setTypes集合
        addSetMethod(propName, match);
      }
    }
  }

  private Method pickBetterSetter(Method setter1, Method setter2, String property) {
    if (setter1 == null) {
      return setter2;
    }
    Class<?> paramType1 = setter1.getParameterTypes()[0];
    Class<?> paramType2 = setter2.getParameterTypes()[0];
    if (paramType1.isAssignableFrom(paramType2)) {
      return setter2;
    } else if (paramType2.isAssignableFrom(paramType1)) {
      return setter1;
    }
    MethodInvoker invoker = new AmbiguousMethodInvoker(setter1,
        MessageFormat.format(
            "Ambiguous setters defined for property ''{0}'' in class ''{1}'' with types ''{2}'' and ''{3}''.",
            property, setter2.getDeclaringClass().getName(), paramType1.getName(), paramType2.getName()));
    setMethods.put(property, invoker);
    Type[] paramTypes = TypeParameterResolver.resolveParamTypes(setter1, type);
    setTypes.put(property, typeToClass(paramTypes[0]));
    return null;
  }

  private void addSetMethod(String name, Method method) {
    MethodInvoker invoker = new MethodInvoker(method);
    setMethods.put(name, invoker);
    Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
    setTypes.put(name, typeToClass(paramTypes[0]));
  }

  private Class<?> typeToClass(Type src) {
    Class<?> result = null;
    if (src instanceof Class) {
      result = (Class<?>) src;
    } else if (src instanceof ParameterizedType) {
      result = (Class<?>) ((ParameterizedType) src).getRawType();
    } else if (src instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) src).getGenericComponentType();
      if (componentType instanceof Class) {
        result = Array.newInstance((Class<?>) componentType, 0).getClass();
      } else {
        Class<?> componentClass = typeToClass(componentType);
        result = Array.newInstance(componentClass, 0).getClass();
      }
    }
    if (result == null) {
      result = Object.class;
    }
    return result;
  }

  private void addFields(Class<?> clazz) {
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      if (!setMethods.containsKey(field.getName())) {
        // issue #379 - removed the check for final because JDK 1.5 allows
        // modification of final fields through reflection (JSR-133). (JGB)
        // pr #16 - final static can only be set by the classloader
        int modifiers = field.getModifiers();
        if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
          addSetField(field);
        }
      }
      if (!getMethods.containsKey(field.getName())) {
        addGetField(field);
      }
    }
    if (clazz.getSuperclass() != null) {
      addFields(clazz.getSuperclass());
    }
  }

  private void addSetField(Field field) {
    if (isValidPropertyName(field.getName())) {
      setMethods.put(field.getName(), new SetFieldInvoker(field));
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      setTypes.put(field.getName(), typeToClass(fieldType));
    }
  }

  private void addGetField(Field field) {
    if (isValidPropertyName(field.getName())) {
      getMethods.put(field.getName(), new GetFieldInvoker(field));
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      getTypes.put(field.getName(), typeToClass(fieldType));
    }
  }

  private boolean isValidPropertyName(String name) {
    return !(name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name));
  }

  /**
   * This method returns an array containing all methods
   * declared in this class and any superclass.
   * We use this method, instead of the simpler <code>Class.getMethods()</code>,
   * because we want to look for private methods as well.
   *
   * @param clazz The class
   * @return An array containing all methods in this class
   */
  private Method[] getClassMethods(Class<?> clazz) {
    // 这里的uniqueMethods是不会重复的，因为key是方法签名 （returnType#方法名:参数名1,参数名2）
    Map<String, Method> uniqueMethods = new HashMap<>();
    Class<?> currentClass = clazz;
    // 如果当前类不为null，并且也不是Object类型的
    while (currentClass != null && currentClass != Object.class) {
      // 添加方法，
      addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());

      // we also need to look for interface methods -
      // because the class may be abstract
      // 获得当前类的所有接口，遍历添加每一个接口中的方法
      Class<?>[] interfaces = currentClass.getInterfaces();
      for (Class<?> anInterface : interfaces) {
        addUniqueMethods(uniqueMethods, anInterface.getMethods());
      }
      // 获得该类的父类，继续循环添加方法
      currentClass = currentClass.getSuperclass();
    }
    // 获得所有的方法，返回
    Collection<Method> methods = uniqueMethods.values();

    return methods.toArray(new Method[0]);
  }

  private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
    // 对所有的方法遍历
    for (Method currentMethod : methods) {
      // 该方法不是桥接方法， （桥接方法是虚拟机在泛型擦除过程中生成的辅助方法）
      if (!currentMethod.isBridge()) {
        /*
        * 获得方法签名，格式为 returnType#方法名:参数名1,参数名2,参数名3
        * 例如 void#checkPackageAccess:java.lang.ClassLoader,boolean
        * */
        String signature = getSignature(currentMethod);
        // check to see if the method is already known
        // if it is known, then an extended class must have
        // overridden a method
        // 不存在这个函数签名，就添加这个方法
        if (!uniqueMethods.containsKey(signature)) {
          uniqueMethods.put(signature, currentMethod);
        }
      }
    }
  }

  private String getSignature(Method method) {
    StringBuilder sb = new StringBuilder();
    Class<?> returnType = method.getReturnType();
    if (returnType != null) {
      sb.append(returnType.getName()).append('#');
    }
    sb.append(method.getName());
    Class<?>[] parameters = method.getParameterTypes();
    for (int i = 0; i < parameters.length; i++) {
      sb.append(i == 0 ? ':' : ',').append(parameters[i].getName());
    }
    return sb.toString();
  }

  /**
   * Checks whether can control member accessible.
   *
   * @return If can control member accessible, it return {@literal true}
   * @since 3.5.0
   */
  public static boolean canControlMemberAccessible() {
    try {
      SecurityManager securityManager = System.getSecurityManager();
      if (null != securityManager) {
        securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
      }
    } catch (SecurityException e) {
      return false;
    }
    return true;
  }

  /**
   * Gets the name of the class the instance provides information for.
   *
   * @return The class name
   */
  public Class<?> getType() {
    return type;
  }

  public Constructor<?> getDefaultConstructor() {
    if (defaultConstructor != null) {
      return defaultConstructor;
    } else {
      throw new ReflectionException("There is no default constructor for " + type);
    }
  }

  public boolean hasDefaultConstructor() {
    return defaultConstructor != null;
  }

  public Invoker getSetInvoker(String propertyName) {
    Invoker method = setMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
    }
    return method;
  }

  public Invoker getGetInvoker(String propertyName) {
    Invoker method = getMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
    }
    return method;
  }

  /**
   * Gets the type for a property setter.
   *
   * @param propertyName - the name of the property
   * @return The Class of the property setter
   */
  public Class<?> getSetterType(String propertyName) {
    Class<?> clazz = setTypes.get(propertyName);
    if (clazz == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
    }
    return clazz;
  }

  /**
   * Gets the type for a property getter.
   *
   * @param propertyName - the name of the property
   * @return The Class of the property getter
   */
  public Class<?> getGetterType(String propertyName) {
    Class<?> clazz = getTypes.get(propertyName);
    if (clazz == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
    }
    return clazz;
  }

  /**
   * Gets an array of the readable properties for an object.
   *
   * @return The array
   */
  public String[] getGetablePropertyNames() {
    return readablePropertyNames;
  }

  /**
   * Gets an array of the writable properties for an object.
   *
   * @return The array
   */
  public String[] getSetablePropertyNames() {
    return writablePropertyNames;
  }

  /**
   * Check to see if a class has a writable property by name.
   *
   * @param propertyName - the name of the property to check
   * @return True if the object has a writable property by the name
   */
  public boolean hasSetter(String propertyName) {
    return setMethods.keySet().contains(propertyName);
  }

  /**
   * Check to see if a class has a readable property by name.
   *
   * @param propertyName - the name of the property to check
   * @return True if the object has a readable property by the name
   */
  public boolean hasGetter(String propertyName) {
    return getMethods.keySet().contains(propertyName);
  }

  public String findPropertyName(String name) {
    return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
  }
}
