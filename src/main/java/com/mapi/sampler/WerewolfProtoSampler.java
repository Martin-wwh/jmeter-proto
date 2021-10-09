package com.mapi.sampler;

import com.alibaba.fastjson.JSONObject;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class WerewolfProtoSampler extends AbstractSampler implements Interruptible {

    private static final long serialVersionUID = 240L;

    private static final Logger log = LoggerFactory.getLogger(WerewolfProtoSampler.class);

    public static final String SERVER = "GameProtoSampler.server";

    public static final String PORT = "GameProtoSampler.port";

    public static final String ACCOUNT = "GameProtoSampler.account";

    public static final String CASE_CONTENT = "GameProtoSampler.casecontent";

    public WerewolfProtoSampler() {
        setName("werewolf");
    }

    private String getTitle(){
        return this.getName();
    }

    public void setServer(String newServer) { this.setProperty(SERVER, newServer);}

    public String getServer() { return getPropertyAsString(SERVER); }

    public void setPort(String newPort) { this.setProperty(PORT, newPort, ""); }

    public String getPort() { return getPropertyAsString(PORT,""); }

    public int getPortAsInt() { return getPropertyAsInt(PORT, 0); }

    public void setAccount(String newAccount) { this.setProperty(ACCOUNT, newAccount);}

    public String getAccount() { return getPropertyAsString(ACCOUNT); }

    public void setCaseContent(String newCaseContent) { this.setProperty(CASE_CONTENT, newCaseContent); }

    public String getCaseContent() { return getPropertyAsString(CASE_CONTENT); }


    private boolean isOK = true;

    private String errorMsg = "";

    private transient volatile TestService mapiServer;

    private void formatLogger(String msg){
        log.info(String.format("threadNum %d: %s, ", getThreadContext().getThreadNum(), msg));
    }

    @Override
    public SampleResult sample(Entry entry) {
        SampleResult res = new SampleResult();
        res.setSuccessful(false);
        String server = getServer();
        int port = getPortAsInt();
        String account = getAccount();
        String caseContent = getCaseContent();
        res.setSamplerData(caseContent);
        res.sampleStart();
        int threadNum = getThreadContext().getThreadNum();
        JMeterVariables jmvars = getThreadContext().getVariables();
        mapiServer = new TestService(jmvars, threadNum);
        HashMap data = mapiServer.test(server, port, account, caseContent);
        if (data == null){
            formatLogger("data is null!!!");
        }
        this.isOK = mapiServer.isOK();
        this.errorMsg = mapiServer.getErrorMsg();
        String responseData = JSONObject.toJSONString(data);
        formatLogger(Integer.toString(responseData.length()));
        String label = getName() + "---" + mapiServer.getAccount();
        res.setSampleLabel(label);
        log.info(Integer.toString(responseData.length()));
        res.setResponseData(responseData, null);
        res.setDataType(SampleResult.TEXT);
        res.sampleEnd();
        res.setSuccessful(isOK);
        return res;
    }


    @Override
    public boolean interrupt() {
        TestService server = mapiServer;
        if (server.getSocket()!=null){
//            server.close();
        }
        return !server.getSocket().isClosed();
    }
}
