package util;

import java.util.List;

public class StringUtils {

	public static String join(List<String> strs,String splitter) {
        StringBuffer sb = new StringBuffer();
        if(strs.size()==0)
        	return "";
        	
        for(String s:strs){
            sb.append(s);
            sb.append(splitter);
        }
        return sb.toString().substring(0, sb.toString().length()-splitter.length());
    }
}
