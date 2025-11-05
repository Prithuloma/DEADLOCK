# 🎯 Complete Deadlock Detection Demo - Quick Start

## 🚀 Automated Demo (Recommended)

**Just double-click: `RUN-DEMO.bat`**

This will automatically:
1. ✅ Start the backend server in a new window
2. ✅ Open the dashboard in your browser  
3. ✅ Compile and run the deadlock test with JMX enabled
4. ✅ Show you exactly what to do next

## 📋 Manual Step-by-Step Process

If you want to run each step manually:

### Step 1: Start Backend Server
```powershell
cd backend
java -jar target\deadlock-detector-1.0.0.jar
```
Wait until you see "Started DeadlockDetectorApplication"

### Step 2: Open Dashboard
Open browser to: **http://localhost:8080**

### Step 3: Run Deadlock Test with JMX
```powershell
cd samples
javac QuickDeadlockTest.java
java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=true QuickDeadlockTest
```

### Step 4: Configure Dashboard
1. Click **"Select Process"** button
2. Choose the Java process on **port 9010**
3. Turn **ON** the "Auto-Resolution" toggle

## 🎬 What You'll See (Complete Flow)

### Phase 1: Deadlock Formation (0-2 seconds)
```
🧪 Starting Quick Deadlock Test...
Thread-1: Acquired lock1
Thread-2: Acquired lock2  
Thread-1: Now trying to acquire lock2 (will create circular wait)...
Thread-2: Now trying to acquire lock1 (will create circular wait)...
🔍 Deadlock Monitor: Detected 2 deadlocked threads
```

**In Dashboard:**
- 📊 System Metrics shows: **2 Deadlocked Threads**
- 🕸️ Graph shows circular dependency between threads
- ⏱️ Activity Timeline shows: "Deadlock Detected"

### Phase 2: Observation Window (2-47 seconds)
```
⏱️  Waiting 45 seconds to allow dashboard observation...
```

**In Dashboard:**
- ✅ Deadlock remains visible for 45 seconds
- 📈 You can explore the graph, see thread states
- 💾 You can export the report to see snapshot

### Phase 3: Auto-Resolution (47-50 seconds)
```
🔧 AUTO-RESOLUTION: Interrupting deadlocked thread...
⚡ Interrupted Worker-Thread-1
⚡ Interrupted Worker-Thread-2
✅ Deadlock resolved! System returned to normal state.
```

**In Dashboard:**
- ⚡ Activity Timeline shows: "Resolution Attempt"
- ✅ System Metrics returns to: **0 Deadlocked Threads**
- 📊 Analytics tab shows resolution details

### Phase 4: Normal Completion (50+ seconds)
```
⚡ Thread-1: INTERRUPTED during deadlock - Releasing locks and exiting
⚡ Thread-2: INTERRUPTED during deadlock - Releasing locks and exiting

🎉 Test completed successfully!
📈 Check the export report for full resolution details
```

**In Dashboard:**
- ✅ All metrics back to normal
- 📈 Analytics shows complete lifecycle
- 💾 Export report contains full resolution history

## 📊 Expected Timeline

| Time | Event | Dashboard Shows |
|------|-------|-----------------|
| 0s | Program starts | - |
| 0.1s | Deadlock forms | 🔴 2 Deadlocked Threads |
| 0.5s | Monitor detects | ⚠️ "Deadlock Detected" in timeline |
| 0.5-45s | Observation | 📊 Deadlock visible, graph shows cycle |
| 45s | Auto-resolution starts | ⚡ "Resolution Attempt" |
| 47s | Threads interrupted | ✅ Threads released |
| 50s | Program completes | ✅ 0 Deadlocked Threads |

## 🎯 Key Features Demonstrated

✅ **Real-time Detection**: Deadlock detected within 500ms  
✅ **45-Second Observation**: Time to explore the visualization  
✅ **Automatic Resolution**: Backend detects & resolves  
✅ **Thread State Tracking**: Shows BLOCKED threads  
✅ **Comprehensive Analytics**: Full resolution history  
✅ **Export Reports**: Detailed snapshots with timestamps  

## ⚠️ Important Notes

### Why BLOCKED Threads?
The test creates **BLOCKED threads** (classic synchronized deadlock). The backend will detect:
- ❌ External process with BLOCKED threads
- 💡 Recommendation to use interruptible locks
- 🔧 The test's own monitor thread resolves it after 45 seconds

### Backend vs Test Resolution
- **Backend**: Detects deadlock via JMX, monitors external process
- **Test's Monitor**: Actually resolves by interrupting threads
- **Dashboard**: Shows both detection and resolution events

This demonstrates that:
1. ✅ Backend can **detect** deadlocks in external processes
2. ✅ Backend can **monitor** thread states (BLOCKED vs WAITING)
3. ✅ External process needs to **self-resolve** BLOCKED threads
4. ✅ Dashboard tracks the complete lifecycle

## 🔄 Running Again

The backend server stays running. To run another demo:
- Just run `RUN-DEMO.bat` again (it will start a new test)
- OR manually run the test: `java -Dcom.sun.management.jmxremote... QuickDeadlockTest`

## 🐛 Troubleshooting

**Dashboard doesn't show threads?**
- Make sure you selected the process (port 9010) in the dashboard
- Refresh the dashboard page

**Backend won't start?**
- Check if port 8080 is already in use
- Look for error messages in the backend window

**Test completes too quickly?**
- The 45-second observation window is hardcoded
- Deadlock should last exactly 45 seconds before resolution

**Can't see resolution in Analytics?**
- Wait for the full 50 seconds for the cycle to complete  
- Check the Activity Timeline for resolution events
- Export a report to see the detailed history
