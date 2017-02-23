package util;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {
	private static Logger logger=LoggerFactory.getLogger("LightHttpServer");
	
	public static void debug(String message, Exception exception) {
		Date now=new Date();
		logger.debug(String.format("%s %s",now, message),exception);
    }
  
	public static void info(String message) {
    	Date now=new Date();
		logger.info(String.format("%s %s",now, message));
    }
	
    public static void info(String message, Exception exception) {
    	Date now=new Date();
		logger.info(String.format("%s %s",now, message),exception);
    }

    public static void warn(String message, Exception exception) {
    	Date now=new Date();
		logger.warn(String.format("%s %s",now, message),exception);
    }
    
    public static void error(String message, Exception exception) {
    	Date now=new Date();
		logger.error(String.format("%s %s",now, message),exception);
    }
}
