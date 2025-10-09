# Java Deadlock Detection & Visualization Tool

## 🎯 Project Overview
A real-time deadlock detection, visualization, and recovery tool for Java applications using JMX APIs and interactive web dashboard.

## 🚀 Features
- **Automatic Deadlock Detection** using ThreadMXBean
- **Interactive Graph Visualization** with Cytoscape.js
- **Real-time Monitoring** via WebSocket
- **Safe Recovery Options** with thread interruption
- **Web Dashboard** for monitoring multiple applications

## 🏗️ Project Structure
```
deadlock/
├── backend/                    # Spring Boot application
│   ├── src/main/java/
│   │   └── com/deadlock/
│   │       ├── DeadlockApplication.java
│   │       ├── service/
│   │       │   └── DeadlockService.java
│   │       ├── controller/
│   │       │   └── DeadlockController.java
│   │       └── model/
│   │           └── DeadlockSnapshot.java
│   └── pom.xml
├── frontend/                   # Web dashboard
│   ├── index.html
│   ├── css/
│   │   └── style.css
│   └── js/
│       └── app.js
├── samples/                    # Test applications
│   ├── SimpleDeadlock.java
│   ├── ReentrantLockDeadlock.java
│   └── MultiThreadDeadlock.java
└── docs/                      # Documentation
    └── setup.md
```

## 🛠️ Tech Stack
- **Backend**: Java 17+, Spring Boot, Maven
- **Frontend**: HTML5, CSS3, JavaScript, Cytoscape.js
- **Communication**: REST API, WebSocket (STOMP)
- **Monitoring**: JMX ThreadMXBean API
- **Development**: VS Code with Java extensions

## ⏱️ Development Timeline
- **Week 1**: Project setup + Core detection service
- **Week 2**: Sample applications + Frontend visualization  
- **Week 3**: Real-time updates + Recovery features
- **Week 4**: Testing + Documentation + Polish

## 🚦 Getting Started
1. Ensure Java 17+ and Maven are installed
2. Install VS Code with Java Extension Pack
3. Clone this repository
4. Follow setup instructions in `docs/setup.md`

## 📊 Demo
Will include screenshots and video demos showing:
- Live deadlock detection
- Interactive graph visualization
- Real-time thread monitoring
- Safe recovery actions