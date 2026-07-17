package com.smartproject.config;

import java.io.*;
import java.util.Properties;

/**
 * Ayarlari (API key, GitHub bilgileri, SSH bilgileri) diske kaydeder ve yukler.
 * Dosya: ~/.smartproject/config.properties
 */
public class ConfigManager {

    private static final String CONFIG_FOLDER = System.getProperty("user.home") + File.separator + ".smartproject";
    private static final String CONFIG_FILE   = CONFIG_FOLDER + File.separator + "config.properties";

    private Properties props;

    public ConfigManager() {
        props = new Properties();
        ensureFolder();
        load();
    }

    private void ensureFolder() {
        File folder = new File(CONFIG_FOLDER);
        if (!folder.exists()) folder.mkdirs();
        File profFolder = new File(CONFIG_FOLDER + File.separator + "profiles");
        if (!profFolder.exists()) profFolder.mkdirs();
    }

    public void saveProfile(String profileName) {
        ensureFolder();
        File file = new File(CONFIG_FOLDER + File.separator + "profiles" + File.separator + profileName + ".properties");
        try (OutputStream out = new FileOutputStream(file)) {
            props.store(out, "Smart Project Manager - Profile: " + profileName);
        } catch (Exception e) {
            System.err.println("Profil kaydedilemedi: " + e.getMessage());
        }
    }

    public boolean loadProfile(String profileName) {
        File file = new File(CONFIG_FOLDER + File.separator + "profiles" + File.separator + profileName + ".properties");
        if (!file.exists()) return false;
        try (InputStream in = new FileInputStream(file)) {
            props.clear();
            props.load(in);
            save(); // Save to default properties as well
            return true;
        } catch (Exception e) {
            System.err.println("Profil yuklenemedi: " + e.getMessage());
            return false;
        }
    }

    public java.util.List<String> getProfileNames() {
        java.util.List<String> list = new java.util.ArrayList<>();
        File folder = new File(CONFIG_FOLDER + File.separator + "profiles");
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".properties"));
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    list.add(name.substring(0, name.lastIndexOf('.')));
                }
            }
        }
        return list;
    }

    public void deleteProfile(String profileName) {
        File file = new File(CONFIG_FOLDER + File.separator + "profiles" + File.separator + profileName + ".properties");
        if (file.exists()) {
            file.delete();
        }
    }

    private void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) return;
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (Exception e) {
            System.err.println("Config yuklenemedi: " + e.getMessage());
        }
    }

    public void save() {
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Smart Project Manager - Ayarlar");
        } catch (Exception e) {
            System.err.println("Config kaydedilemedi: " + e.getMessage());
        }
    }

    // --- AI Servisi Secimi ---
    public String getAiService()         { return props.getProperty("ai.service", "groq"); }
    public void   setAiService(String v) { props.setProperty("ai.service", v); }

    // --- Groq ---
    public String getApiKey()            { return props.getProperty("groq.apikey", ""); }
    public void   setApiKey(String v)    { props.setProperty("groq.apikey", v); }

    // --- Gemini ---
    public String getGeminiApiKey()         { return props.getProperty("gemini.apikey", ""); }
    public void   setGeminiApiKey(String v) { props.setProperty("gemini.apikey", v); }

    public String getGeminiModel()          { return props.getProperty("gemini.model", "gemini-2.5-flash"); }
    public void   setGeminiModel(String v)  { props.setProperty("gemini.model", v); }

    // --- GPT / OpenAI ---
    public String getGptApiKey()         { return props.getProperty("gpt.apikey", ""); }
    public void   setGptApiKey(String v) { props.setProperty("gpt.apikey", v); }

    public String getGptApiUrl()         { return props.getProperty("gpt.apiurl", "https://api.openai.com/v1/chat/completions"); }
    public void   setGptApiUrl(String v) { props.setProperty("gpt.apiurl", v); }

    public String getGptModel()          { return props.getProperty("gpt.model", "gpt-4o-mini"); }
    public void   setGptModel(String v)  { props.setProperty("gpt.model", v); }

    // --- GitHub ---
    public String getGithubUser()         { return props.getProperty("github.user", ""); }
    public void   setGithubUser(String v) { props.setProperty("github.user", v); }

    public String getGithubToken()         { return props.getProperty("github.token", ""); }
    public void   setGithubToken(String v) { props.setProperty("github.token", v); }

    // --- Son taranan klasor ---
    public String getLastFolder()          { return props.getProperty("last.folder", ""); }
    public void   setLastFolder(String v)  { props.setProperty("last.folder", v); }

    // --- SSH Ayarlari ---
    public String getSshHost()             { return props.getProperty("ssh.host", ""); }
    public void   setSshHost(String v)     { props.setProperty("ssh.host", v); }

    public String getSshPort()             { return props.getProperty("ssh.port", "22"); }
    public void   setSshPort(String v)     { props.setProperty("ssh.port", v); }

    public String getSshUser()             { return props.getProperty("ssh.user", ""); }
    public void   setSshUser(String v)     { props.setProperty("ssh.user", v); }

    public String getSshPemPath()          { return props.getProperty("ssh.pempath", ""); }
    public void   setSshPemPath(String v)  { props.setProperty("ssh.pempath", v); }

    public String getSshLastPath()         { return props.getProperty("ssh.lastpath", ""); }
    public void   setSshLastPath(String v) { props.setProperty("ssh.lastpath", v); }

    public boolean getSshUseSudo()         { return Boolean.parseBoolean(props.getProperty("ssh.usesudo", "false")); }
    public void    setSshUseSudo(boolean v){ props.setProperty("ssh.usesudo", String.valueOf(v)); }

    // --- Database Ayarlari ---
    public String getDbType()              { return props.getProperty("db.type", "json"); }
    public void   setDbType(String v)      { props.setProperty("db.type", v); }

    public String getDbUrl()               { return props.getProperty("db.url", ""); }
    public void   setDbUrl(String v)       { props.setProperty("db.url", v); }

    public String getDbName()              { return props.getProperty("db.name", "smartproject"); }
    public void   setDbName(String v)      { props.setProperty("db.name", v); }

    public String getDbUser()              { return props.getProperty("db.user", ""); }
    public void   setDbUser(String v)      { props.setProperty("db.user", v); }

    public String getDbPass()              { return props.getProperty("db.pass", ""); }
    public void   setDbPass(String v)      { props.setProperty("db.pass", v); }

    public String getDbTable()             { return props.getProperty("db.table", "projects"); }
    public void   setDbTable(String v)     { props.setProperty("db.table", v); }

    public String getDbDriver()            { return props.getProperty("db.driver", ""); }
    public void   setDbDriver(String v)    { props.setProperty("db.driver", v); }
}
