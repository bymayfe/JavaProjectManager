package com.smartproject.gui;

import com.smartproject.ai.ProjectAssistant;
import com.smartproject.db.ProjectRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Supplier;

public class AssistantPanel extends JPanel {

    private JPanel   chatPanel;
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

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
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

        // Mesaj paneli - BoxLayout ile dikey dizilim
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        chatScroll = new JScrollPane(chatPanel);
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

    // --- Kullanici balonu: sag hizali, mavi ---
    private void addUserBubble(String text) {
        addBubble(text, new Color(55, 95, 200), new Color(230, 235, 255), true);
    }

    // --- AI balonu: sol hizali, koyu gri ---
    private void addAIBubble(String text) {
        addBubble(text, new Color(50, 50, 60), new Color(215, 215, 225), false);
    }

    /**
     * Baloncuk olusturur.
     * Genislik: chatPanel genisliginin %72'si (sabit degil, scroll panele gore ayarlanir).
     * isUser=true => sag hizali, isUser=false => sol hizali.
     */
    private void addBubble(String text, Color bg, Color fg, boolean isUser) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setBackground(bg);
        area.setForeground(fg);
        area.setBorder(new EmptyBorder(8, 12, 8, 12));
        area.setOpaque(true);
        
        // Secip kopyalamayi gorunur kil
        area.setSelectionColor(new Color(100, 150, 255, 120));
        area.setSelectedTextColor(Color.WHITE);
        area.getCaret().setSelectionVisible(true);

        // Genisligi sinirla ve dogru yuksekligi hesaplamasi icin uyar
        int bubbleWidth = 480;
        area.setSize(new Dimension(bubbleWidth, Short.MAX_VALUE));
        int calculatedHeight = area.getPreferredSize().height;
        area.setPreferredSize(new Dimension(bubbleWidth, calculatedHeight));

        // Hizalama icin BorderLayout kullanan bir satir paneli
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        if (isUser) {
            // Sag: mesaj merkeze, sol taraf bos
            JPanel filler = new JPanel();
            filler.setOpaque(false);
            row.add(filler, BorderLayout.CENTER);
            row.add(area,   BorderLayout.EAST);
        } else {
            // Sol: mesaj basa, sag taraf bos
            JPanel filler = new JPanel();
            filler.setOpaque(false);
            row.add(area,   BorderLayout.WEST);
            row.add(filler, BorderLayout.CENTER);
        }

        chatPanel.add(row);
        chatPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        chatPanel.revalidate();
        chatPanel.repaint();
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
            modelName = "Groq: llama-3.1-8b-instant";
        }

        lblActiveModel.setText("Aktif Model: " + modelName);
    }
}
