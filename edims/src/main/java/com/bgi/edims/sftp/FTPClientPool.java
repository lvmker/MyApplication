package com.bgi.edims.sftp;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;


public class FTPClientPool implements ObjectPool<PooledObject<FTPClient>>{
 private static final int DEFAULT_POOL_SIZE = 10;
 private final BlockingQueue<PooledObject<FTPClient>> pool;
 private final FTPClientFactory factory;

 /**
 * 初始化连接池，需要注入一个工厂来提供FTPClient实例
 * @param factory
 * @throws Exception
 */
 public FTPClientPool(FTPClientFactory factory) throws Exception{
      this(DEFAULT_POOL_SIZE, factory);
 }
 /**
 *
 * @param maxPoolSize      
 * @param factory
 * @throws Exception
 */
 public FTPClientPool(int poolSize, FTPClientFactory factory) throws Exception {
      this.factory = factory;
      pool = new ArrayBlockingQueue<PooledObject<FTPClient>>(poolSize*2);
      initPool(poolSize);
 }
 /**
 * 初始化连接池，需要注入一个工厂来提供FTPClient实例
 * @param maxPoolSize
 * @throws Exception
 */
 private void initPool(int maxPoolSize) throws Exception {
      for(int i=0;i<maxPoolSize;i++){
           //往池中添加对象
           addObject();
      }

 }
 /* (non-Javadoc)
 * @see org.apache.commons.pool.ObjectPool#borrowObject()
 */
 public PooledObject<FTPClient> borrowObject(){
	 try {
		 PooledObject<FTPClient> client = pool.take();
	      if (client == null) {
	           client = factory.makeObject();
	           addObject();
	      }else if(!factory.validateObject(client)){//验证不通过
	           //使对象在池中失效
	           invalidateObject(client);
	           //制造并添加新对象到池中
	           client = factory.makeObject();
	           addObject();
	      }
	      return client;
	} catch (Exception e) {
		// TODO: handle exception
		e.printStackTrace();
	}
	 return null;

 }

 /* (non-Javadoc)
 * @see org.apache.commons.pool.ObjectPool#returnObject(java.lang.Object)
 */
 public void returnObject(PooledObject<FTPClient> client){
	 try {
	      if ((client != null) && !pool.offer(client,3,TimeUnit.SECONDS)) {
	           try {
	                factory.destroyObject(client);
	           } catch (IOException e) {
	                e.printStackTrace();
	           }
	      }
	} catch (Exception e) {
		// TODO: handle exception
		e.printStackTrace();
	}

 }

 public void invalidateObject(PooledObject<FTPClient> client) throws Exception {
      //移除无效的客户端
      pool.remove(client);
 }

 /* (non-Javadoc)
 * @see org.apache.commons.pool.ObjectPool#addObject()
 */
 public void addObject() throws Exception, IllegalStateException, UnsupportedOperationException {
      //插入对象到队列
      pool.offer(factory.makeObject(),3,TimeUnit.SECONDS);
 }

 public int getNumIdle() throws UnsupportedOperationException {
      return 0;
 }

 public int getNumActive() throws UnsupportedOperationException {
      return 0;
 }

 public void clear() throws Exception, UnsupportedOperationException {

 }

 /* (non-Javadoc)
 * @see org.apache.commons.pool.ObjectPool#close()
 */
 public void close() {
	 try {
	      while(pool.iterator().hasNext()){
	    	  PooledObject<FTPClient> client = pool.take();
	           factory.destroyObject(client);
	      }
	} catch (Exception e) {
		// TODO: handle exception
	}

 }

 public void setFactory(PooledObjectFactory<FTPClient> factory) throws IllegalStateException, UnsupportedOperationException {

 }


}