package LightHttpServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  

import util.LogUtil;


public class Server {

	private volatile static Server server;
	private Config config;
	private ThreadPoolExecutor threadPool;
	private ServerSocket socket;
	
	
	public static Server getServer(){
		if(server==null){
			synchronized (Server.class) {
				if(server==null){
					server=new Server();
				}
			}
		}
		return server;
	}
	
	public void init() {
		
		config=new Config();
		config.loadConfig();
		threadPool=new ThreadPoolExecutor(config.coreNum, 
				config.maxThreadNum,
				config.threadIdleTime,
				TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(config.waitingQueueSize),new ThreadPoolExecutor.AbortPolicy());//TODO Tune it to best
		try{
			
			socket=new ServerSocket(config.port);
			 File docRoot;
			 docRoot = new File(config.documentRootDirectoryPath);
			 if(!docRoot.isDirectory()){
				 throw new IOException("Document root directory path \""+
			 config.documentRootDirectoryPath+"\" is no valid");
			 }
			//have to turn to be OS specifically canonical
			 config.documentRootDirectoryPath=docRoot.getCanonicalPath();
			 
			 File docCGI;
			 docCGI = new File(config.CGIPath);
			 if(!docCGI.isDirectory()){
				 throw new IOException("CGI directory path \""+
						 config.CGIPath+"\" is no valid");
			 }
			 config.CGIPath=docCGI.getCanonicalPath();
			 
		}
		catch (IOException ex){
			
			LogUtil.error(ex.getMessage(), ex);
			System.exit(-1);
		}
		LogUtil.info("Server inited!");
		
	}
	public void start() {
		LogUtil.info("Server is running!");
		 while (true){
			 try{
				 Socket requestSocket = socket.accept();  
				
				 LogUtil.info("Received access from: "+requestSocket.getInetAddress().getHostAddress());
				 threadPool.execute(new HttpTask(config, requestSocket));
			 }
			 catch (IOException ex) {  
				
				 LogUtil.error(ex.getMessage(), ex);
	         }
			 catch (RejectedExecutionException e) {
				// TODO: handle RejectedExecutionException
			}
		 }
	}
	
	
}
