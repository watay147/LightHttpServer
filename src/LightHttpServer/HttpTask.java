package LightHttpServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.net.FileNameMap;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.KeyStore.Entry;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import util.LogUtil;
import util.StringUtils;
import LightHttpServer.HttpRequest.Method;


public class HttpTask implements Runnable   {
	private static int CGI_ENV_LENGTH=13;
	private Config config;
	private String documentRootDirectoryPath;
	private Socket connectionSocket;
	
	public HttpTask(Config config,Object taskData){
		
		this.config=new Config(config);
		this.documentRootDirectoryPath=config.documentRootDirectoryPath;
		this.connectionSocket= (Socket) taskData;
		
	}

	@Override
	public void run() {
        try   
        {    
        	
        	BufferedReader bufferedReader=new BufferedReader(
        			new InputStreamReader(connectionSocket.getInputStream()));
        	HttpRequest httpRequest=parseRequest(bufferedReader);
			if (httpRequest.shouldServe) {
				OutputStream raw = new BufferedOutputStream(
						connectionSocket.getOutputStream());
				HttpResponse httpResponse = handleRequest(httpRequest);
				handleResponse(httpResponse, raw);
			}
        }
        catch (IOException ex){
        	LogUtil.error(ex.getMessage(), ex);
        }
        finally  
        {  
            try  
            {  
            	connectionSocket.close();          
            }  
            catch (IOException ex) {
            	LogUtil.error(ex.getMessage(), ex);
            }   
        }  
		
	}
	
	
	private HttpRequest parseRequest(BufferedReader in) throws IOException {
		HttpRequest httpRequest=new HttpRequest();
		httpRequest.host=config.host;
		
		
		//read the first line
		String requestLine = in.readLine();
		if(requestLine!=null){
			httpRequest.shouldServe = true;
			StringTokenizer st = new StringTokenizer(requestLine);
			httpRequest.methodString = st.nextToken();
			httpRequest.method = getMethod(httpRequest.methodString);
			httpRequest.requestUrl = st.nextToken();
			// if in GET method, check if need to auto-fill index file path
			if (httpRequest.method == Method.GET) {
				if (httpRequest.requestUrl.equals("/"))
					httpRequest.requestUrl += config.indexFilePath;
			}
			httpRequest.version = st.nextToken();

			httpRequest.headers = getHeaders(in);
			httpRequest.entity = getRequestEntity(in, httpRequest.headers);
		}
		else{
			httpRequest.shouldServe=false;
		}
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
			if (headersLine.contains(":")) {
				String key = headersLine.substring(0, headersLine.indexOf(":"));
				String value = headersLine.substring(
						headersLine.indexOf(":") + 1, headersLine.length())
						.trim();
				// may has duplicated headers, just save and wait for the
				// process later
				if (headers.containsKey(key)) {
					headers.get(key).add(value);
				} else {
					List<String> values = new ArrayList<>();
					values.add(value);
					headers.put(key, values);
				}
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
        else{
        	return null;
        }
        
		return entity;
	}

	
	private HttpResponse handleRequest(HttpRequest httpRequest) throws IOException{
		if(config.CGIAlias!=null&&httpRequest.requestUrl.startsWith(config.CGIAlias)){
			return handleCGIRequest(httpRequest);
		}
		else{
			return handleStaticRequest(httpRequest);
		}
	}
	
	
	private HttpCGIResponse handleCGIRequest(HttpRequest httpRequest) throws IOException{
		HttpCGIResponse httpResponse=new HttpCGIResponse();
		StringBuilder responseLinesBuilder=new StringBuilder();
		StringBuilder entityBuilder=new StringBuilder();
		Date now = new Date();
		responseLinesBuilder.append("Date:"+now+"\r\n");
		responseLinesBuilder.append("Server:"+config.serverVersion+"\r\n");
		String scriptPath=httpRequest.requestUrl.substring(httpRequest.requestUrl.indexOf(config.CGIAlias)+config.CGIAlias.length());
		if(scriptPath.contains("?"))
			scriptPath=scriptPath.substring(0,scriptPath.indexOf("?"));
		File file=new File(config.CGIPath,scriptPath);
		if (file.canRead() &&file.getCanonicalPath().startsWith(config.CGIPath)){
			CGI_ENV_LENGTH=5;//TODO support all environment variables
			String[] envp=new String[CGI_ENV_LENGTH];
			int index=0;
			List<String> emotyList=new ArrayList<>();
			
			envp[index++]="REQUEST_METHOD="+httpRequest.methodString;
			envp[index++]="QUERY_STRING="+(httpRequest.requestUrl.contains("?")?httpRequest.requestUrl.substring(httpRequest.requestUrl.indexOf("?")+1):"");
			envp[index++]="CONTENT_LENGTH="+StringUtils.join(httpRequest.headers.getOrDefault("Content-Length", emotyList), ",");
			envp[index++]="CONTENT_TYPE="+StringUtils.join(httpRequest.headers.getOrDefault("Content-Type", emotyList), ",");
			envp[index++]="HTTP_COOKIE="+(httpRequest.headers.containsKey("Cookie")?StringUtils.join(httpRequest.headers.get("Cookie")," "):"");
			
			
			Process process;
			if(scriptPath.endsWith(".exe")){
				process= Runtime.getRuntime().exec(file.getCanonicalPath(), envp);
				executeCGI(httpResponse, httpRequest, responseLinesBuilder,entityBuilder, process);
			}
			else{
				BufferedReader reader=new BufferedReader(new FileReader(file));
				String comandLine=reader.readLine();
				reader.close();
				if(comandLine.startsWith("#!")&&file.canExecute()){
					comandLine=comandLine.substring(comandLine.indexOf("#!")+2);
					process= Runtime.getRuntime().exec(comandLine+" "+file.getCanonicalPath(), envp);
					executeCGI(httpResponse, httpRequest, responseLinesBuilder,entityBuilder, process);
					
				}
				else{
					String message=generateErrorHtml("500 Internal Error: Not an executable or valid CGI script");
					httpResponse.responseLines=String.format("%s %d %s\r\n%s\r\n\r\n%s", config.httpVersion,
							HttpResponse.HTTP_INTERNAL_ERROR,"Internal Error","Content-type: text/html",message);
		            
				}
			}
			
			
			
			
			
		}
		else{
			httpResponse.responseLines=String.format("%s %d %s\r\n%s\r\n\r\n%s", config.httpVersion,
					HttpResponse.HTTP_NOT_FOUND,"File Not Found","Content-type: text/html",config.html404Content);
            
		}
		
		return httpResponse;
	}
	
	private void executeCGI(HttpCGIResponse httpResponse,HttpRequest httpRequest,StringBuilder responseLinesBuilder,
			StringBuilder entityBuilder, Process process) throws IOException{
		if(httpRequest.method==HttpRequest.Method.POST&&httpRequest.entity!=null){
			DataOutputStream toCGI=new DataOutputStream(process.getOutputStream());
			toCGI.write(httpRequest.entity.body);
			toCGI.flush();//remember to flush
		}
		
		BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
		
		String line = null;
		while ((line = input.readLine()) != null&&!line.isEmpty()) {
			responseLinesBuilder.append(line+"\r\n");
		}
		while ((line = input.readLine()) != null) {
			entityBuilder.append(line);
		}
		process.destroy();
		String entityString=entityBuilder.toString();
		responseLinesBuilder.append("Content-length:"+entityString.getBytes().length+"\r\n");
		responseLinesBuilder.append("\r\n");//an empty to separate headers and entity 
		responseLinesBuilder.append(entityString);
		httpResponse.responseLines=String.format("%s %d %s\r\n%s", config.httpVersion,
				HttpResponse.HTTP_OK,"",responseLinesBuilder.toString());
	}
	
	private HttpStaticResponse handleStaticRequest(HttpRequest httpRequest) throws IOException{
		HttpStaticResponse httpResponse=new HttpStaticResponse();
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
			
			String contentType=null;
			if((contentType=guessContentTypeFromName(httpRequest.requestUrl))==null){
				contentType=URLConnection.guessContentTypeFromStream(fis);
			}
			Date lastModified=new Date(file.lastModified());
			byte[] data = new byte[(int) file.length()];
			fis.readFully(data);
			fis.close();
			if(checkCacheValid(httpRequest,httpResponse,data,lastModified)){
				httpResponse.setStatusLine(config.httpVersion, HttpResponse.HTTP_NOT_MODIFIED, "Not Modified");
			}
			else{
				httpResponse.entity=new HttpEntity();
				httpResponse.entity.contentType=contentType;
				httpResponse.entity.lastModified=lastModified.toGMTString();
				httpResponse.entity.body=data;
				httpResponse.entity.contentLength=file.length();
			
				httpResponse.headers.put("Content-length",new ArrayList<String>());
				httpResponse.headers.get("Content-length").add(httpResponse.entity.contentLength+"");
				httpResponse.headers.put("Content-type",new ArrayList<String>());
				httpResponse.headers.get("Content-type").add(httpResponse.entity.contentType);
			
				addAdditionalHeaders(httpResponse,httpRequest);
			
				httpResponse.setStatusLine(config.httpVersion, HttpResponse.HTTP_OK, "");
			}
		}
		else{
			httpResponse.setStatusLine(config.httpVersion, HttpResponse.HTTP_NOT_FOUND, "File Not Found");
			httpResponse.headers.put("Content-type",new ArrayList<String>());
			httpResponse.headers.get("Content-type").add("text/html");
			httpResponse.entity=new HttpEntity();
			httpResponse.entity.body=config.html404Content.getBytes();
			httpResponse.entity.contentLength=Long.valueOf(httpResponse.entity.body.length);
			httpResponse.headers.put("Content-length",new ArrayList<String>());
			httpResponse.headers.get("Content-length").add(httpResponse.entity.contentLength+"");

            
		}
		Date now = new Date();
		httpResponse.headers.put("Date",new ArrayList<String>());
		httpResponse.headers.get("Date").add(now.toGMTString());
		httpResponse.headers.put("Server",new ArrayList<String>());
		httpResponse.headers.get("Server").add(config.serverVersion);
		
		
		
		
		return httpResponse;
		
	}
	
	
	private boolean checkCacheValid(HttpRequest httpRequest,HttpStaticResponse httpResponse,byte[] data,Date lastModified) {
		boolean flag=false;
		if(httpRequest.headers.containsKey("If-Modified-Since")){
			if(httpRequest.headers.get("If-Modified-Since").get(0).trim().equals(lastModified.toGMTString())){
				List<String> header=new ArrayList<>();
				header.add(lastModified.toGMTString());
				httpResponse.headers.put("Last-Modified", header);
				flag=true;
			}
		}
		else if(httpRequest.headers.containsKey("If-None-Match")){
			String ETag=generateETag(data);
			if(httpRequest.headers.get("If-None-Match").get(0).trim().equals(ETag)){
				List<String> header=new ArrayList<>();
				header.add(ETag);
				httpResponse.headers.put("ETag", header);
				flag=true;
			}
		}
		
		return flag;
	}
	
