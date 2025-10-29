# 🎓 AI Tutor - Subject-Specific Learning Assistant

A lightweight web-based AI tutoring application that provides educational assistance for Computer Science subjects. Built with Java backend and vanilla HTML/CSS/JavaScript frontend, powered by Google's Gemini API.

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=java&logoColor=white)
![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=flat&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=flat&logo=css3&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=flat&logo=javascript&logoColor=black)

## ✨ Features

- **Subject-Specific Tutoring**: Specialized assistance in 6 core CS subjects
  - Java Programming
  - C++ Programming
  - Data Structures
  - Operating Systems
  - Database Management Systems (DBMS)
  - Computer Networks
  
- **Smart Content Filtering**: Only answers questions within allowed subjects
- **Real-time AI Responses**: Powered by Google Gemini 1.5 Flash
- **Clean Modern UI**: Responsive chat interface with smooth animations
- **Zero Dependencies**: No external Java libraries required
- **CORS Enabled**: Ready for cross-origin requests
- **Auto Port Selection**: Automatically finds available ports (8080-9091)

## 🚀 Quick Start

### Prerequisites

- Java Development Kit (JDK) 11 or higher
- A modern web browser (Chrome, Firefox, Safari, Edge)
- Google Gemini API key (free tier available)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/ai-tutor.git
   cd ai-tutor
   ```

2. **Get your Gemini API key**
   - Visit [Google AI Studio](https://aistudio.google.com/app/apikey)
   - Create a new API key (free tier: 60 requests/minute)

3. **Create a `.env` file**
   ```bash
   echo "GEMINI_API_KEY=your-api-key-here" > .env
   ```
   
   Or manually create `.env` in the project root:
   ```
   GEMINI_API_KEY=your-api-key-here
   ```

4. **Compile the Java backend**
   ```bash
   javac AITutorServer.java
   ```

5. **Run the server**
   ```bash
   java AITutorServer
   ```
   
   You should see:
   ```
   ✅ API key loaded from .env file
   🚀 AI Tutor backend running at http://localhost:8080/
   📝 Update your index.html to use port 8080
   🤖 Using Google Gemini API (Free Tier: 60 requests/min)
   ```

6. **Open the frontend**
   - Open `index.html` in your web browser
   - Or serve it with any local server:
     ```bash
     # Python 3
     python -m http.server 3000
     
     # Node.js
     npx http-server
     ```

## 📁 Project Structure

```
ai-tutor/
│
├── AITutorServer.java    # Backend server with Gemini API integration
├── index.html            # Frontend chat interface
├── .env                  # API key configuration (create this)
├── README.md            # This file
└── .gitignore           # Git ignore file
```

## 🛠️ Configuration

### Port Configuration

The server automatically tries these ports in order:
```
8080, 8081, 8082, 9090, 9091, 3000, 5000
```

If you need to use a specific port, modify the `portsToTry` array in `AITutorServer.java`:
```java
int[] portsToTry = { YOUR_PORT, 8080, 8081 };
```

### Subject Keywords

To add or modify subject detection, edit the `ALLOWED_SUBJECTS` map in `AITutorServer.java`:
```java
ALLOWED_SUBJECTS.put("Your Subject", new String[] { "keyword1", "keyword2" });
```

### API Configuration

Token limits and generation parameters in `buildGeminiPayload()`:
```java
"maxOutputTokens": 2048,    // Max response length
"temperature": 0.7,         // Creativity (0.0-1.0)
"topP": 0.95,              // Nucleus sampling
"topK": 40                 // Top-K sampling
```

## 💡 Usage Examples

### Valid Questions (Within Allowed Subjects)
- "Explain Java inheritance with examples"
- "What is a binary search tree?"
- "How does TCP differ from UDP?"
- "Explain deadlock in operating systems"
- "What is database normalization?"
- "How do pointers work in C++?"

### Invalid Questions (Outside Scope)
- "What's the weather today?"
- "Write me a Python script"
- "Explain quantum physics"

The AI Tutor will politely inform you that it can only help with the specified CS subjects.

## 🔧 Troubleshooting

### API Key Issues
```
❌ ERROR: GEMINI_API_KEY not found!
```
**Solution**: Ensure `.env` file exists with correct format:
```
GEMINI_API_KEY=your-actual-key-here
```

### Port Already in Use
```
⚠️ Port 8080 is busy, trying next...
```
**Solution**: The server will automatically try the next port. Note the port it uses and update `index.html` if needed.

### CORS Errors
```
Access to fetch at 'http://localhost:8080/ask' has been blocked by CORS policy
```
**Solution**: 
1. Ensure the backend is running
2. Check that the port in `index.html` matches the backend port
3. Open `index.html` via a local server, not as a file

### Rate Limit Exceeded
```
⚠️ Rate limit exceeded (60 requests/min)
```
**Solution**: Wait a minute before sending more requests. Free tier limits: 60 requests/minute.

### Empty or Truncated Responses
```
⚠️ Response was too long and got cut off
```
**Solution**: Ask more specific questions. The response limit is 2048 tokens (~1500 words).

## 🔒 Security Notes

- **API Key**: Never commit your `.env` file to version control
- **CORS**: Currently allows all origins (`*`). For production, restrict to specific domains
- **Input Validation**: The server validates subject relevance before API calls
- **Rate Limiting**: Relies on Gemini's built-in rate limits (60/min free tier)

## 📊 API Response Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Response returned |
| 400 | Bad Request | Check request format |
| 403 | Forbidden | Invalid API key |
| 404 | Not Found | Model name incorrect |
| 429 | Rate Limited | Wait and retry |

## 🎨 Customization

### Frontend Styling

Edit the `<style>` section in `index.html` to customize:
- Colors (modify `#2563eb` for primary blue)
- Fonts (currently using 'Poppins')
- Layout dimensions
- Animation speeds

### Adding New Subjects

1. Add to `ALLOWED_SUBJECTS` in `AITutorServer.java`:
   ```java
   ALLOWED_SUBJECTS.put("Machine Learning", 
       new String[] { "ml", "machine learning", "neural network" });
   ```

2. Add button to sidebar in `index.html`:
   ```html
   <button>Machine Learning</button>
   ```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes:

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is open source and available under the [MIT License](LICENSE).

## 🙏 Acknowledgments

- [Google Gemini API](https://ai.google.dev/) for powering the AI responses
- [Poppins Font](https://fonts.google.com/specimen/Poppins) by Indian Type Foundry

## 📮 Contact

For questions or suggestions:
- Open an issue on GitHub
- Email: your.email@example.com
- Twitter: [@yourhandle](https://twitter.com/yourhandle)

## 🗺️ Roadmap

- [ ] Add conversation history persistence
- [ ] Implement user authentication
- [ ] Add code syntax highlighting
- [ ] Support for code execution/testing
- [ ] Mobile app version
- [ ] Multi-language support
- [ ] Export chat transcripts
- [ ] Voice input/output

---

**Made with ❤️ for students learning Computer Science**

⭐ Star this repo if you find it helpful!
