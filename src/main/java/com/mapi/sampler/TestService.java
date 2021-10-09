package com.mapi.sampler;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import com.mapi.utils.WereTransformUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestService {

    private static final Logger log = LoggerFactory.getLogger(WerewolfProtoSampler.class);

    private Map<String, Object> caseMap = null;

    public Socket getSocket() {
        return socket;
    }

    private Socket socket = null;

    private String token = "";

    private String userID = "";

    public boolean isOK() {
        return isOK;
    }

    private boolean isOK = true;

    public String getErrorMsg() {
        return errorMsg;
    }

    private String errorMsg = "";

    private boolean error = false;

    private JMeterVariables jmvars = null;

    private int threadNum = -2;

    public String getAccount() {
        return account;
    }

    private String account = "";

    private Deque<Object> q = new LinkedList<>();

    private WereWolfTool tool = new WereWolfTool();

    private void formatLogger(String msg){
        log.info(String.format("threadNum %d, lineno %d: %s, ", this.threadNum, Thread.currentThread().getStackTrace()[2].getLineNumber(), msg));
    }

    public String getCurrentTime(){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(new Date());
    }

    public TestService(JMeterVariables jmvars, int threadNum){
        this.jmvars = jmvars;
        this.threadNum = threadNum;
        String token = this.jmvars.get("token");
        String userID = this.jmvars.get("userID");
        String account = this.jmvars.get("account");
        if (token!=null){
            this.token = token;
        }
        if (userID!=null){
            this.userID = userID;
        }
        if (account!=null){
            this.account = account;
        }
    }

    public void createSocket(String server, int port){
        formatLogger(" 当前时间:" + getCurrentTime());
        LinkedList q = (LinkedList) this.jmvars.getObject("recv_queue");

        if (q!=null){
            formatLogger("get recv_queue from last request");
            this.q = q;
        }else{
            formatLogger("create recv_queue");
            this.q = new LinkedList<>();
            this.jmvars.putObject("recv_queue", q);
        }
        Socket socket = (Socket) this.jmvars.getObject("wereSocket");
//        Socket socket = null;
        if (socket!=null){
            formatLogger("get wereSocket from last request");
            this.socket = socket;
        }else {
            formatLogger("create wereSocket");
            socket = new Socket();
            String host = !StringUtils.isEmpty(server) ? server:"10.225.136.164";
            int sport = port==0 ? 8000:port;
            InetSocketAddress socketAddress = new InetSocketAddress(host, sport);
            try {
                socket.connect(socketAddress);

                socket.setSoTimeout(4*1000);
                this.socket = socket;
                jmvars.putObject("wereSocket", this.socket);
            } catch (SocketException e) {
                formatLogger(e.getMessage());
            } catch (IOException exception) {
                formatLogger(exception.getCause().getMessage());
            }
        }
    }

    public void send(int[] data) throws IOException {

        OutputStream outputStream = socket.getOutputStream();
        for (int i=0;i<data.length;i++){
            outputStream.write(data[i]);
        }
        outputStream.flush();

    }

    public HashMap<String, Object> test(String server, int port, String account, String caseContent) {
        ArrayList<Object> data = null;
        HashMap<String, Object> response_map = new HashMap<>();
        this.createSocket(server, port);

            /**
             打成jar包后启用**/
        String sendTestPath = "/gameproto/werewolf/settings/sendtest.json";
        String recvTestPath = "/gameproto/werewolf/settings/recvtest.json";
        InputStream sendInputStream = this.getClass().getResourceAsStream(sendTestPath);
        InputStream recvInputStream = this.getClass().getResourceAsStream(recvTestPath);


        Map<String, Object> sendjson = WereTransformUtils.jsonToMap(sendInputStream);
        Map<String, Object> recvjson = WereTransformUtils.jsonToMap(recvInputStream);
        login(sendjson, recvjson, account);
        try {
            Thread.sleep(3*1000);
        }catch (InterruptedException e){

        }


        run(sendjson, recvjson, caseContent, account);
        formatLogger("返回值:");
        formatLogger(this.q.toString());
        data = this.verifyData(caseContent);
        for (Object o: data){
            HashMap map = (HashMap) o;
            if (!(boolean) map.get("result")){
                this.isOK = false;
            }
        }
        response_map.put("data", data);
//        this.close();
        return response_map;


    }

    public String preProcess(String caseContent){
        return setVariables(caseContent);
    }

    public String setVariables(String source){
        try {
            Pattern p = Pattern.compile("(\\$\\{)([\\w]+)(\\})");
            Matcher m = p.matcher(source);
            while (m.find()){
                String group = m.group(2);
                String varValue = (String) this.jmvars.get(group);
                if (varValue!=null){
                    source = source.replace("${" + group + "}", varValue);
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return source;
    }

    private boolean isMatchingVariables(Map params){
        // 判断当前的参数中是否有未匹配替换的变量
        String param = JSON.toJSONString(params);
        boolean hasNoMatchingVariable = false;
        Pattern p = Pattern.compile("(\\$\\{)([\\w]+)(\\})");
        Matcher m = p.matcher(param);
        while (m.find()){
            String group = m.group(2);
            String varValue = (String) this.jmvars.get(group);
            if (varValue==null){
                hasNoMatchingVariable = true;
                break;
            }
        }
        return !hasNoMatchingVariable;
    }

    private String getNullVariable(Map params){
        String param = JSON.toJSONString(params);
        String hasNoMatchingVariable = "";
        Pattern p = Pattern.compile("(\\$\\{)([\\w]+)(\\})");
        Matcher m = p.matcher(param);
        while (m.find()){
            String group = m.group(2);
            String varValue = (String) this.jmvars.get(group);
            if (varValue==null){
                hasNoMatchingVariable = "${" + group + "}";
                break;
            }
        }
        return hasNoMatchingVariable;
    }

    public void close(){
        if (this.socket!=null && !this.socket.isClosed()){
            try {
                this.socket.close();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

//    public int[] bytes2NnsignedInt(byte[] bytes){
//        int[] unsigned = new int[bytes.length];
//        for (int i=0;i<bytes.length;i++){
//            unsigned[i] = bytes[i] & 0xFF;
//        }
//        return unsigned;
//    }

    public void login(Map<String, Object> sendjson, Map<String, Object> recvjson, String account){
        /**打成jar后启用*/
        formatLogger(" 登录当前时间:" + getCurrentTime());
         String loginPath = "/gameproto/werewolf/settings/case/login.json";
         InputStream loginInputStream = this.getClass().getResourceAsStream(loginPath);

        String loginJsonContent = WereTransformUtils.readJson(loginInputStream);
        formatLogger(loginJsonContent);
        Map<String, Object> caseMap = WereTransformUtils.nestJsonToMap(loginJsonContent);
        String token= "";
        String userID = "";
        int size = caseMap.size();
        for(int i=1;i<=size;i++){
            Map<String, Object> nowCase = (Map<String, Object>) caseMap.get(new Integer(i).toString());
            String reqName = (String) nowCase.get("reqName");
            String packages = (String) nowCase.get("package");
            Map<String, Object> jsdict = (Map<String, Object>) nowCase.get("data");
            if (jsdict.containsKey("token") && !StringUtils.isEmpty(token)){
                jsdict.put("token", token);
            }
            if (jsdict.containsKey("userID") && !StringUtils.isEmpty(userID)){
                jsdict.put("userID", userID);
            }
            String mname = packages + "." + reqName;
            Map<String, Object> sendPro = (Map<String, Object>) sendjson.get(mname);
            long pid = new Long(sendPro.get("pid").toString());
            try {
                byte[] res = serializeProtoData(sendPro, jsdict);
                byte[] data_bytes = tool.encrypt(res, pid);

                log.info(Integer.toString(data_bytes.length));
                int[] data_ints = WereTransformUtils.bytes2UnsignedIntT(data_bytes);
                try {
                    send(data_ints);
                    InputStream inputStream = socket.getInputStream();
                    byte[] head_data = new byte[2];
                    inputStream.read(head_data);
                    int body_lens = WereTransformUtils.bytes2Int(head_data, ByteOrder.LITTLE_ENDIAN);
                    log.info("数据长度:" + Integer.toString(body_lens));
                    byte[] body_data = new byte[body_lens + 4];
                    inputStream.read(body_data);
                    byte[] bodydatadec = tool.decrypt(body_data);
                    byte[] resp_id_data = Arrays.copyOfRange(bodydatadec, 0, 4);
                    byte[] bodydec = Arrays.copyOfRange(bodydatadec, 4, bodydatadec.length);
                    byte[] full_resp_id_data = new byte[8];
                    System.arraycopy(resp_id_data, 0, full_resp_id_data, 0, resp_id_data.length);
                    long resp_id = WereTransformUtils.bytes2Long(full_resp_id_data, ByteOrder.LITTLE_ENDIAN);

                    Map<String, Object> recvPro = (Map<String, Object>) recvjson.get(new Long(resp_id).toString());
                    Map<String, Object> resultData = deserializationProtoData(recvPro, bodydec);
                    formatLogger(resultData.toString());
                    if (resultData.containsKey("token")) {
                        token = (String) resultData.get("token");
                        this.token = token;
                    }
                    if (resultData.containsKey("userID")) {
                        userID = (String) resultData.get("userID");
                        this.userID = userID;
                    }
                } catch (SocketTimeoutException exception) {
                    // 接收超时大约是服务端无数据发送导致，无需处理
                    exception.printStackTrace();
                } catch (IOException exception) {
                    log.info(exception.getCause().getMessage());
                }
            }catch (InvalidProtocolBufferException exception){
                log.info(exception.getCause().getMessage());
            }
        }
    }

    public void run(Map<String,Object> sendJson, Map<String,Object> recvJson, String caseContent, String account){
        formatLogger(" 执行当前时间:" + getCurrentTime());
        caseContent = preProcess(caseContent);
        Map<String, Object> caseContentMap = WereTransformUtils.nestJsonToMap(caseContent);
        formatLogger("当前case内容:");
        formatLogger(caseContent);
        int num = caseContentMap.size();
        for(int i=1;i<=num;i++){
            Map<String,Object> nowCase = (Map<String, Object>) caseContentMap.get(Integer.toString(i));
            String reqName = (String) nowCase.get("reqName");
            String packages = (String) nowCase.get("package");
            Map<String,Object> jsDict = (Map<String, Object>) nowCase.get("data");

            if (jsDict.containsKey("token") && !StringUtils.isEmpty(token)){
                jsDict.put("token", token);
            }
            if (jsDict.containsKey("userID") && !StringUtils.isEmpty(userID)){
                jsDict.put("userID", userID);
            }
            if (jsDict.containsKey("account")){
                this.account = (String) jsDict.get("account");
                this.jmvars.put("account", this.account);
            }

            String mname = packages + "." + reqName;
            Map<String, Object> sendPro = (Map<String, Object>) sendJson.get(mname);
            formatLogger(sendPro.toString());
            if (sendPro==null){
                this.isOK = false;
                this.errorMsg = String.format("%s未在现有proto协议中,请检查拼写或者联系管理员更新服务器proto协议", mname);
                return;
            }
            long pid = new Long(sendPro.get("pid").toString());
            try {
                byte[] res = serializeProtoData(sendPro, jsDict);
                byte[] data_bytes = tool.encrypt(res, pid);
                int[] data_ints = WereTransformUtils.bytes2UnsignedIntT(data_bytes);
                send(data_ints);
                formatLogger("发送成功");
                recvData(recvJson);
            } catch (SocketException exception){
                formatLogger("发送失败" + exception.getMessage());
                this.error = false;
                this.errorMsg = exception.getMessage();
                Map<String, Object> result = new HashMap<>();
                result.put("mname", null);
                Map respData = new HashMap();
                respData.put("errMsg", this.errorMsg);
                result.put("resp", respData);
                this.q.add(result);
            }
            catch (InvalidProtocolBufferException exception) {
                formatLogger("发送失败" + exception.getCause().getMessage());
                this.isOK = false;
                this.errorMsg = String.format("参数错误,%s", exception.toString());
                return;
            } catch (IOException exception) {
                formatLogger("发送失败" + exception.getCause().getMessage());
                exception.printStackTrace();
            }
        }

    }



    public void recvData(Map<String, Object> recvJson){
        Map<String, Object> resultData = null;
        while (true){
            try {
                InputStream inputStream = socket.getInputStream();
                formatLogger(Integer.toString(inputStream.available()));
                byte[] head_data = new byte[2];
                try {
                    inputStream.read(head_data);
                }catch (SocketTimeoutException exception){
                    // 超时说明数据已读完，跳出接收函数
                    log.info("啥也没有");
                    break;
                }
                formatLogger("开始接收数据");
                int body_lens = WereTransformUtils.bytes2Int(head_data, ByteOrder.LITTLE_ENDIAN);
//                byte[] resp_id_data = new byte[4];
//                inputStream.read(resp_id_data);
//                byte[] full_resp_id_data = new byte[8];
//                System.arraycopy(resp_id_data, 0, full_resp_id_data, 0, resp_id_data.length);
//                long resp_id = TransformUtils.bytes2Long(full_resp_id_data, ByteOrder.LITTLE_ENDIAN);
                byte[] body_data = new byte[body_lens + 4];
                inputStream.read(body_data);
                byte[] resp_data = tool.decrypt(body_data);
                byte[] resp_id_data = Arrays.copyOfRange(resp_data, 0, 4);
                byte[] full_resp_id_data = new byte[8];
                System.arraycopy(resp_id_data, 0, full_resp_id_data, 0, resp_id_data.length);
                long resp_id = WereTransformUtils.bytes2Long(full_resp_id_data, ByteOrder.LITTLE_ENDIAN);
                byte[] respdatadec = Arrays.copyOfRange(resp_data, 4, resp_data.length);
                Map<String, Object> recvPro = null;
                if (recvJson.get(new Long(resp_id).toString()) != null){
                    recvPro = (Map<String, Object>) recvJson.get(new Long(resp_id).toString());
                }
                try {
                    if (recvPro != null) {
                        resultData = deserializationProtoData(recvPro, respdatadec);
                        if (resultData.containsKey("token")) {
                            this.token = (String) resultData.get("token");
                            this.jmvars.put("token", this.token);
                        }
                        if (resultData.containsKey("userID")) {
                            this.userID = (String) resultData.get("userID");
                            this.jmvars.put("userID", this.userID);
                        }
                    }else {
                        recvPro = new HashMap<>();
                        recvPro.put("mname", "Unknow");
                    }
                }
                catch (InvalidProtocolBufferException exception){
                    if (recvPro==null){
                        recvPro = new HashMap<>();
                        recvPro.put("mname", "Unknow");
                    }
                    resultData = new HashMap<>();
                    resultData.put("Expecting", "未匹配到服务端返回的协议id(响应数据序列化异常)，请联系管理员更新proto协议");
                }
                Map<String, Object> result = new HashMap<>();
                result.put("mname", recvPro.get("mname"));
                result.put("resp", resultData);
                this.q.add(result);
            }
            catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }


    public ArrayList<Object> verifyData(String caseContent){
        Map<String, Object> expect = new HashMap<>();
        ArrayList<Object> res = new ArrayList<>();
        Map<String, Object> caseContentMap = WereTransformUtils.nestJsonToMap(caseContent);
        for (String key:caseContentMap.keySet()){
            Map<String, Object> value = (Map<String, Object>) caseContentMap.get(key);
            Map<String, Object> item = new HashMap<>();
            if (value.containsKey("data")){
                item.put("parms", value.get("data"));
            }
            if (value.containsKey("expect")){
                item.put("expect", value.get("expect"));
            }
            if (value.containsKey("reqName")){
                item.put("reqName", value.get("reqName"));
            }
            expect.put(key, item);
        }
        int size = expect.size();
        for(int i=1;i<=size;i++){
            String m = Integer.toString(i);
            Map<String, Object> n = (Map<String, Object>) expect.get(m);
            Map<String, Object> resDict = new HashMap<>();
            Map<String, Object> i_except = (Map<String, Object>) n.get("expect");
            Object respExpectData = null;
            if (i_except.containsKey("data")){
                respExpectData = (Map<String, Object>) i_except.get("data");
            }
            resDict.put("sq", m);
            String reqName = (String) n.get("reqName");
            resDict.put("reqName", reqName);
            resDict.put("respExpectData", respExpectData);
            resDict.put("parms", n.get("parms"));
            if (i_except==null || i_except.size()==0){
                resDict.put("respExpectData", new HashMap<>());
                resDict.put("respName", "");
                resDict.put("respTrueData", new HashMap<>());
                resDict.put("result", true);
                res.add(resDict);
                continue;
            }
            else if (i_except.containsKey("data")){
                respExpectData = (Map<String, Object>) i_except.get("data");
            }
            resDict.put("respExpectData", respExpectData);
            String respName = null;
            if (i_except!=null) {
                respName = (String) i_except.get("respName");
            }
            // 这里有可能是没有respName的，(比如认证或者其他的什么请求，是没有响应名字的）
            if (respName!=null){
                resDict.put("respName", respName);
            }
            else {
                resDict.put("respName", "");
            }
            Map<String, Object> respTrueDataDict = null;
            while (true){
                if (this.q.isEmpty()){
                    break;
                }
                respTrueDataDict = (Map<String, Object>) this.q.peekFirst();
                Map resp = (Map<String, Object>) respTrueDataDict.get("resp");
                this.q.pollFirst();
                if (respName!=null && respName.equals(respTrueDataDict.get("mname"))){
                    break;
                }else if(i_except.keySet().equals(resp.keySet())){
                    break;
                }
                else {
                    continue;
                }
            }
            Object respTrueData = null;
            if (respTrueDataDict!=null && respTrueDataDict.containsKey("resp")){
                if (respName!=null){
                    if (respName.equals(respTrueDataDict.get("mname"))) respTrueData = respTrueDataDict.get("resp");
                }else{
                    respTrueData = respTrueDataDict.get("resp");
                }
            }
            if (respTrueDataDict!=null && respTrueDataDict.containsKey("mname")){
                respName = (String) respTrueDataDict.get("mname");
            }
            if (respTrueData==null){
                resDict.put("result", false);
                resDict.put("respTrueData", "未匹配到服务端返回的协议(期待的响应数据为空)，请检查用例中响应协议的拼写");
                res.add(resDict);
                continue;
            }
            if (!isMatchingVariables((HashMap) n.get("parms"))){
                resDict.put("result", false);
                String nullVariable = getNullVariable((HashMap) n.get("parms"));
                String value = "获取变量:" + nullVariable + "对应的值失败!";
                resDict.put("respTrueData", value);
                res.add(resDict);
                continue;
            }
            if (respExpectData!=null && !respTrueData.getClass().getName().equals(respExpectData.getClass().getName())){
                resDict.put("result", false);
                resDict.put("respTrueData", respTrueData);
                continue;
            }
            if (respExpectData instanceof Map){
                Map<String, Object> respExpectDataMap = (Map<String, Object>) respExpectData;
                Map<String, Object> respTrueDataMap = (Map<String, Object>) respTrueData;
                boolean isBreak = false;
                for (String key:respExpectDataMap.keySet()){
                    Object flag = respTrueDataMap.containsKey(key)? respTrueDataMap.get(key) :"falseSelf";
                    if ((flag.equals("falseSelf") || !flag.toString().equals(respExpectDataMap.get(key).toString())) && !respExpectDataMap.get(key).equals("pass")){
                        resDict.put("result", false);
                        resDict.put("respTrueData", respTrueData);
                        res.add(resDict);
                        isBreak = true;
                        break;
                    }

                }
                if (!isBreak) {
                    // 如果期望与实际完全不同，false
//                    if (respTrueDataMap.containsKey("errMsg")){
//                        resDict.put("result", false);
//                    }else {
//                        resDict.put("result", true);
//                    }
                    resDict.put("result", true);
                    resDict.put("respTrueData", respTrueData);
                    res.add(resDict);
                }
            }
            else if (respExpectData !=null && respExpectData.getClass().isArray()){
                Object[] respExpectDataArray = (Object[]) respExpectData;
                Object[] respTrueDataArray = (Object[]) respTrueData;
                if (respExpectDataArray.length!=respTrueDataArray.length){
                    resDict.put("result", false);
                    resDict.put("respTrueData", respTrueData);
                    res.add(resDict);
                    continue;
                }
                for(int j=0;j<respTrueDataArray.length;j++){
                    boolean flag = false;
                    for(int k=0;k<respExpectDataArray.length;k++){
                        if (respExpectDataArray[k].equals(respTrueDataArray[j])) flag=true;
                    }
                    if (!flag){
                        resDict.put("result", false);
                        resDict.put("respTrueData", respTrueData);
                        res.add(resDict);
                        break;
                    }
                }
                resDict.put("result", true);
                resDict.put("respTrueData", respTrueData);
                res.add(resDict);
            }
            else {
                if (respExpectData!=null && !respExpectData.equals(respTrueData)){
                    resDict.put("result", false);
                    resDict.put("respTrueData", respTrueData);
                    res.add(resDict);
                    continue;
                }else {
                    resDict.put("result", true);
                    resDict.put("respTrueData", respTrueData);
                    resDict.put("respName", respName);
                    if (i_except!=null){
                        resDict.put("respExpectData", i_except);
                    }
                    res.add(resDict);
                }
            }
        }
        return res;
    }


    public byte[] serializeProtoData(Map<String, Object> sendPro, Map<String, Object> jsdict) throws InvalidProtocolBufferException{
        String packname = (String) sendPro.get("packname");
        String packpath = (String) sendPro.get("packpath");
        String mname = (String) sendPro.get("mname");
        try {
            log.info(packpath + "$" + mname);
            Class messageClass = Class.forName(packpath + "$" + mname);
            Class messageBuilder = Class.forName(messageClass.getName()+"$Builder");
            Class messageGrandpa = messageClass.getSuperclass().getSuperclass().getSuperclass();
            Method newBuilder = messageClass.getDeclaredMethod("newBuilder");
            Method toByteArray = messageGrandpa.getDeclaredMethod("toByteArray");
            Method message_build = messageBuilder.getDeclaredMethod("build");
            Object obj = newBuilder.invoke(messageClass);
            String caseContent = JSON.toJSONString(jsdict);
            JsonFormat.parser().merge(caseContent, (Message.Builder) obj);
            Object message_obj = message_build.invoke(obj);
            byte[] data = (byte[]) toByteArray.invoke(message_obj);
            return data;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<String,Object> deserializationProtoData(Map<String, Object> recvPro, byte[] body_data) throws InvalidProtocolBufferException{
        Map<String, Object> resultMap = null;
        String packname = (String) recvPro.get("packname");
        String packpath = (String) recvPro.get("packpath");
        String mname = (String) recvPro.get("mname");
        try{
            log.info(packpath + "$" + mname);
            Class recv_class = Class.forName(packpath + "$" + mname);
            Method parseFrom = recv_class.getDeclaredMethod("parseFrom", ByteBuffer.class);
            ByteBuffer body_data_buffer = ByteBuffer.wrap(body_data);
            Object result = parseFrom.invoke(recv_class, body_data_buffer);
            String resultStr = JsonFormat.printer().print((MessageOrBuilder) result);
            resultMap = WereTransformUtils.nestJsonToMap(resultStr);
            return resultMap;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return resultMap;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return resultMap;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return resultMap;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return resultMap;
        } catch (ClassCastException exception) {
            exception.printStackTrace();
            return resultMap;
        }
    }
}