	private String guessContentTypeFromName(String url) {
		String fileName=url.substring(url.lastIndexOf("/")+1,url.length());
		return URLConnection.guessContentTypeFromName(fileName);
		
	}
	
	private void addAdditionalHeaders(HttpStaticResponse httpResponse,HttpRequest httpRequest) {
		if (config.headersForPathMap != null) {
			// set additional headers according to configured default headers
			// respect to specific paths
			for (Map.Entry<String, Map<String, String>> entry : config.headersForPathMap
					.entrySet()) {
				if (httpRequest.requestUrl.startsWith(entry.getKey())) {
					for (Map.Entry<String, String> subEntry : entry.getValue()
							.entrySet()) {
						if (httpResponse.headers.containsKey(subEntry.getKey())) {
							httpResponse.headers.get(subEntry.getKey()).add(
									subEntry.getValue());
						} else {
							if (subEntry.getKey().equals("Last-Modified")) {
								List<String> header = new ArrayList<>();
								header.add(httpResponse.entity.lastModified);
								httpResponse.headers.put(subEntry.getKey(),
										header);
								continue;
							} else if (subEntry.getKey().equals("ETag")) {
								List<String> header = new ArrayList<>();
								header.add(generateETag(httpResponse.entity.body));
								httpResponse.headers.put(subEntry.getKey(),
										header);
								continue;
							}

							List<String> header = new ArrayList<>();
							header.add(subEntry.getValue());
							httpResponse.headers.put(subEntry.getKey(), header);
						}
					}

				}

			}
		}
	}
	
