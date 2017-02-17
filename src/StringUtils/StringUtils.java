package StringUtils;

import java.util.List;

public class StringUtils {

	public static String join(List<String> strs,String splitter) {
        StringBuffer sb = new StringBuffer();
        for(String s:strs){
            sb.append(s);
            sb.append(splitter);
        }
        return sb.toString().substring(0, sb.toString().length()-splitter.length());
    }
}
