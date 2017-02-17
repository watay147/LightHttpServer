package LightHttpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


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
				config.keepAliveTime,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());//TODO Tune it to best
		try{
			socket=new ServerSocket(config.port);
		}
		catch (IOException ex){
			System.out.println("Error: "+ex.toString());
			ex.printStackTrace();
			//TODO log
			System.exit(-1);
		}
		
	}
	public void start() {
		 while (true){
			 try{
				 Socket requestSocket = socket.accept();  
				 threadPool.execute(new HttpTask(config, requestSocket));
			 }
			 catch (IOException ex) {  
				//TODO log
	         } 
		 }
	}
	
	
}
