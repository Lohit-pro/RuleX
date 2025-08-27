# 📊 RuleX Engine

**RuleX Engine** is a lightweight, flexible **Excel Data Validation Engine** that helps businesses and individuals ensure their data quality.  
Upload your Excel file, define validation rules, and instantly get a clean report highlighting all issues — no more manual checks! ✅  

---

## ✨ Features

- 📂 Upload Excel files directly from UI  
- 🛠️ Define **custom rules per column**:
  - Numeric  
  - Alphabetic  
  - Email  
  - Date  
  - Non-Empty  
  - Range (min/max)  
- ⚡ Fast validation powered by **Apache POI**  
- 📑 Download error report as **Excel** with invalid cell references  
- 🖍️ Highlight errors inside Excel for quick review  
- 📧 Send validation report via email directly from the app  
- 🗑️ Automatic cleanup of uploaded files after processing  

---

## 🚀 Tech Stack

- **Frontend:** React + TailwindCSS  
- **Backend:** Spring Boot (Java)  
- **Excel Handling:** Apache POI  
- **Database:** DB Free
- **Email Service:** Jakarta Mail (Gmail SMTP)  

---

## 🖥️ Getting Started

### 1️⃣ Clone Repository
```bash
git clone https://github.com/your-username/rulex-engine.git
cd rulex-engine
```

### 2️⃣ Backend Setup

Update application.properties:

```bash
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

error.files.path=<path_inside_resources>
excel.files.path=<path_inside_resources>
```

Run Backend

```bash
mvn spring-boot:run
```

### 3️⃣ Frontend Setup

```bash
cd rulex-ui
npm install
npm run dev
```

App will be running at http://localhost:5173/ 🚀

### 🤝 Contributing

We welcome contributions!

```bash
1. Fork the repo

2. Create your branch (git checkout -b feature/my-feature)

3. Commit changes (git commit -m 'Add my feature')

4. Push branch (git push origin feature/my-feature)

5. Open a Pull Request
```

### 🔥 With RuleX Engine, say goodbye to messy spreadsheets and hello to clean, reliable data.
