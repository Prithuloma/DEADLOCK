// =========================
// 🧠 Deadlock Dashboard Script
// =========================

// Initialize Cytoscape Graph
let cy = cytoscape({
  container: document.getElementById('cy'),
  style: [
    {
      selector: 'node',
      style: {
        'background-color': '#4F46E5',
        'label': 'data(label)',
        'color': '#fff',
        'text-valign': 'center',
        'text-halign': 'center',
        'font-size': '12px',
      },
    },
    {
      selector: 'node.thread-deadlocked',
      style: {
        'background-color': '#DC2626',
        'border-width': 3,
        'border-color': '#ef4444',
      },
    },
    {
      selector: 'node.lock',
      style: {
        'shape': 'rectangle',
        'background-color': '#059669',
      },
    },
    {
      selector: 'edge',
      style: {
        'width': 2,
        'line-color': '#ccc',
        'target-arrow-color': '#ccc',
        'target-arrow-shape': 'triangle',
        'curve-style': 'bezier',
      },
    },
  ],
  layout: { name: 'breadthfirst' },
});

// =========================
// 🌐 Backend API Functions
// =========================
function loadThreadGraph() {
  fetch('/api/threads/graph')
    .then((res) => res.json())
    .then((data) => {
      cy.elements().remove();
      cy.add(data.nodes);
      cy.add(data.edges);
      cy.layout({ name: 'breadthfirst' }).run();
    })
    .catch((err) => console.error('Error loading graph:', err));
}

function loadMetrics() {
  fetch('/api/threads/metrics')
    .then((res) => res.json())
    .then((data) => {
      document.getElementById('total-threads').textContent = data.totalThreads;
      document.getElementById('active-locks').textContent = data.activeLocks;
      document.getElementById('blocked-threads').textContent = data.blockedThreads;
      document.getElementById('deadlocked-threads').textContent = data.deadlockedThreads;
      document.getElementById('last-update').textContent = new Date().toLocaleTimeString();
    })
    .catch((err) => console.error('Error loading metrics:', err));
}

// =========================
// 🔄 Auto Refresh Function
// =========================
function refreshDashboard() {
  loadThreadGraph();
  loadMetrics();
}

// =========================
// ⚡ WebSocket Setup
// =========================
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function () {
  console.log('🟢 Connected to WebSocket');

  document.getElementById('connection-status').classList.remove('status-connecting');
  document.getElementById('connection-status').classList.add('status-connected');
  document.getElementById('connection-status').innerHTML = '<span class="status-dot"></span>Connected';

  stompClient.subscribe('/topic/deadlocks', function (message) {
    const event = JSON.parse(message.body);
    console.log('📡 WebSocket Event:', event);

    // Update dashboard metrics
    refreshDashboard();

    // Handle resolution event
    if (event.event === 'DEADLOCK_RESOLVED') {
      console.log('✅ Deadlock resolved, refreshing visualization...');
      showToast('Deadlock Resolved!', 'success');
      refreshDashboard();
    }

    // Handle detection event
    if (event.event === 'DEADLOCK_DETECTED') {
      console.log('⚠️ Deadlock detected!');
      showToast('Deadlock Detected!', 'warning');
      refreshDashboard();
    }
  });
});

// =========================
// 🧭 UI Handlers
// =========================
document.getElementById('refresh-btn').addEventListener('click', () => {
  refreshDashboard();
  showToast('Dashboard refreshed!', 'info');
});

// Toast Notification System
function showToast(message, type) {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
}

// =========================
// 🚀 Initial Load
// =========================
window.addEventListener('load', () => {
  loadThreadGraph();
  loadMetrics();
  console.log('📊 Dashboard initialized.');
});
