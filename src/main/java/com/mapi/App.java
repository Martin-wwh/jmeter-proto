package com.mapi;

import com.alibaba.fastjson.JSONObject;
import com.mapi.sampler.TestService;
import com.mapi.utils.WereTransformUtils;
import org.apache.jmeter.threads.JMeterVariables;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class App 
{

    public InputStream getInputStream(String path){
        return this.getClass().getResourceAsStream(path);
    }

    public static byte[] hexToBytes(String hex) {
        hex = hex.length() % 2 != 0 ? "0" + hex : hex;

        byte[] b = new byte[hex.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(hex.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }


    public void test(String filepath){
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(filepath);
            BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String lineTxt= null;
            String res = "";
            while ((lineTxt = br.readLine())!=null){
                res += lineTxt;
            }
//            System.out.println(res);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void main1( String[] args )
    {
//        System.out.println(CLibrary.INSTANCE.retsetStates());
        byte namein[] = {-98, -24, 14, -96, 82, 94, 45, -58, 67};
        int nameint = 9;
        byte[] bodydec = Arrays.copyOfRange(namein, 0, 3);
        for (byte b:
             bodydec) {
            System.out.println(b);
        }
    }

    public static void main(String[] args) {
//        String account = "2430";
//        JMeterVariables jMeterVariables = new JMeterVariables();
//        jMeterVariables.put("account", account);
//        TestService server = new TestService(jMeterVariables, 0);
//        String casePath = "/gameproto/werewolf/settings/case/C_ReqChangeCurCardBack.json";
//        InputStream caseInputStream = new App().getInputStream(casePath);
//        String caseContent = WereTransformUtils.readJson(caseInputStream);
//        HashMap data  = server.test("", 0, account, caseContent);
//        System.out.println("aaaaa");
//        System.out.println(JSONObject.toJSONString(data));
        JMeterVariables jMeterVariables = new JMeterVariables();
        int threadNum = (int) Thread.currentThread().getId();
        TestService testService = new TestService(jMeterVariables, threadNum);
        testService.createSocket("", 0);
        String sendTestPath = "/gameproto/werewolf/settings/sendtest.json";
        String recvTestPath = "/gameproto/werewolf/settings/recvtest.json";
        InputStream sendInputStream = new App().getClass().getResourceAsStream(sendTestPath);
        InputStream recvInputStream = new App().getClass().getResourceAsStream(recvTestPath);


        Map<String, Object> sendjson = WereTransformUtils.jsonToMap(sendInputStream);
        Map<String, Object> recvjson = WereTransformUtils.jsonToMap(recvInputStream);
        testService.login(sendjson, recvjson, "");
    }
}
