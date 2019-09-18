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
package org.apache.ibatis.cache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ibatis.cache.Cache;

/**
 * Lru (least recently used) cache decorator.
 *
 *
 * 按照一定规则来清理缓存 --> 最近最少使用
 * @author Clinton Begin
 */
public class LruCache implements Cache {

  // 底层被装饰的cache对象
  private final Cache delegate;
  // 有序的Map， 记录最近使用的key
  private Map<Object, Object> keyMap;
  // 记录最少被使用的key
  private Object eldestKey;

  public LruCache(Cache delegate) {
    this.delegate = delegate;
    setSize(1024);
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  public void setSize(final int size) {
    // 注意第三个参数是accessOrder为true，也就是使用Map#get()方法时，会改变key的顺序
    keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
      private static final long serialVersionUID = 4267176411845948333L;

      // 当调用put()方法，会调用该方法，
      @Override
      protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
        boolean tooBig = size() > size;
        if (tooBig) {
          // 如果缓存达到上限，更新eldestKey，后面会删除该项
          eldestKey = eldest.getKey();
        }
        return tooBig;
      }
    };
  }

  @Override
  public void putObject(Object key, Object value) {
    delegate.putObject(key, value);
    // size达到上限，删除最久未使用的缓存项
    cycleKeyList(key);
  }

  @Override
  public Object getObject(Object key) {
    keyMap.get(key); //touch
    return delegate.getObject(key);
  }

  @Override
  public Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  @Override
  public void clear() {
    delegate.clear();
    keyMap.clear();
  }

  private void cycleKeyList(Object key) {
    keyMap.put(key, key);
    if (eldestKey != null) {
      // eldestKey不为null，说明已经达到最大缓存上限了，则删除这个key的缓存
      delegate.removeObject(eldestKey);
      eldestKey = null;
    }
  }

}
