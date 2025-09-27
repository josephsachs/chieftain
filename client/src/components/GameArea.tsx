import { useState, useEffect, useRef } from 'react';

interface LogMessage {
  timestamp: string;
  message: string;
}

interface MapTile {
  _id: string;
  type: string;
  [key: string]: any;
}

const GameArea = () => {
  const [connectionStatus, setConnectionStatus] = useState('disconnected');
  const [messages, setMessages] = useState<LogMessage[]>([]);
  const [connectionId, setConnectionId] = useState<string | null>(null);
  const [upSocket, setUpSocket] = useState<WebSocket | null>(null);
  const [downSocket, setDownSocket] = useState<WebSocket | null>(null);
  const [mapData, setMapData] = useState<MapTile[]>([]);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const addMessage = (message: string) => {
    const timestamp = new Date().toLocaleTimeString();
    setMessages(prev => [...prev.slice(-19), { timestamp, message }]); // Keep last 20 messages
  };

  const connectUpSocket = async () => {
    try {
      addMessage('Connecting to up socket...');
      // Connect to the HTTP endpoint that gets upgraded to WebSocket
      const ws = new WebSocket('ws://localhost:4225/command');
      
      ws.onopen = () => {
        addMessage('Connected to up socket');
        setUpSocket(ws);
        setConnectionStatus('connected-up');
      };

      ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          addMessage(`UP: ${JSON.stringify(message)}`);
          
          if (message.type === 'connection_confirm') {
            const connId = message.connectionId;
            setConnectionId(connId);
            addMessage(`Got connection ID: ${connId}`);
            
            // Send sync request
            const syncRequest = {
              command: "sync",
              timestamp: Date.now()
            };
            ws.send(JSON.stringify(syncRequest));
            addMessage('Sent sync request');
            
            // Now connect down socket
            connectDownSocket(connId);
          } else if (message.type === 'sync' && message.data) {
            if (message.data.entities) {
              addMessage(`Received ${message.data.entities.length} entities from sync`);
              // Filter for MapTile entities
              const mapTiles = message.data.entities.filter((e: any) => e.type === 'MapTile');
              setMapData(mapTiles);
              addMessage(`Found ${mapTiles.length} map tiles`);
            }
          }
        } catch (e) {
          addMessage(`Error parsing up socket message: ${e instanceof Error ? e.message : 'Unknown error'}`);
        }
      };

      ws.onclose = () => {
        addMessage('Up socket closed');
        setConnectionStatus('disconnected');
        setUpSocket(null);
      };

      ws.onerror = (error) => {
        addMessage(`Up socket error: ${error}`);
      };

    } catch (error) {
      addMessage(`Failed to connect up socket: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  };

  const connectDownSocket = async (connId: string) => {
    try {
      addMessage('Connecting to down socket...');
      // Connect to the HTTP endpoint that gets upgraded to WebSocket  
      const ws = new WebSocket('ws://localhost:4226/update');
      
      ws.onopen = () => {
        addMessage('Connected to down socket');
        setDownSocket(ws);
        setConnectionStatus('fully-connected');
        
        // Send connection ID
        const connectionMessage = { connectionId: connId };
        ws.send(JSON.stringify(connectionMessage));
        addMessage('Sent connection ID to down socket');
      };

      ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          addMessage(`DOWN: ${JSON.stringify(message)}`);
          
          if (message.type === 'down_socket_confirm') {
            addMessage('Down socket confirmed');
          } else if (message.type === 'update_batch' && message.updates) {
            const updateCount = Object.keys(message.updates).length;
            addMessage(`Received batch update with ${updateCount} entities`);
            
            // Update map data with any MapTile updates
            Object.values(message.updates).forEach((update: any) => {
              if (update.type === 'MapTile') {
                setMapData(prev => {
                  const newData = [...prev];
                  const index = newData.findIndex(tile => tile._id === update._id);
                  if (index >= 0) {
                    newData[index] = update;
                  } else {
                    newData.push(update);
                  }
                  return newData;
                });
              }
            });
          }
        } catch (e) {
          addMessage(`Error parsing down socket message: ${e instanceof Error ? e.message : 'Unknown error'}`);
        }
      };

      ws.onclose = () => {
        addMessage('Down socket closed');
        setConnectionStatus('connected-up');
        setDownSocket(null);
      };

      ws.onerror = (error) => {
        addMessage(`Down socket error: ${error}`);
      };

    } catch (error) {
      addMessage(`Failed to connect down socket: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  };

  // Auto-connect on mount
  useEffect(() => {
    let cancelled = false;
    
    const connect = async () => {
      if (!cancelled) {
        connectUpSocket();
      }
    };
    
    connect();
    
    // Cleanup function
    return () => {
      cancelled = true;
      if (upSocket) {
        upSocket.close();
      }
      if (downSocket) {
        downSocket.close();
      }
    };
  }, []);

  return (
    <div className="h-screen w-screen bg-gray-900 flex flex-col">
      {/* Debug Messages - inline on page */}
      <div className="p-2">
        <div className="text-green-400 font-mono text-xs mb-1">
          Status: {connectionStatus}
        </div>
        {messages.slice(-5).map((msg, index) => (
          <div key={index} className="text-green-400 font-mono text-xs leading-tight opacity-60">
            <span className="text-gray-500">{msg.timestamp}</span> {msg.message}
          </div>
        ))}
      </div>

      {/* Game Map */}
      <div className="flex-1">
        {connectionStatus === 'fully-connected' && (
          <GameMap mapData={mapData} />
        )}
      </div>
    </div>
  );
};

// Placeholder GameMap component
const GameMap = ({ mapData }: { mapData: MapTile[] }) => {
  return (
    <div className="w-full h-full bg-gray-800 flex items-center justify-center">
      <div className="text-white font-mono">
        <div>Map loaded with {mapData.length} tiles</div>
        <div className="text-sm text-gray-400 mt-2">
          Canvas rendering will go here
        </div>
      </div>
    </div>
  );
};

export default GameArea;