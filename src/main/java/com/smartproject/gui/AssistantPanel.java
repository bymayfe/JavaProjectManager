package com.smartproject.gui;

import com.smartproject.ai.ProjectAssistant;
import com.smartproject.db.ProjectRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Supplier;

public class AssistantPanel extends JPanel {

    private JTextPane   chatPane;
    private StringBuilder chatHtml;
    private JScrollPane chatScroll;
    private JTextField  txtInput;
    private JButton     btnSend;
    private JLabel      lblStatus;
    private JLabel      lblActiveModel;

    private ProjectAssistant assistant;
    private Supplier<String> serviceSupplier;
    private Supplier<String> apiKeySupplier;
    private Supplier<String> geminiModelSupplier;
    private Supplier<String> gptApiUrlSupplier;
    private Supplier<String> gptModelSupplier;

    public AssistantPanel(ProjectRepository repository, 
                          Supplier<String> serviceSupplier,
                          Supplier<String> apiKeySupplier,
                          Supplier<String> geminiModelSupplier,
                          Supplier<String> gptApiUrlSupplier,
                          Supplier<String> gptModelSupplier) {
        this.assistant           = new ProjectAssistant(repository);
        this.serviceSupplier     = serviceSupplier;
        this.apiKeySupplier      = apiKeySupplier;
        this.geminiModelSupplier = geminiModelSupplier;
        this.gptApiUrlSupplier   = gptApiUrlSupplier;
        this.gptModelSupplier    = gptModelSupplier;

        this.chatHtml            = new StringBuilder();
        
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Baslangic HTML yapisi
        chatHtml.append("<html><body style='font-family: Segoe UI, sans-serif; font-size: 13px; color: #E0E0E0; background-color: #2b2b2b; margin: 0; padding: 10px;'>");
        
        buildUI();
        addWelcomeMessage();
        refreshActiveModel();
    }

