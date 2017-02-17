package LightHttpServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import LightHttpServer.HttpRequest.Method;
import StringUtils.StringUtils;


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
        	
        	BufferedReader bufferedReader=new BufferedReader(
        			new InputStreamReader(connectionSocket.getInputStream()));
        	HttpRequest httpRequest=parseRequest(bufferedReader);
        	HttpResponse httpResponse=handleRequest(httpRequest);
        	handleResponse(httpResponse,raw);
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
    	httpRequest.entity=getRequestEntity(in,httpRequest.headers);
    	
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

	private HttpEntity getRequestEntity(BufferedReader in,Map<String, List<String>> headers) throws IOException{
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

	private HttpResponse handleRequest(HttpRequest httpRequest) throws IOException{
		HttpResponse httpResponse=new HttpResponse();
		File file = new File(documentRootDirectoryPath, 
				httpRequest.requestUrl.substring(1,httpRequest.requestUrl.length()));
		if (file.canRead() 
	              // Don't let clients outside the document root
	            		&& file.getCanonicalPath().startsWith(documentRootDirectoryPath)){
			DataInputStream fis = new DataInputStream(
                    new BufferedInputStream(
                     new FileInputStream(file)
                    )
                   );
			byte[] data = new byte[(int) file.length()];
			fis.readFully(data);
			fis.close();
			httpResponse.entity.body=data;
			httpResponse.entity.contentLength=file.length();
			httpResponse.entity.contentType="text/html";//TODO fix type
			
			httpResponse.headers.put("Content-length",new ArrayList<String>());
			httpResponse.headers.get("Content-length").add(httpResponse.entity.contentLength+"");
			httpResponse.headers.put("Content-type",new ArrayList<String>());
			httpResponse.headers.get("Content-type").add(httpResponse.entity.contentType);
			httpResponse.setStatusLine("HTTP/1.0", HttpResponse.HTTP_OK, "");
		}
		else{
			httpResponse.setStatusLine("HTTP/1.0", HttpResponse.HTTP_NOT_FOUND, "File Not Found");
			httpResponse.headers.put("Content-type",new ArrayList<String>());
			httpResponse.headers.get("Content-type").add("text/plain");
            
		}
		Date now = new Date();
		httpResponse.headers.put("Date",new ArrayList<String>());
		httpResponse.headers.get("Date").add(now+"");
		httpResponse.headers.put("Server",new ArrayList<String>());
		httpResponse.headers.get("Server").add(config.serverVersion);
		
		
		
		
		return httpResponse;
		
	}
	
	private void handleResponse(HttpResponse httpResponse,OutputStream raw) throws IOException {
		Writer out = new OutputStreamWriter(raw);
		 out.write(String.format("%s %d %s\r\n", httpResponse.version,httpResponse.status,httpResponse.reasonPhrase));
		 for(Map.Entry<String,List<String>> entry:httpResponse.headers.entrySet()){
			 out.write(String.format("%s: %s\r\n", entry.getKey(),StringUtils.join(entry.getValue(), " ")));
		 }
		
         out.write("\r\n");
         out.flush();
         if(httpResponse.status==HttpResponse.HTTP_OK){
        	 raw.write(httpResponse.entity.body);
        	 raw.flush();
         }
	}

}
