const { Client } = require('@stomp/stompjs');
const WebSocket = require('ws');

const wsUrl = process.env.WS_URL || 'ws://localhost:8080/ws';
const topic = process.env.STOMP_TOPIC || '/topic/tasks';

const client = new Client({
  brokerURL: wsUrl,
  webSocketFactory: () => new WebSocket(wsUrl),
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  debug: (msg) => console.log('[STOMP]', msg),
});

client.onConnect = () => {
  console.log('Connected to', wsUrl);
  console.log('Subscribed to', topic);

  client.subscribe(topic, (message) => {
    const now = new Date().toISOString();
    console.log('[' + now + ']', message.body);
  });
};

client.onStompError = (frame) => {
  console.error('STOMP error:', frame.headers['message']);
  console.error('Details:', frame.body);
};

client.onWebSocketError = (err) => {
  console.error('WebSocket error:', err && err.message ? err.message : err);
};

client.onWebSocketClose = () => {
  console.log('WebSocket closed. Reconnecting if configured...');
};

client.activate();

process.on('SIGINT', async () => {
  console.log('\nDisconnecting...');
  await client.deactivate();
  process.exit(0);
});