    private void buildUI() {
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setOpaque(false);
        northPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

        JLabel lblTitle = new JLabel("Proje Asistani");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        northPanel.add(lblTitle, BorderLayout.WEST);

        lblActiveModel = new JLabel("Aktif Model: Yukleniyor...");
        lblActiveModel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblActiveModel.setForeground(new Color(140, 140, 150));
        northPanel.add(lblActiveModel, BorderLayout.EAST);

        add(northPanel, BorderLayout.NORTH);

        // Mesaj paneli - JTextPane ile HTML destegi
        chatPane = new JTextPane();
        chatPane.setContentType("text/html");
        chatPane.setEditable(false);
        chatPane.setBackground(new Color(43, 43, 43));
        
        chatScroll = new JScrollPane(chatPane);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.setBorder(BorderFactory.createTitledBorder("Sohbet"));
        add(chatScroll, BorderLayout.CENTER);

        // Alt panel
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 0));
        bottomPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

        txtInput = new JTextField();
        txtInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtInput.setToolTipText("Ornek: 'Python ile web scraping projesi ariyorum'");
        txtInput.addActionListener(e -> sendMessage());

        btnSend = new JButton("Sor");
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSend.addActionListener(e -> sendMessage());

        bottomPanel.add(txtInput, BorderLayout.CENTER);
        bottomPanel.add(btnSend,  BorderLayout.EAST);

        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        JPanel southWrapper = new JPanel(new BorderLayout());
        southWrapper.add(lblStatus,    BorderLayout.NORTH);
        southWrapper.add(bottomPanel,  BorderLayout.CENTER);
        add(southWrapper, BorderLayout.SOUTH);
    }

    private void addWelcomeMessage() {
        addAIBubble(
            "Merhaba! Ben Proje Asistaniyim.\n\n" +
            "Taranan projelerinden herhangi birine benzer proje aramak icin bana sorabilirsin.\n\n" +
            "Ornek sorular:\n" +
            "  - Java ile GUI projesi ariyorum\n" +
            "  - Python ve veri analizi iceren proje var mi?\n" +
            "  - Web scraping yapan bir proje ornegi goster\n\n" +
            "Not: Once 'Projeler' sekmesinden klasor taraman gerekiyor."
        );
    }

    private void sendMessage() {
        String question = txtInput.getText().trim();
        if (question.isEmpty()) return;

        String service = serviceSupplier.get();
        String apiKey = apiKeySupplier.get();
        String geminiModel = geminiModelSupplier.get();
        String gptApiUrl = gptApiUrlSupplier.get();
        String gptModel = gptModelSupplier.get();

        addUserBubble(question);
        txtInput.setText("");
        btnSend.setEnabled(false);
        lblStatus.setText("Asistan dusunuyor...");

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return assistant.ask(question, service, apiKey, geminiModel, gptApiUrl, gptModel);
            }
            @Override protected void done() {
                try {
                    addAIBubble(get());
                    lblStatus.setText(" ");
                } catch (Exception ex) {
                    addAIBubble("Hata: " + ex.getMessage());
                    lblStatus.setText("Bir hata olustu.");
                } finally {
                    btnSend.setEnabled(true);
                }
            }
        }.execute();
    }

    // --- Kullanici balonu: sag hizali ---
    private void addUserBubble(String text) {
        String formatted = escapeAndFormat(text);
        chatHtml.append("<table width='100%' border='0' cellspacing='0' cellpadding='5'><tr>")
                .append("<td width='20%'></td>")
                .append("<td align='right'>")
                .append("<table border='0' cellspacing='0' cellpadding='10' bgcolor='#375fc8'>")
                .append("<tr><td align='left' style='color: #e6ebff; font-family: Segoe UI, sans-serif; font-size: 13px;'>")
                .append(formatted)
                .append("</td></tr></table>")
                .append("</td></tr></table><br>");
        updateChatPane();
    }

    // --- AI balonu: sol hizali ---
    private void addAIBubble(String text) {
        String formatted = escapeAndFormat(text);
        chatHtml.append("<table width='100%' border='0' cellspacing='0' cellpadding='5'><tr>")
                .append("<td align='left'>")
                .append("<table border='0' cellspacing='0' cellpadding='10' bgcolor='#32323c'>")
                .append("<tr><td align='left' style='color: #d7d7e1; font-family: Segoe UI, sans-serif; font-size: 13px;'>")
                .append(formatted)
                .append("</td></tr></table>")
                .append("</td>")
                .append("<td width='20%'></td>")
                .append("</tr></table><br>");
        updateChatPane();
    }

    private String escapeAndFormat(String text) {
        if (text == null) return "";
        // 1. HTML Karakterlerini Kacis
        String escaped = text.replace("&", "&amp;")
                             .replace("<", "&lt;")
                             .replace(">", "&gt;");
        // 2. Yeni Satirlar -> <br>
        escaped = escaped.replace("\n", "<br>");
        // 3. Markdown Kalin Yazi **metin** -> <b>metin</b>
        escaped = escaped.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        // 4. Markdown Kod `metin` -> HTML code block
        escaped = escaped.replaceAll("`(.*?)`", "<code style='background-color: #4a4a5a; color: #ffffff;'>&nbsp;$1&nbsp;</code>");
        return escaped;
    }

    private void updateChatPane() {
        chatPane.setText(chatHtml.toString() + "</body></html>");
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = chatScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    public void refreshActiveModel() {
        if (lblActiveModel == null) return;
        String service = serviceSupplier.get();
        String modelName = "";

        if ("gemini".equalsIgnoreCase(service)) {
            String m = geminiModelSupplier.get();
            modelName = "Gemini: " + (m == null || m.isEmpty() ? "gemini-2.5-flash" : m);
        } else if ("gpt".equalsIgnoreCase(service)) {
            modelName = "OpenAI: gpt-4o-mini";
        } else if ("custom".equalsIgnoreCase(service)) {
            String m = gptModelSupplier.get();
            modelName = "Custom API: " + (m == null || m.isEmpty() ? "gpt-4o-mini" : m);
        } else {
            modelName = "Groq: llama-3.3-70b-versatile";
        }

        lblActiveModel.setText("Aktif Model: " + modelName);
    }
}
