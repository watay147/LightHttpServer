package LightHttpServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import LightHttpServer.HttpRequest.Method;


public class HttpTask implements Runnable   {
	
	private Config config;
	private String documentRootDirectoryPath;
	private Object taskData;  
	
	public HttpTask(Config config,Object taskData){
		this.config=config;
		this.documentRootDirectoryPath=config.documentRootDirectoryPath;
		this.taskData=taskData;
		
	}

	@Override
	public void run() {
		Socket connectionSocket;  
        connectionSocket = (Socket) taskData;   
        try   
        {    
        	OutputStream raw = new BufferedOutputStream(
        			connectionSocket.getOutputStream()
                   );
        	Writer out = new OutputStreamWriter(raw);
        	BufferedReader bufferedReader=new BufferedReader(
        			new InputStreamReader(connectionSocket.getInputStream()));
        	HttpRequest httpRequest=parseRequest(bufferedReader);
        	HttpResponse httpResponse=handleRequest(httpRequest);
        }
        catch (IOException ex){
        	//TODO log
        }
        finally  
        {  
            try  
            {  
            	connectionSocket.close();          
            }  
            catch (IOException ex) {
            	//TODO log
            }   
        }  
		
	}
	
	
	private HttpRequest parseRequest(BufferedReader in) throws IOException {
		HttpRequest httpRequest=new HttpRequest();
		httpRequest.host=config.host;
		
		
		//read the first line
		String requestLine = in.readLine();
    	StringTokenizer st = new StringTokenizer(requestLine);
    	httpRequest.method = getMethod(st.nextToken());
    	httpRequest.requestUrl=st.nextToken();
    	//if in GET method, check if need to auto-fill index file path
    	if(httpRequest.method==Method.GET){
    		if(httpRequest.requestUrl.equals("/"))
    			httpRequest.requestUrl+=config.indexFilePath;
    	}
    	httpRequest.version=st.nextToken();
    
    	
    	httpRequest.headers=getHeaders(in);
    	httpRequest.entity=getEntity(in,httpRequest.headers);
    	
    	return httpRequest;	 
		
	}
	
	private int getMethod(String method){
		if(method.equals("GET"))
			return HttpRequest.Method.GET;
		else if(method.equals("POST"))
			return HttpRequest.Method.POST;
		else if(method.equals("PUT"))
			return HttpRequest.Method.PUT;
		else if(method.equals("DELETE"))
			return HttpRequest.Method.DELETE;
		else if(method.equals("HEAD"))
			return HttpRequest.Method.HEAD;
		else if(method.equals("OPTIONS"))
			return HttpRequest.Method.OPTIONS;
		else if(method.equals("PATCH"))
			return HttpRequest.Method.PATCH;
		else {
			return HttpRequest.Method.PATCH;
		}
		
			
	}

	private Map<String, List<String>> getHeaders(BufferedReader in) throws IOException{
		HashMap<String, List<String>> headers=new HashMap<>();
		String headersLine ;
		
		//the below will also read the \r\n for separating the headers and the entity
		while((headersLine=in.readLine())!=null&&!headersLine.isEmpty()){
			String key=headersLine.substring(0,headersLine.indexOf(":"));
			String value=headersLine.substring(headersLine.indexOf(":")+1,headersLine.length()).trim();
			//may has duplicated headers, just save and wait for the process later
			if(headers.containsKey(key)){
				headers.get(key).add(value);
			}
			else{
				List<String> values=new ArrayList<>();
				values.add(value);
				headers.put(key, values);
			}
		}
		
		return headers;
		
	}

	private HttpEntity getEntity(BufferedReader in,Map<String, List<String>> headers) throws IOException{
		HttpEntity entity=new HttpEntity();
		if(headers.containsKey("Content-Length")){
			entity.contentLength=Long.valueOf(headers.get("Content-Length").get(0).trim());
		}
		else
			entity.contentLength=0L;
		if(headers.containsKey("Content-Type")){
			entity.contentType=headers.get("Content-Type").get(0).trim();
		}
		if(headers.containsKey("Content-Encoding")){
			entity.contentEncoding=headers.get("Content-Encoding").get(0).trim();
		}
		
        if(entity.contentLength>0){
        	StringBuffer sb=new StringBuffer();
            for (int i = 0; i < entity.contentLength; i++) {
                sb.append((char)in.read());
            }
            entity.body=sb.toString().getBytes(Charset.forName("utf8"));
        }
        
		return entity;
	}

	private HttpResponse handleRequest(HttpRequest httpRequest){
		HttpResponse httpResponse=new HttpResponse();
		
		return httpResponse;
		
	}

}
