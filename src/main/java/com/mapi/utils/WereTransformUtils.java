package com.mapi.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WereTransformUtils {

    public static int bytes2Int(byte[] src, ByteOrder byteOrder){
        ByteBuffer byteBuffer = ByteBuffer.wrap(src);
        byteBuffer.order(byteOrder);
        return byteBuffer.getShort();
    }

    public static Long bytes2Long(byte[] src, ByteOrder byteOrder){
        ByteBuffer buffer = ByteBuffer.wrap(src);
        buffer.order(byteOrder);
        return buffer.getLong();

    }

    public static int[] bytes2UnsignedIntT(byte[] bytes){
        int[] unsigned = new int[bytes.length];
        for (int i=0;i<bytes.length;i++){
            unsigned[i] = bytes[i] & 0xFF;
        }
        return unsigned;
    }

    public static String pathJoin(String[] strings){
        int n = strings.length;
        StringBuilder sb = new StringBuilder("");
        for(int i=0; i<n;i++){
            sb.append(strings[i] + File.separator);
        }
        return sb.toString();
    }

    public static Object getMaxKey(Map map){
        if (map == null) return "0";
        if (map.size()==0) return "0";
        Set set = map.keySet();

        Object[] obj = set.toArray();

        Arrays.sort(obj);

        return obj[obj.length-1];
    }

    public static String readJson(InputStream is) {
        String json = "";
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
            int line = 1;
            String context = null;
            while ((context = bufferedReader.readLine()) != null) {
                json += context;
                line++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        finally {
            return json;
        }
    }

    public static Map<String, Object> jsonToMap(InputStream is){
        Map<String, Object> result = null;
        String json = readJson(is);
        result = nestJsonToMap(json);
        return result;

    }

    public static Map<String, Object> nestJsonToMap(String json){
        Map<String, Object> result = null;
        try {
            JSONObject jsonObject = new JSONObject(json);

            result = nestJsonObject2Map(jsonObject);

        }catch (JSONException e){
            e.printStackTrace();
        }
        return result;
    }

    public static Object[] nestJsonArray2Array(org.json.JSONArray jsonArray){
        Object[] valueArray = new Object[jsonArray.length()];
        for (int i=0;i<jsonArray.length();i++){
            if (jsonArray.get(i) instanceof JSONObject){
                valueArray[i]  = nestJsonObject2Map((JSONObject) jsonArray.get(i));
            }else if (jsonArray.get(i) instanceof JSONArray){
                valueArray[i] = nestJsonArray2Array((JSONArray) jsonArray.get(i));
            }else {
                valueArray[i] = jsonArray.get(i);
            }
        }
        return valueArray;
    }

    public static Map<String, Object> nestJsonObject2Map(JSONObject jsonObject){
        Map<String, Object> map = new HashMap<>();
        for(Object key : jsonObject.keySet()){
            Object value = jsonObject.get((String) key);
            if (value instanceof JSONObject){
                map.put((String) key, nestJsonObject2Map((JSONObject) value));
            }
            else if (value instanceof org.json.JSONArray){
                Object[] valueArray = nestJsonArray2Array((JSONArray) value);
                map.put((String) key, valueArray);
            }
            else {
                map.put((String) key, value);
            }
        }
        return map;
    }

    public static void main(String[] args) {
        String[] PathList = {System.getProperty("user.dir"), "src","main","resources","gameproto","sgsNew"};
        String casePath = WereTransformUtils.pathJoin(PathList) + "case" + File.separator + "result.json";
        InputStream caseInputStream = null;
        try {
            File file = new File(casePath);
            caseInputStream = new FileInputStream(file);
        }catch (FileNotFoundException exception){
            exception.printStackTrace();
        }
        String caseContent = WereTransformUtils.readJson(caseInputStream);
        Map<String, Object> map = WereTransformUtils.nestJsonToMap(caseContent);
        int i = 1;
    }

}
