/**
 * Professional Deadlock Detection Dashboard
 * FIXED VERSION - Real data from external processes
 */

class DeadlockDashboard {
    constructor() {
        this.cy = null;
        this.stompClient = null;
        this.isConnected = false;
        this.currentSnapshot = null;
        this.snapshots = [];
        this.selectedProcessPid = null;
        this.selectedProcessName = null;
        
        // WebSocket retry configuration
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 10;
        this.reconnectDelay = 5000;
        
        // 🚀 AUTO-RESOLUTION FEATURES
        this.autoResolutionEnabled = true;
        this.resolutionHistory = [];
        this.resolutionStats = null;
        this.isShowingResolutionPanel = false;
        
        console.log('🚀 Dashboard initialized with auto-resolution support');
    }

    // Utility method for safe DOM element access
    safeGetElement(id) {
        const element = document.getElementById(id);
        if (!element) {
            console.warn(`⚠️ Element not found: ${id}`);
        }
        return element;
    }

    // Utility method for safe element text update
    safeUpdateText(id, text) {
        const element = this.safeGetElement(id);
        if (element) {
            element.textContent = text;
        }
    }

    // Utility method for fetch with timeout and error handling
    async fetchWithTimeout(url, options = {}, timeout = 10000) {
        const controller = new AbortController();
        const id = setTimeout(() => controller.abort(), timeout);
        
        try {
            const response = await fetch(url, {
                ...options,
                signal: controller.signal
            });
            clearTimeout(id);
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status} - ${response.statusText}`);
            }
            
            return response;
        } catch (error) {
            clearTimeout(id);
            if (error.name === 'AbortError') {
                throw new Error(`Request timeout (${timeout}ms) for ${url}`);
            }
            throw error;
        }
    }

    // Health check method
    async checkBackendHealth() {
        try {
            const response = await this.fetchWithTimeout('/api/health', {}, 5000);
            const health = await response.json();
            console.log('✅ Backend is healthy:', health);
            return true;
        } catch (error) {
            console.error('❌ Backend health check failed:', error);
            this.showNotification('Backend server is not responding', 'error');
            return false;
        }
    }

    async initialize() {
        console.log('🔧 Starting dashboard initialization...');
        
        // Check backend health first
        const isHealthy = await this.checkBackendHealth();
        if (!isHealthy) {
            console.error('❌ Backend unhealthy, skipping initialization');
            return;
        }
        
        this.initializeGraph();
        this.setupEventListeners();
        this.initializeCharts();
        this.connectWebSocket();
        this.loadInitialData();
        console.log('✅ Dashboard initialization complete');
    }

    initializeGraph() {
        console.log('📊 Initializing Cytoscape graph...');
        
        this.cy = cytoscape({
            container: document.getElementById('cy'),
            
            style: [
                {
                    selector: 'node[type="thread"]',
                    style: {
                        'background-color': '#667eea',
                        'label': 'data(label)',
                        'width': 60,
                        'height': 60,
                        'text-valign': 'center',
                        'text-halign': 'center',
                        'font-size': '12px',
                        'color': '#fff',
                        'text-outline-width': 2,
                        'text-outline-color': '#667eea'
                    }
                },
                {
                    selector: 'node[type="thread"].deadlocked',
                    style: {
                        'background-color': '#f56565',
                        'text-outline-color': '#f56565',
                        'border-width': 3,
                        'border-color': '#c53030'
                    }
                },
                {
                    selector: 'node[type="lock"]',
                    style: {
                        'background-color': '#48bb78',
                        'label': 'data(label)',
                        'shape': 'rectangle',
                        'width': 80,
                        'height': 50,
                        'text-valign': 'center',
                        'text-halign': 'center',
                        'font-size': '11px',
                        'color': '#fff',
                        'text-outline-width': 2,
                        'text-outline-color': '#48bb78'
                    }
                },
                {
                    selector: 'edge',
                    style: {
                        'width': 3,
                        'line-color': '#cbd5e0',
                        'target-arrow-color': '#cbd5e0',
                        'target-arrow-shape': 'triangle',
                        'curve-style': 'bezier',
                        'label': 'data(label)',
                        'font-size': '10px',
                        'text-rotation': 'autorotate',
                        'text-margin-y': -10
                    }
                },
                {
                    selector: 'edge.waiting',
                    style: {
                        'line-color': '#f56565',
                        'target-arrow-color': '#f56565',
                        'line-style': 'dashed'
                    }
                },
                {
                    selector: 'edge.held',
                    style: {
                        'line-color': '#48bb78',
                        'target-arrow-color': '#48bb78'
                    }
                }
            ],
            
            layout: {
                name: 'cose',
                animate: true,
                animationDuration: 500
            }
        });
        
        console.log('✅ Graph initialized successfully');
    }

    initializeCharts() {
        console.log('📊 Initializing charts...');
        
        // Initialize resolution time chart if container exists
        const chartContainer = this.safeGetElement('resolutionChart');
        if (chartContainer && typeof Chart !== 'undefined') {
            this.resolutionChart = new Chart(chartContainer, {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'Resolution Time (ms)',
                        data: [],
                        borderColor: '#4F46E5',
                        backgroundColor: 'rgba(79, 70, 229, 0.1)',
                        tension: 0.4,
                        fill: true
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: true,
                            position: 'top'
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            title: {
                                display: true,
                                text: 'Time (ms)'
                            }
                        },
                        x: {
                            title: {
                                display: true,
                                text: 'Time'
                            }
                        }
                    }
                }
            });
            console.log('✅ Resolution chart initialized');
        } else {
            console.log('⚠️ Chart.js not available or container not found');
        }
    }

    setupEventListeners() {
        console.log('🎧 Setting up event listeners...');
        
        // Refresh button
        const refreshBtn = document.getElementById('refresh-btn');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => {
                console.log('🔄 Manual refresh requested');
                this.loadInitialData();
            });
        }
        
        // Process selector
        const processSelect = document.getElementById('process-select');
        if (processSelect) {
            processSelect.addEventListener('change', (e) => {
                const pid = e.target.value;
                const displayName = e.target.options[e.target.selectedIndex].text;
                
                if (pid) {
                    this.selectProcess(pid, displayName);
                }
            });
        }
        
        // Snapshot button
        const snapshotBtn = document.getElementById('snapshot-btn');
        if (snapshotBtn) {
            snapshotBtn.addEventListener('click', () => {
                this.captureSnapshot();
            });
        }
        
        // Comparison button
        const comparisonBtn = document.getElementById('comparison-btn');
        if (comparisonBtn) {
            comparisonBtn.addEventListener('click', () => {
                this.showBeforeAfterComparison();
            });
        }
        
        // 🚀 AUTO-RESOLUTION: Toggle button
        const autoResBtn = document.getElementById('auto-res-toggle');
        if (autoResBtn) {
            autoResBtn.addEventListener('click', () => {
                this.toggleAutoResolution();
            });
        }
        
        // 🚀 AUTO-RESOLUTION: Manual trigger button
        const manualResBtn = document.getElementById('manual-res-trigger');
        if (manualResBtn) {
            manualResBtn.addEventListener('click', () => {
                this.triggerManualResolution();
            });
        }
        
        // 🚀 AUTO-RESOLUTION: Stats panel button
        const resStatsBtn = document.getElementById('res-stats-btn');
        if (resStatsBtn) {
            resStatsBtn.addEventListener('click', () => {
                this.toggleResolutionPanel();
            });
        }
        
        console.log('✅ Event listeners configured (including auto-resolution)');
    }

    // Method to update resolution time chart
    updateResolutionChart(resolutionTime) {
        if (this.resolutionChart) {
            const now = new Date().toLocaleTimeString();
            
            // Add new data point
            this.resolutionChart.data.labels.push(now);
            this.resolutionChart.data.datasets[0].data.push(resolutionTime);
            
            // Keep only last 20 data points
            if (this.resolutionChart.data.labels.length > 20) {
                this.resolutionChart.data.labels.shift();
                this.resolutionChart.data.datasets[0].data.shift();
            }
            
            this.resolutionChart.update('none'); // Update without animation for performance
            console.log(`📊 Chart updated with resolution time: ${resolutionTime}ms`);
        }
    }

    connectWebSocket() {
        console.log('🔌 Connecting to WebSocket...');
        
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, (frame) => {
            console.log('✅ WebSocket connected:', frame);
            this.isConnected = true;
            this.reconnectAttempts = 0; // Reset on successful connection
            this.updateConnectionStatus(true);
            
            // Subscribe to deadlock updates
            this.stompClient.subscribe('/topic/deadlock', (message) => {
                console.log('📨 Received deadlock update via WebSocket');
                const data = JSON.parse(message.body);
                console.log('📦 Deadlock data:', data);
                this.handleDeadlockUpdate(data);
            });
            
            // Subscribe to process list updates
            this.stompClient.subscribe('/topic/processes', (message) => {
                console.log('📨 Received process list update');
                const processes = JSON.parse(message.body);
                console.log('📦 Process list:', processes);
                this.updateProcessList(processes);
            });
            
            // Subscribe to resolution updates
            this.stompClient.subscribe('/topic/resolution', (message) => {
                console.log('📨 Received resolution update');
                const update = JSON.parse(message.body);
                this.handleResolutionUpdate(update);
            });
            
        }, (error) => {
            console.error('❌ WebSocket error:', error);
            this.isConnected = false;
            this.updateConnectionStatus(false);
            
            // Retry connection with limit
            if (this.reconnectAttempts < this.maxReconnectAttempts) {
                this.reconnectAttempts++;
                setTimeout(() => {
                    console.log(`🔄 Retrying WebSocket connection (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
                    this.connectWebSocket();
                }, this.reconnectDelay);
            } else {
                console.error('❌ Max reconnection attempts reached');
                this.showNotification('⚠️ Unable to connect to server. Please refresh the page.', 'error');
            }
        });
    }

    async loadInitialData() {
        console.log('📡 Loading initial data from API...');
        
        try {
            const response = await this.fetchWithTimeout('/api/state');
            const data = await response.json();
            console.log('✅ Initial data loaded:', data);
            this.handleDeadlockUpdate(data);
            
            // 🚀 AUTO-RESOLUTION: Load initial auto-resolution status and data
            await this.loadResolutionData();
            this.updateAutoResolutionBadge();
            this.updateAutoResolutionButton();
        } catch (error) {
            console.error('❌ Failed to load initial data:', error);
            this.showNotification(`Failed to load initial data: ${error.message}`, 'error');
        }
    }

    handleDeadlockUpdate(data) {
        console.log('🔄 Processing deadlock update:', data);
        
        this.currentSnapshot = data;
        
        // Update all UI components
        this.updateMetrics(data);
        this.updateGraph(data);
        this.updateSystemInfo(data);
        
        // Update status indicator
        if (data.deadlockDetected) {
            console.log('🔴 DEADLOCK DETECTED!');
            this.updateSystemStatus('error', '⚠️ Deadlock Detected');
            const deadlockedCount = data.threads && data.threads.filter ? 
                data.threads.filter(t => t.deadlocked).length : 0;
            this.addActivityEvent('error', 'Deadlock Detected', 
                `Found ${deadlockedCount} deadlocked threads`);
        } else {
            console.log('🟢 No deadlock - System healthy');
            this.updateSystemStatus('success', '✅ No Deadlock');
        }
        
        // Update resolution stats if available
        if (data.additionalData && data.additionalData.resolution) {
            this.updateResolutionStatsFromData(data.additionalData.resolution);
        }
        
        // Update last update time
        this.updateLastUpdateTime();
    }

    updateMetrics(data) {
        console.log('📊 Updating metrics with data:', data);
        
        // Total Threads
        const totalThreads = data.threads ? data.threads.length : 0;
        this.safeUpdateText('total-threads', totalThreads);
        console.log(`  ├─ Total Threads: ${totalThreads}`);
        
        // Active Locks
        const activeLocks = data.locks ? data.locks.length : 0;
        this.safeUpdateText('active-locks', activeLocks);
        console.log(`  ├─ Active Locks: ${activeLocks}`);
        
        // Deadlocked Threads
        const deadlockedThreads = data.threads ? 
            data.threads.filter(t => t.deadlocked === true).length : 0;
        this.safeUpdateText('deadlocked-threads', deadlockedThreads);
        console.log(`  ├─ Deadlocked Threads: ${deadlockedThreads}`);
        
        // Blocked Threads
        const blockedThreads = data.threads ? 
            data.threads.filter(t => 
                t.state === 'BLOCKED' || 
                t.state === 'WAITING' || 
                t.state === 'TIMED_WAITING'
            ).length : 0;
        document.getElementById('blocked-threads').textContent = blockedThreads;
        console.log(`  └─ Blocked Threads: ${blockedThreads}`);
        
        console.log('✅ Metrics updated successfully');
    }

    updateGraph(data) {
        if (!this.cy) {
            console.warn('⚠️ Graph not initialized');
            return;
        }
        
        console.log('📊 Updating graph visualization...');
        
        // Clear existing graph
        this.cy.elements().remove();
        
        const elements = [];
        
        // ✅ FIX: Create consistent lock ID mapping to avoid mismatches
        const lockIdMap = new Map();
        
        // Add thread nodes
        if (data.threads && Array.isArray(data.threads)) {
            console.log(`  📌 Adding ${data.threads.length} thread nodes...`);
            
            data.threads.forEach(thread => {
                const isDeadlocked = thread.deadlocked === true;
                
                elements.push({
                    group: 'nodes',
                    data: {
                        id: `thread-${thread.id}`,
                        label: thread.name || `Thread-${thread.id}`,
                        type: 'thread',
                        state: thread.state || 'UNKNOWN',
                        deadlocked: isDeadlocked
                    },
                    classes: isDeadlocked ? 'deadlocked' : ''
                });
                
                console.log(`    ✅ ${thread.name} (${isDeadlocked ? 'DEADLOCKED' : 'normal'})`);
            });
        }
        
        // Add lock nodes with consistent IDs
        if (data.locks && Array.isArray(data.locks)) {
            console.log(`  🔒 Adding ${data.locks.length} lock nodes...`);
            
            data.locks.forEach(lock => {
                // ✅ FIX: Always use identityHashCode for consistency
                const lockId = `lock-${lock.identityHashCode}`;
                lockIdMap.set(lock.identityHashCode, lockId);
                
                elements.push({
                    group: 'nodes',
                    data: {
                        id: lockId,
                        label: this.formatLockName(lock.className || lock.name || 'Lock'),
                        type: 'lock'
                    }
                });
                
                console.log(`    ✅ ${lockId}`);
            });
        }
        
        // Add edges using consistent IDs
        if (data.threads && Array.isArray(data.threads)) {
            console.log(`  🔗 Adding edges...`);
            
            data.threads.forEach(thread => {
                // Waiting for lock
                if (thread.waitingLock) {
                    // ✅ FIX: Use lockIdMap for consistent ID lookup
                    const lockId = lockIdMap.get(thread.waitingLock.identityHashCode) || 
                                 `lock-${thread.waitingLock.identityHashCode}`;
                    
                    elements.push({
                        group: 'edges',
                        data: {
                            source: `thread-${thread.id}`,
                            target: lockId,
                            label: 'waiting for'
                        },
                        classes: 'waiting'
                    });
                    
                    console.log(`    ⏳ ${thread.name} -> ${lockId} (waiting)`);
                }
                
                // Owned locks
                if (thread.ownedLocks && Array.isArray(thread.ownedLocks)) {
                    thread.ownedLocks.forEach(lock => {
                        // ✅ FIX: Use lockIdMap for consistent ID lookup
                        const lockId = lockIdMap.get(lock.identityHashCode) || 
                                     `lock-${lock.identityHashCode}`;
                        
                        elements.push({
                            group: 'edges',
                            data: {
                                source: lockId,
                                target: `thread-${thread.id}`,
                                label: 'held by'
                            },
                            classes: 'held'
                        });
                        
                        console.log(`    🔒 ${lockId} -> ${thread.name} (held)`);
                    });
                }
            });
        }
        
        console.log(`  📊 Total elements: ${elements.length}`);
        
        // Add to graph and layout
        if (elements.length > 0) {
            this.cy.add(elements);
            
            this.cy.layout({
                name: 'cose',
                animate: true,
                animationDuration: 500,
                nodeRepulsion: 8000,
                idealEdgeLength: 100,
                edgeElasticity: 100,
                gravity: 80
            }).run();
            
            console.log('✅ Graph updated and layout applied');
        } else {
            console.log('⚠️ No elements to display in graph');
        }
    }

    formatLockName(className) {
        if (!className) return 'Unknown';
        const parts = className.split('.');
        return parts[parts.length - 1];
    }

    updateProcessList(processes) {
        console.log(`📋 Updating process list (${processes.length} processes)...`);
        
        const select = document.getElementById('process-select');
        if (!select) {
            console.warn('⚠️ Process select element not found');
            return;
        }
        
        // Clear and rebuild
        select.innerHTML = '<option value="">Select a Java process...</option>';
        
        processes.forEach(process => {
            const option = document.createElement('option');
            option.value = process.pid;
            option.textContent = `${process.displayName} (PID: ${process.pid})`;
            select.appendChild(option);
            
            console.log(`  ✅ ${process.displayName} (PID: ${process.pid})`);
        });
        
        console.log('✅ Process list updated');
    }

    selectProcess(pid, displayName) {
        console.log(`🎯 Selecting process: ${displayName} (PID: ${pid})`);
        
        this.selectedProcessPid = pid;
        this.selectedProcessName = displayName;
        
        // ✅ FIX: Update process info panel with selected process details
        const currentPidElem = document.getElementById('current-pid');
        if (currentPidElem) {
            currentPidElem.textContent = pid;
            currentPidElem.style.color = '#10b981';
            currentPidElem.style.fontWeight = '600';
        }
        
        const processStatusElem = document.getElementById('process-status');
        if (processStatusElem) {
            processStatusElem.textContent = '🟢 Monitoring';
            processStatusElem.classList.remove('status-error');
            processStatusElem.classList.add('status-active');
            processStatusElem.style.color = '#10b981';
        }
        
        console.log(`✅ Process panel updated: PID ${pid}, Name: ${displayName}`);
        
        // Notify backend with proper error handling
        this.fetchWithTimeout(`/api/monitor/${pid}`, { method: 'POST' })
            .then(response => response.text())
            .then(message => {
                console.log('✅ Backend acknowledged selection:', message);
                this.addActivityEvent('info', 'Process Selected', 
                    `Now monitoring ${displayName} (PID: ${pid})`);
            })
            .catch(error => {
                console.error('❌ Failed to notify backend:', error);
                this.showNotification(`Failed to select process: ${error.message}`, 'error');
            });
    }

    captureSnapshot() {
        if (!this.currentSnapshot) {
            alert('⚠️ No data available to capture');
            return;
        }
        
        console.log('📸 Capturing snapshot...');
        
        const snapshot = {
            timestamp: new Date().toISOString(),
            processName: this.selectedProcessName || 'Unknown',
            processPid: this.selectedProcessPid || 'Unknown',
            ...this.currentSnapshot
        };
        
        this.snapshots.push(snapshot);
        
        // Download snapshot
        const blob = new Blob([JSON.stringify(snapshot, null, 2)], 
            { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `deadlock-snapshot-${Date.now()}.json`;
        a.click();
        
        console.log(`✅ Snapshot captured (${this.snapshots.length} total)`);
        
        alert(`✅ Snapshot captured!\n\nTotal snapshots: ${this.snapshots.length}\nDownloaded to your Downloads folder`);
    }

    showBeforeAfterComparison() {
        if (this.snapshots.length < 2) {
            alert('⚠️ Need at least 2 snapshots to compare.\n\nCapture more snapshots first!');
            return;
        }
        
        console.log('📊 Showing before/after comparison...');
        
        const before = this.snapshots[this.snapshots.length - 2];
        const after = this.snapshots[this.snapshots.length - 1];
        
        const beforeThreads = before.threads && before.threads.length ? before.threads.length : 0;
        const beforeLocks = before.locks && before.locks.length ? before.locks.length : 0;
        const afterThreads = after.threads && after.threads.length ? after.threads.length : 0;
        const afterLocks = after.locks && after.locks.length ? after.locks.length : 0;
        
        const comparison = {
            before: {
                timestamp: before.timestamp,
                threads: beforeThreads,
                locks: beforeLocks,
                deadlocked: before.deadlockDetected
            },
            after: {
                timestamp: after.timestamp,
                threads: afterThreads,
                locks: afterLocks,
                deadlocked: after.deadlockDetected
            },
            changes: {
                threads: afterThreads - beforeThreads,
                locks: afterLocks - beforeLocks,
                deadlockCleared: before.deadlockDetected && !after.deadlockDetected
            }
        };
        
        console.log('📊 Comparison:', comparison);
        
        alert(`📊 Before/After Comparison\n\n` +
              `BEFORE (${new Date(before.timestamp).toLocaleTimeString()}):\n` +
              `  Threads: ${comparison.before.threads}\n` +
              `  Locks: ${comparison.before.locks}\n` +
              `  Deadlock: ${comparison.before.deadlocked ? 'YES' : 'NO'}\n\n` +
              `AFTER (${new Date(after.timestamp).toLocaleTimeString()}):\n` +
              `  Threads: ${comparison.after.threads}\n` +
              `  Locks: ${comparison.after.locks}\n` +
              `  Deadlock: ${comparison.after.deadlocked ? 'YES' : 'NO'}\n\n` +
              `CHANGES:\n` +
              `  Threads: ${comparison.changes.threads >= 0 ? '+' : ''}${comparison.changes.threads}\n` +
              `  Locks: ${comparison.changes.locks >= 0 ? '+' : ''}${comparison.changes.locks}\n` +
              `  ${comparison.changes.deadlockCleared ? '✅ Deadlock CLEARED!' : '⚠️ Status unchanged'}`);
    }

    updateSystemInfo(data) {
        // Update JVM info if available
        if (data.additionalData && data.additionalData.jvmInfo) {
            const jvmInfo = data.additionalData.jvmInfo;
            document.getElementById('jvm-vendor').textContent = jvmInfo.vendor || 'N/A';
            document.getElementById('jvm-version').textContent = jvmInfo.version || 'N/A';
        }
        
        // Update selected process info
        if (this.selectedProcessPid) {
            document.getElementById('monitored-process').textContent = 
                `${this.selectedProcessName} (PID: ${this.selectedProcessPid})`;
        }
    }

    updateResolutionStatsFromData(resolutionData) {
        if (!resolutionData) {
            // ✅ FIX: Set defaults when no resolution data available
            console.log('⚠️ No resolution data available, setting defaults');
            const totalResElem = document.getElementById('total-resolutions');
            const successRateElem = document.getElementById('success-rate');
            const avgTimeElem = document.getElementById('avg-resolution-time');
            const lastDetectionElem = document.getElementById('last-detection');
            
            if (totalResElem) totalResElem.textContent = '0';
            if (successRateElem) successRateElem.textContent = '0.0%';
            if (avgTimeElem) avgTimeElem.textContent = '0ms';
            if (lastDetectionElem) lastDetectionElem.textContent = 'Never';
            return;
        }
        
        console.log('📈 Updating resolution stats:', resolutionData);
        
        const totalResolutions = resolutionData.totalResolutions || 0;
        const recentEvents = resolutionData.recentEvents || [];
        
        const totalResElem = document.getElementById('total-resolutions');
        if (totalResElem) {
            totalResElem.textContent = totalResolutions;
        }
        
        if (totalResolutions > 0 && recentEvents.length > 0) {
            const successful = recentEvents.filter(e => e.successful || e.status === 'SUCCESS').length;
            const successRate = ((successful / recentEvents.length) * 100).toFixed(1);
            
            const successRateElem = document.getElementById('success-rate');
            if (successRateElem) {
                successRateElem.textContent = `${successRate}%`;
            }
            
            const avgTime = recentEvents.reduce((sum, e) => sum + (e.resolutionTime || e.duration || 0), 0) / recentEvents.length;
            const avgTimeElem = document.getElementById('avg-resolution-time');
            if (avgTimeElem) {
                avgTimeElem.textContent = `${Math.round(avgTime)}ms`;
            }
        } else {
            // No resolution events yet
            const successRateElem = document.getElementById('success-rate');
            if (successRateElem) successRateElem.textContent = '0.0%';
            
            const avgTimeElem = document.getElementById('avg-resolution-time');
            if (avgTimeElem) avgTimeElem.textContent = '0ms';
        }
        
        // ✅ FIX: Update last detection time properly
        const lastDetectionElem = document.getElementById('last-detection');
        if (lastDetectionElem) {
            if (recentEvents.length > 0) {
                const lastEvent = recentEvents[recentEvents.length - 1];
                const lastTime = new Date(lastEvent.timestamp).toLocaleTimeString();
                lastDetectionElem.textContent = lastTime;
                console.log(`✅ Last detection: ${lastTime}`);
            } else {
                lastDetectionElem.textContent = 'Never';
            }
        }
    }

    handleResolutionUpdate(update) {
        console.log('⚡ Resolution update:', update);
        
        // Update auto-resolution status badge
        this.updateAutoResolutionBadge();
        
        // Update chart if resolution time is available
        if (update.resolutionTime && update.resolutionTime > 0) {
            this.updateResolutionChart(update.resolutionTime);
        }
        
        // Show notification based on update type
        const status = update.status || 'RESOLVING';
        const message = update.message || 'Auto-resolution triggered';
        
        if (status === 'RESOLVED' || status === 'SUCCESS') {
            this.addActivityEvent('success', '✅ Resolution Success', message);
            this.showNotification('Deadlock Resolved!', 'success');
        } else if (status === 'FAILED') {
            this.addActivityEvent('error', '❌ Resolution Failed', message);
            this.showNotification('Resolution Failed', 'error');
        } else {
            this.addActivityEvent('warning', '⚡ Resolution Attempt', message);
        }
        
        // Refresh resolution panel if visible
        if (this.isShowingResolutionPanel) {
            this.loadResolutionData();
        }
    }

    showNotification(message, type = 'info') {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        notification.style.cssText = `
            position: fixed;
            top: 80px;
            right: 20px;
            padding: 15px 25px;
            background: ${type === 'success' ? '#48bb78' : type === 'error' ? '#f56565' : '#4299e1'};
            color: white;
            border-radius: 8px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            z-index: 10000;
            animation: slideIn 0.3s ease-out;
        `;
        
        document.body.appendChild(notification);
        
        // Auto-remove after 3 seconds
        setTimeout(() => {
            notification.style.animation = 'slideOut 0.3s ease-out';
            setTimeout(() => notification.remove(), 300);
        }, 3000);
    }
    
    // 🚀 AUTO-RESOLUTION: Toggle auto-resolution ON/OFF
    async toggleAutoResolution() {
        try {
            console.log('🔄 Toggling auto-resolution...');
            
            const response = await this.fetchWithTimeout('/api/resolution/toggle', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            });
            
            const result = await response.json();
            this.autoResolutionEnabled = result.autoResolutionEnabled;
            
            console.log('✅ Auto-resolution toggled:', this.autoResolutionEnabled);
            
            // Update UI
            this.updateAutoResolutionBadge();
            this.updateAutoResolutionButton();
            
            // Show notification
            this.showNotification(
                `Auto-Resolution ${this.autoResolutionEnabled ? 'ENABLED' : 'DISABLED'}`,
                this.autoResolutionEnabled ? 'success' : 'warning'
            );
            
            // Refresh resolution panel if visible
            if (this.isShowingResolutionPanel) {
                this.loadResolutionData();
            }
            
        } catch (error) {
            console.error('❌ Failed to toggle auto-resolution:', error);
            this.showNotification('Failed to toggle auto-resolution', 'error');
        }
    }
    
    // 🚀 AUTO-RESOLUTION: Manually trigger resolution
    async triggerManualResolution() {
        try {
            console.log('⚡ Triggering manual resolution...');
            
            const response = await this.fetchWithTimeout('/api/resolution/trigger', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            });
            
            const result = await response.json();
            
            console.log('✅ Manual resolution triggered:', result);
            
            this.showNotification('Manual resolution triggered!', 'info');
            this.addActivityEvent('info', '🔧 Manual Resolution', result.message || 'Resolution triggered by user');
            
            // Refresh resolution panel if visible
            if (this.isShowingResolutionPanel) {
                setTimeout(() => this.loadResolutionData(), 1000);
            }
            
        } catch (error) {
            console.error('❌ Failed to trigger manual resolution:', error);
            this.showNotification('Failed to trigger resolution', 'error');
        }
    }
    
    // 🚀 AUTO-RESOLUTION: Toggle resolution panel visibility
    toggleResolutionPanel() {
        this.isShowingResolutionPanel = !this.isShowingResolutionPanel;
        
        if (this.isShowingResolutionPanel) {
            this.showResolutionPanel();
        } else {
            this.hideResolutionPanel();
        }
    }
    
    // 🚀 AUTO-RESOLUTION: Show resolution statistics panel
    async showResolutionPanel() {
        console.log('📊 Showing resolution panel...');
        
        let panel = document.getElementById('resolution-panel');
        
        if (!panel) {
            panel = this.createResolutionPanel();
            document.body.appendChild(panel);
        }
        
        panel.style.display = 'block';
        
        // Load resolution data
        await this.loadResolutionData();
    }
    
    // 🚀 AUTO-RESOLUTION: Hide resolution panel
    hideResolutionPanel() {
        const panel = document.getElementById('resolution-panel');
        if (panel) {
            panel.style.display = 'none';
        }
    }
    
    // 🚀 AUTO-RESOLUTION: Create resolution panel HTML
    createResolutionPanel() {
        const panel = document.createElement('div');
        panel.id = 'resolution-panel';
        panel.className = 'resolution-panel';
        panel.innerHTML = `
            <div class="resolution-panel-header">
                <h2>🔧 Auto-Resolution Monitor</h2>
                <button class="close-btn" onclick="document.getElementById('resolution-panel').style.display='none'">×</button>
            </div>
            <div class="resolution-panel-body">
                <div class="resolution-stats">
                    <div class="stat-card">
                        <div class="stat-label">Total Resolutions</div>
                        <div class="stat-value" id="res-total">0</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-label">Success Rate</div>
                        <div class="stat-value" id="res-success-rate">0%</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-label">Avg Resolution Time</div>
                        <div class="stat-value" id="res-avg-time">0ms</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-label">Auto-Resolution</div>
                        <div class="stat-value" id="res-status">ON</div>
                    </div>
                </div>
                <div class="resolution-history">
                    <h3>Resolution History</h3>
                    <div id="res-history-list" class="history-list">
                        <p class="loading">Loading...</p>
                    </div>
                </div>
            </div>
        `;
        
        // Add styles
        const style = document.createElement('style');
        style.textContent = `
            .resolution-panel {
                position: fixed;
                top: 70px;
                right: 20px;
                width: 400px;
                max-height: 80vh;
                background: white;
                border-radius: 12px;
                box-shadow: 0 8px 32px rgba(0,0,0,0.2);
                z-index: 9999;
                overflow: hidden;
                display: none;
            }
            .resolution-panel-header {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                padding: 15px 20px;
                display: flex;
                justify-content: space-between;
                align-items: center;
            }
            .resolution-panel-header h2 {
                margin: 0;
                font-size: 18px;
            }
            .close-btn {
                background: rgba(255,255,255,0.2);
                border: none;
                color: white;
                font-size: 24px;
                width: 30px;
                height: 30px;
                border-radius: 50%;
                cursor: pointer;
                line-height: 1;
            }
            .close-btn:hover {
                background: rgba(255,255,255,0.3);
            }
            .resolution-panel-body {
                padding: 20px;
                max-height: calc(80vh - 70px);
                overflow-y: auto;
            }
            .resolution-stats {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 15px;
                margin-bottom: 20px;
            }
            .stat-card {
                background: #f7fafc;
                padding: 15px;
                border-radius: 8px;
                text-align: center;
            }
            .stat-label {
                font-size: 12px;
                color: #718096;
                margin-bottom: 5px;
            }
            .stat-value {
                font-size: 24px;
                font-weight: bold;
                color: #2d3748;
            }
            .resolution-history h3 {
                margin-top: 0;
                margin-bottom: 15px;
                font-size: 16px;
            }
            .history-list {
                max-height: 300px;
                overflow-y: auto;
            }
            .history-item {
                background: #f7fafc;
                padding: 12px;
                border-radius: 6px;
                margin-bottom: 10px;
                border-left: 4px solid #48bb78;
            }
            .history-item.failed {
                border-left-color: #f56565;
            }
            .history-time {
                font-size: 11px;
                color: #718096;
            }
            .history-method {
                font-weight: bold;
                color: #2d3748;
                margin-top: 5px;
            }
            .history-details {
                font-size: 12px;
                color: #4a5568;
                margin-top: 5px;
            }
        `;
        document.head.appendChild(style);
        
        return panel;
    }
    
    // 🚀 AUTO-RESOLUTION: Load resolution data from backend
    async loadResolutionData() {
        try {
            // Fetch statistics
            const statsResponse = await this.fetchWithTimeout('/api/resolution/stats');
            const stats = await statsResponse.json();
            this.updateResolutionStats(stats);
            
            // Fetch history
            const historyResponse = await this.fetchWithTimeout('/api/resolution/history');
            const history = await historyResponse.json();
            this.updateResolutionHistory(history);
            
        } catch (error) {
            console.error('❌ Failed to load resolution data:', error);
            this.showNotification(`Failed to load resolution data: ${error.message}`, 'error');
        }
    }
    
    // 🚀 AUTO-RESOLUTION: Update statistics display
    updateResolutionStats(stats) {
        const totalElem = document.getElementById('res-total');
        const successRateElem = document.getElementById('res-success-rate');
        const avgTimeElem = document.getElementById('res-avg-time');
        const statusElem = document.getElementById('res-status');
        
        if (totalElem) totalElem.textContent = stats.totalResolutions || 0;
        if (successRateElem) successRateElem.textContent = `${stats.successRate || 0}%`;
        if (avgTimeElem) avgTimeElem.textContent = `${stats.avgResolutionTime || 0}ms`;
        if (statusElem) {
            statusElem.textContent = stats.autoResolutionEnabled ? 'ON' : 'OFF';
            statusElem.style.color = stats.autoResolutionEnabled ? '#48bb78' : '#f56565';
        }
        
        // Update internal state
        this.autoResolutionEnabled = stats.autoResolutionEnabled;
        this.updateAutoResolutionBadge();
        this.updateAutoResolutionButton();
    }
    
    // 🚀 AUTO-RESOLUTION: Update history display
    updateResolutionHistory(history) {
        const historyList = document.getElementById('res-history-list');
        if (!historyList) return;
        
        if (!history || !history.recentEvents || history.recentEvents.length === 0) {
            historyList.innerHTML = '<p class="loading">No resolution history yet</p>';
            return;
        }
        
        historyList.innerHTML = history.recentEvents.map(event => `
            <div class="history-item ${event.status === 'FAILED' ? 'failed' : ''}">
                <div class="history-time">${new Date(event.timestamp).toLocaleString()}</div>
                <div class="history-method">${event.method || 'Unknown Method'}</div>
                <div class="history-details">${event.details || 'No details'}</div>
                <div class="history-details">Duration: ${event.resolutionTime || 0}ms</div>
            </div>
        `).join('');
    }
    
    // 🚀 AUTO-RESOLUTION: Update the navbar status badge
    updateAutoResolutionBadge() {
        const badge = document.getElementById('auto-resolution-status');
        if (badge) {
            badge.className = `status-badge ${this.autoResolutionEnabled ? 'status-enabled' : 'status-disabled'}`;
            badge.innerHTML = `
                <span class="status-dot"></span>
                ${this.autoResolutionEnabled ? 'Enabled' : 'Disabled'}
            `;
        }
    }
    
    // 🚀 AUTO-RESOLUTION: Update the toggle button
    updateAutoResolutionButton() {
        const button = document.getElementById('auto-res-toggle');
        if (button) {
            button.textContent = `🔄 Auto-Resolution: ${this.autoResolutionEnabled ? 'ON' : 'OFF'}`;
            button.className = `btn ${this.autoResolutionEnabled ? 'btn-success' : 'btn-warning'}`;
            button.style.background = this.autoResolutionEnabled ? '#48bb78' : '#ed8936';
        }
    }

    updateConnectionStatus(connected) {
        const indicator = document.getElementById('connection-status');
        if (indicator) {
            indicator.className = `status-indicator ${connected ? 'connected' : 'disconnected'}`;
            indicator.textContent = connected ? 'Connected' : 'Disconnected';
        }
    }

    updateSystemStatus(type, message) {
        const statusElement = document.getElementById('system-status');
        if (statusElement) {
            statusElement.className = `alert alert-${type === 'error' ? 'danger' : 'success'}`;
            statusElement.textContent = message;
        }
    }

    updateLastUpdateTime() {
        const timeElement = document.getElementById('last-update');
        if (timeElement) {
            timeElement.textContent = new Date().toLocaleTimeString();
        }
    }

    // Utility method for escaping HTML to prevent XSS
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    addActivityEvent(type, title, message) {
        const timeline = this.safeGetElement('activity-timeline');
        if (!timeline) return;
        
        const event = document.createElement('div');
        event.className = `timeline-event ${type}`;
        event.innerHTML = `
            <div class="event-time">${new Date().toLocaleTimeString()}</div>
            <div class="event-title">${this.escapeHtml(title)}</div>
            <div class="event-message">${this.escapeHtml(message)}</div>
        `;
        
        timeline.insertBefore(event, timeline.firstChild);
        
        // Keep only last 10 events and clean up old ones
        while (timeline.children.length > 10) {
            const removed = timeline.removeChild(timeline.lastChild);
            // Clean up any event listeners if attached
            removed.replaceWith(removed.cloneNode(true));
        }
    }
}

// Initialize dashboard when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    console.log('🌟 Starting Deadlock Detection Dashboard...');
    const dashboard = new DeadlockDashboard();
    dashboard.initialize();
});