	private String generateETag(byte[] data) {
		try {
	        MessageDigest md = MessageDigest.getInstance("MD5");  
	        md.update(data);        
	        return new BigInteger(1, md.digest()).toString(16);
	    } catch (Exception ex) {
	    	LogUtil.error(ex.getMessage(), ex);
	    	return "";
	    }
	}

	private void handleResponse(HttpResponse httpResponse,OutputStream raw) throws IOException {
		if (httpResponse instanceof HttpStaticResponse) {
			HttpStaticResponse httpStaticResponse = (HttpStaticResponse) httpResponse;
			Writer out = new OutputStreamWriter(raw);
			out.write(String.format("%s %d %s\r\n", httpStaticResponse.version,
					httpStaticResponse.status, httpStaticResponse.reasonPhrase));
			for (Map.Entry<String, List<String>> entry : httpStaticResponse.headers
					.entrySet()) {
				out.write(String.format("%s: %s\r\n", entry.getKey(),
						StringUtils.join(entry.getValue(), " ")));
			}

			out.write("\r\n");
			out.flush();
			if (httpStaticResponse.entity != null) {
				raw.write(httpStaticResponse.entity.body);
				raw.flush();
			}
		}
		else if(httpResponse instanceof HttpCGIResponse){
			HttpCGIResponse httpCGIResponse = (HttpCGIResponse) httpResponse;
			raw.write(httpCGIResponse.responseLines.getBytes());
			raw.flush();
		}
	}

	private static String generateErrorHtml(String message) {
		return String.format("<html><body><h1>%s</h1></body></html>", message);
	
		
		
	}
	
}
