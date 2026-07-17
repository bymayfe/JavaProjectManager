# 🚀 Smart Project Manager

🌐 Read this in other languages: [Türkçe (Turkish)](README_TR.md)

[![Java Version](https://img.shields.io/badge/Java-11%2B-orange?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Maven Build](https://img.shields.io/badge/Maven-3.x-blue?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.1.1-informational)](CHANGELOG.md)
[![Platform Compatibility](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey)](https://github.com/bymayfe)

Smart Project Manager is a comprehensive, modern **Java-based desktop application** designed to support the software development lifecycle. This project was developed as a term project for the developer's **Java Programming Course**. The concept, design, and codebase belong entirely to **Seyfettin Budak**.

It provides developers and project managers with a unified workspace to organize projects, run deep AI code analyses, manage Git/GitHub workflows, and inspect local Docker environments.

---

## ✨ Features

*   📂 **Project Workspace Management:** Create, edit, list, and persist local project workspaces using a clean data model.
*   🧠 **AI-Powered Code Analysis (`AIAnalyzer`):** Integrated AI engine supporting **Gemini**, **Groq**, and **GPT/OpenAI-compatible** services. Analyzes codebases for performance bottlenecks, security vulnerabilities, and syntax improvements. Also auto-generates professional README files and project tags. See the [AI Services](#-ai-services--supported-models) section for supported models.
*   🐳 **Docker Environment Scanner:** Scans local Docker containers, images, and networks to display their status directly inside a graphical dashboard.
*   🐙 **Git & GitHub Workflows:** Stage, commit, and push repository changes dynamically without using external Git CLI commands, using pure JGit and GitHub API.
*   🔒 **Remote Connection & SSH Management:** Establish secure SSH (JSch) connections to remote host machines, browse files, run commands in an embedded terminal, and run remote project analysis. Includes **rate-limit protection** (connection debounce + exponential backoff retry) to prevent SSH servers from blocking repeated connection attempts.
*   💾 **Flexible Database Choices:** Dynamically choose and manage application persistence backends (MongoDB, MySQL, or embedded SQLite).
*   👤 **Connection Profile System:** Save and restore multiple SSH/AI/database configuration profiles via `ConfigManager` — switch between environments in one click.
*   🎨 **Modern User Interface:** Built on Java Swing & AWT, styled with the premium FlatLaf Look and Feel for high-definition (HiDPI) screens and smooth animations.

---

## 🤖 AI Services & Supported Models

All AI features (code analysis, README generation, project tagging, assistant chat) are powered by the `AIAnalyzer` engine, which supports three provider families:

| Provider | Service Key | Default Model | Notes |
|----------|-------------|---------------|-------|
| **Google Gemini** | `gemini` | `gemini-2.5-flash` | Configurable via settings |
| **Groq** | `groq` | `llama-3.3-70b-versatile` | Best free-tier model; fast inference |
| **OpenAI / GPT** | `gpt` | `gpt-4o-mini` | Full OpenAI API |
| **Custom (OpenAI-Compatible)** | `custom` | Configurable | Works with any OpenAI-compatible endpoint (e.g. local Ollama, LM Studio) |

> [!TIP]
> For free usage, **Groq** with `llama-3.3-70b-versatile` provides the best balance of speed and quality. Sign up at [console.groq.com](https://console.groq.com) to get your free API key.

---

## 🛠️ Technology Stack

*   **Language:** Java 11+
*   **GUI Toolkit:** Java Swing & AWT (FlatLaf Modern Theme)
*   **Databases:** SQLite, MySQL, and MongoDB
*   **Version Control:** Eclipse JGit
*   **SSH & Network:** JSch (with keepAlive, debounce & exponential backoff retry)
*   **AI Integration:** Google GenAI SDK (Gemini) & OpenAI-compatible REST API (Groq, GPT, Custom)
*   **Build System:** Maven 3.x

---

## ⚙️ Project Structure

```text
javaProje/
├── .mvn/                  # Maven Wrapper configuration files
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── smartproject/
│   │               ├── ai/         # AI analysis engine and assistants
│   │               ├── config/     # Application configurations manager
│   │               ├── db/         # SQL & NoSQL database providers
│   │               ├── file/       # Local file & workspace helpers
│   │               ├── git/        # JGit client and GitHub API manager
│   │               ├── gui/        # Swing UI frames and dialogs
│   │               ├── model/      # Base entities (Project, etc.)
│   │               └── scanner/    # Docker and local directory scanners
├── pom.xml                # Maven dependencies and build configuration
├── run.bat                # One-click Windows starter script
├── run.sh                 # One-click macOS/Linux starter script
└── README.md              # Project documentation (English)
```

---

## ⚙️ How to Compile & Run

You can run or build the project using either Graphical IDE interfaces or Command Line instructions.

---

### 🎮 Method A: Using IDE Graphical Interfaces (GUI)
If you prefer not to write terminal commands, you can build and run using your IDE's visual buttons.

<details>
<summary><b>👁️ VS Code GUI Instructions (Click to expand)</b></summary>

1. **Build Project (Generate JAR):**
   * Expand the **MAVEN** explorer panel on the left sidebar.
   * Go to `SmartProjectManager` > `Lifecycle` folder.
   * Hover over **`clean`** and click the **Play** button, then hover over **`package`** and click the **Play** button.
2. **Run Project:**
   * Open the file `src/main/java/com/smartproject/Main.java`.
   * Click the **Run Java** action/play button in the top-right corner of the editor.
</details>

<details>
<summary><b>👁️ Apache NetBeans GUI Instructions (Click to expand)</b></summary>

1. **Build Project (Generate JAR):**
   * Right-click the project node (`SmartProjectManager`) in the **Projects** panel.
   * Select **"Clean and Build"** from the context menu. Maven will automatically compile the JAR.
2. **Run Project:**
   * Right-click the project node and select **"Run"**, or press **F6** on your keyboard.
</details>

---

### 💻 Method B: Using Command Line Interface (CLI)
You can compile and run directly from your command shell/terminal.

#### 1. Quick Bootstrap Scripts (No Maven setup required):
Use the platform-specific scripts in the project root:
* **For Windows:** Double-click `run.bat` or run:
  ```cmd
  .\run.bat
  ```
* **For macOS / Linux:** Open your terminal and run:
  ```bash
  chmod +x run.sh
  ./run.sh
  ```
*These scripts search for a pre-compiled version inside `target/` first. If missing, they prompt you to automatically download the Maven Wrapper to compile and launch the project.*

#### 2. Manual Wrapper Build & Execute:
* **Windows (CMD/PowerShell):**
  ```cmd
  mvnw.cmd clean package
  java -jar target/SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar
  ```
* **macOS / Linux (Terminal):**
  ```bash
  ./mvnw clean package
  java -jar target/SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar
  ```

---

### 📦 Method C: Manual Release Packaging
Follow these steps to package the project into release artifacts (.exe / .jar) for distribution:

> [!NOTE]
> Distributing the cross-platform **JAR** file (`SmartProjectManager-CrossPlatform.jar`) is completely sufficient for Windows, macOS, and Linux, provided that the target computer has at least Java 11 installed. Creating native bundles (EXE, APP, DEB) is optional.

1. **Compile the JAR:**
   * **Via GUI:** Use `package` in VS Code Maven panel or `Clean and Build` in NetBeans.
   * **Via CLI:** Run `mvnw.cmd clean package` (Windows) or `./mvnw clean package` (Mac/Linux).
2. **Prepare the Cross-Platform Release JAR:**
   * Copy the fat JAR generated in `target/` to the `dist/` directory and rename it to **`SmartProjectManager-CrossPlatform.jar`**.
3. **Compile Native Installer Bundles (jpackage):**
   *Note: Cross-compilation is not supported by jpackage. You must run these commands on the corresponding host system:*
   * **Windows (.exe - Portable Folder):**
     Create a native Windows executable and bundled JRE:
     ```cmd
     "C:\Program Files\Java\jdk-26.0.1\bin\jpackage.exe" --input target\ --dest dist\ --name SmartProjectManager --main-jar SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar --main-class com.smartproject.Main --type app-image
     ```
   * **macOS (.app Bundle):**
     Generate a native Mac executable structure:
     ```bash
     jpackage --input target/ --dest dist/ --name SmartProjectManager --main-jar SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar --main-class com.smartproject.Main --type app-image
     ```
     *(Use `--type dmg` if you prefer to generate a native drag-and-drop installer image).*
   * **Linux (.deb Installer Package):**
     Generate a Debian installation package:
     ```bash
     jpackage --input target/ --dest dist/ --name smartprojectmanager --main-jar SmartProjectManager-1.0-SNAPSHOT-jar-with-dependencies.jar --main-class com.smartproject.Main --type deb
     ```
4. **Compress Portable Builds (ZIP / TAR.GZ):**
   * **On Windows (Explorer):** Right-click the folder `dist/SmartProjectManager` -> **Send to Compressed (zipped) folder** (or **Compress to ZIP file**) and rename it to `SmartProjectManager-Windows-Portable.zip`.
   * **On macOS / Linux (Terminal):** Archive using tar:
     ```bash
     tar -czvf dist/SmartProjectManager-Portable.tar.gz -C dist SmartProjectManager
     ```

---

## 👥 Authors & Contributors

*   **Seyfettin Budak** - *Project Concept, Design, and Lead Developer* - [bymayfe](https://github.com/bymayfe)

> [!IMPORTANT]
> This project was developed as a term project for the author's **Java Programming Course**. All rights, conceptual designs, and source implementations belong entirely to **Seyfettin Budak**.

---

## 📄 License

This project is licensed under the **MIT License**. See the `LICENSE` file for more details.

---

## 📋 Changelog

See [CHANGELOG.md](CHANGELOG.md) for the full version history and release notes.
