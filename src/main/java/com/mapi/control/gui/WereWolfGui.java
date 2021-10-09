package com.mapi.control.gui;

import com.mapi.sampler.WerewolfProtoSampler;
import org.apache.jmeter.gui.util.HorizontalPanel;
import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.gui.util.JTextScrollPane;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;

import javax.swing.*;
import java.awt.*;

public class WereWolfGui extends AbstractSamplerGui {

    private static final long serialVersionUID = 240L;

    private JTextField server;

    private JTextField port;

    private JTextField account;

    private JSyntaxTextArea caseContent;

    private boolean displayName = true;

    public WereWolfGui(){
        super();
        init();
    }




    public void init(){
        setLayout(new BorderLayout(0, 5));

        if (displayName){
            setBorder(makeBorder());
            add(makeTitlePanel(), BorderLayout.NORTH);
        }

        // main panel
        VerticalPanel mainPanel = new VerticalPanel();
        JPanel serverPanel = new HorizontalPanel();
        serverPanel.add(createServerPanel(), BorderLayout.CENTER);
        serverPanel.add(getPortPanel(), BorderLayout.EAST);
        mainPanel.add(serverPanel);
        mainPanel.add(createAccountPanel());
        mainPanel.add(createCaseContentPanel());

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createServerPanel() {
        JLabel label = new JLabel(JMeterUtils.getResString("server"));

        server = new JTextField(10);
        label.setLabelFor(server);

        JPanel serverPanel = new JPanel(new BorderLayout(5, 0));
        serverPanel.add(label, BorderLayout.WEST);
        serverPanel.add(server, BorderLayout.CENTER);
        return serverPanel;
    }

    private JPanel getPortPanel() {
        port = new JTextField(4);

        JLabel label = new JLabel(JMeterUtils.getResString("web_server_port"));
        label.setLabelFor(port);

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(label, BorderLayout.WEST);
        panel.add(port, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAccountPanel() {
        JLabel label = new JLabel("account");

        account = new JTextField(10);
        label.setLabelFor(account);

        JPanel accountPanel = new JPanel(new BorderLayout(5, 0));
        accountPanel.add(label, BorderLayout.WEST);
        accountPanel.add(account, BorderLayout.CENTER);
        return accountPanel;
    }

    private JPanel createCaseContentPanel() {
        JLabel label = new JLabel("case_content");

        caseContent = JSyntaxTextArea.getInstance(15, 80);
        caseContent.setLanguage("text");
        label.setLabelFor(caseContent);

        JPanel caseContentPanel = new JPanel(new BorderLayout(5, 0));
        caseContentPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));

        caseContentPanel.add(label, BorderLayout.WEST);
        caseContentPanel.add(JTextScrollPane.getInstance(caseContent), BorderLayout.CENTER);
        return caseContentPanel;
    }

    @Override
    public String getStaticLabel() {
        return "werewolf";
    }

    @Override
    public String getLabelResource() {
        throw new IllegalStateException("This shouldn't be called");
    }

    @Override
    public TestElement createTestElement() {
        WerewolfProtoSampler sampler = new WerewolfProtoSampler();
        modifyTestElement(sampler);
        return sampler;
    }

    @Override
    public void modifyTestElement(TestElement testElement) {
        testElement.clear();
        configureTestElement(testElement);

        testElement.setProperty(WerewolfProtoSampler.SERVER, server.getText());
        testElement.setProperty(WerewolfProtoSampler.PORT, port.getText());
        testElement.setProperty(WerewolfProtoSampler.ACCOUNT, account.getText());
        testElement.setProperty(WerewolfProtoSampler.CASE_CONTENT, caseContent.getText());
    }

    @Override
    public void configure(TestElement testElement){
        super.configure(testElement);
        // jmeter运行后，保存参数，不然执行后，输入框会清空
        server.setText(testElement.getPropertyAsString(WerewolfProtoSampler.SERVER));
        port.setText(testElement.getPropertyAsString(WerewolfProtoSampler.PORT));
        account.setText(testElement.getPropertyAsString(WerewolfProtoSampler.ACCOUNT));
        caseContent.setText(testElement.getPropertyAsString(WerewolfProtoSampler.CASE_CONTENT));
    }

    @Override
    public void clearGui() {
        super.clearGui();

        server.setText("");
        port.setText("");
        account.setText("");
        caseContent.setInitialText("");
    }
}
