import { useState, useEffect, useRef } from 'react';
import GameMap from './GameMap';
import { GameEntity, extractEntitiesFromMessage } from '../models/GameEntity';
import { Clan, getClanCoordinates } from '../models/Clan';

interface LogMessage {
  timestamp: string;
  message: string;
  type: 'info' | 'error' | 'console';
}

const GameArea = () => {
  const [connectionStatus, setConnectionStatus] = useState('disconnected');
  const [messages, setMessages] = useState<LogMessage[]>([]);
  const [connectionId, setConnectionId] = useState<string | null>(null);
  const [upSocket, setUpSocket] = useState<WebSocket | null>(null);
  const [downSocket, setDownSocket] = useState<WebSocket | null>(null);
  const [entities, setEntities] = useState<GameEntity[]>([]);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const addMessage = (message: string, type: 'info' | 'error' | 'console' = 'info') => {
    const timestamp = new Date().toLocaleTimeString();
    setMessages(prev => [...prev.slice(-49), { timestamp, message, type }]); // Keep last 50 messages
  };

  // Filter messages to display only certain types
  const shouldDisplayMessage = (message: any): boolean => {
    // Skip heartbeat messages
    if (message.type === 'heartbeat' || message.type === 'heartbeat_response') {
      return false;
    }

    // Skip entity sync messages unless they're explicit console messages
    if (message.type === 'entity_sync' || message.type === 'sync') {
      return false;
    }

    // Display messages with 'console' field
    if (message.console) {
      addMessage(message.console, 'console');
      return true;
    }

    return true;
  };

  // Process entities from incoming messages
  const updateEntities = (newEntities: GameEntity[]) => {
    setEntities(prev => {
      const updated = [...prev];

      newEntities.forEach(newEntity => {
        const index = updated.findIndex(e => e._id === newEntity._id);

        if (index >= 0) {
          // Check if this is a delta update
          const isDeltaUpdate = 'delta' in newEntity && newEntity.operation === 'update';

          if (isDeltaUpdate) {
            // Merge the delta with the existing entity
            const existingEntity = updated[index];
            const mergedEntity = {
              ...existingEntity,
              version: newEntity.version
            };

            // Apply deltas to the state
            if (newEntity.delta) {
              // Initialize state object if it doesn't exist
              if (!mergedEntity.state) {
                mergedEntity.state = {};
              }

              // Now we know state exists
              const state = mergedEntity.state;

              // For each property in delta, update the corresponding property in state
              Object.entries(newEntity.delta).forEach(([key, value]) => {
                if (key === 'location') {
                  // Special handling for location since it's nested
                  if (!state.location) {
                    state.location = {};
                  }

                  state.location = {
                    ...state.location,
                    ...value
                  };
                } else {
                  state[key] = value;
                }
              });
            }

            // Check if it's a Clan and if it moved
            if (existingEntity.type === 'Clan') {
              const oldClan = existingEntity as Clan;
              const newClan = mergedEntity as Clan;

              const oldCoords = getClanCoordinates(oldClan);
              const newCoords = getClanCoordinates(newClan);

              if (oldCoords && newCoords &&
                  (oldCoords[0] !== newCoords[0] || oldCoords[1] !== newCoords[1])) {
                // Log the movement
                addMessage(`Clan ${oldClan.state?.name} moved from (${oldCoords[0]}, ${oldCoords[1]}) to (${newCoords[0]}, ${newCoords[1]})`, 'console');
              }
            }

            // Update with the merged entity
            updated[index] = mergedEntity;
          } else {
            // Full entity replacement (not a delta)
            updated[index] = newEntity;
          }
        } else {
          // Add new entity
          updated.push(newEntity);

          // If it's a new Clan, log it
          if (newEntity.type === 'Clan') {
            const coords = getClanCoordinates(newEntity as Clan);
            if (coords) {
              addMessage(`New Clan ${(newEntity as Clan).state?.name} at (${coords[0]}, ${coords[1]})`, 'console');
            }
          }
        }
      });

      return updated;
    });
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

          // Only log if it's a message we want to display
          if (shouldDisplayMessage(message)) {
            addMessage(`UP: ${JSON.stringify(message)}`);
          }

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

              // Extract entities from the sync message
              const receivedEntities = extractEntitiesFromMessage(message);
              updateEntities(receivedEntities);
            }
          }
        } catch (e) {
          addMessage(`Error parsing up socket message: ${e instanceof Error ? e.message : 'Unknown error'}`, 'error');
        }
      };

      ws.onclose = () => {
        addMessage('Up socket closed');
        setConnectionStatus('disconnected');
        setUpSocket(null);
      };

      ws.onerror = (error) => {
        addMessage(`Up socket error: ${error}`, 'error');
      };

    } catch (error) {
      addMessage(`Failed to connect up socket: ${error instanceof Error ? error.message : 'Unknown error'}`, 'error');
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

          // Only log if it's a message we want to display
          if (shouldDisplayMessage(message)) {
            addMessage(`DOWN: ${JSON.stringify(message)}`);
          }

          if (message.type === 'down_socket_confirm') {
            addMessage('Down socket confirmed');
          } else if (message.type === 'update_batch' && message.updates) {
            const updateCount = Object.keys(message.updates).length;

            // Only log if it contains updates
            if (updateCount > 0) {
              // Extract entities from the update batch
              const receivedEntities = extractEntitiesFromMessage(message);

              // Log clan updates specifically for debugging
              const clanUpdates = receivedEntities.filter(entity => entity.type === 'Clan');
              if (clanUpdates.length > 0) {
                addMessage(`Received batch update with ${clanUpdates.length} clan updates`, 'console');
                clanUpdates.forEach(clan => {
                  const x = clan.state?.location?.x;
                  const y = clan.state?.location?.y;
                  if (x !== undefined && y !== undefined) {
                    addMessage(`Clan ${clan.state?.name} at position (${x}, ${y})`, 'console');
                  }
                });
              } else {
                addMessage(`Received batch update with ${updateCount} entities`);
              }

              // Update all entities
              updateEntities(receivedEntities);
            }
          } else if (message.console) {
            // Display console messages prominently
            addMessage(message.console, 'console');
          }
        } catch (e) {
          addMessage(`Error parsing down socket message: ${e instanceof Error ? e.message : 'Unknown error'}`, 'error');
        }
      };

      ws.onclose = () => {
        addMessage('Down socket closed');
        setConnectionStatus('connected-up');
        setDownSocket(null);
      };

      ws.onerror = (error) => {
        addMessage(`Down socket error: ${error}`, 'error');
      };

    } catch (error) {
      addMessage(`Failed to connect down socket: ${error instanceof Error ? error.message : 'Unknown error'}`, 'error');
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

  // Filter messages for display
  const filteredMessages = messages.filter(msg => {
    // Always show console messages
    if (msg.type === 'console') return true;

    // For info and error messages, filter out heartbeat and entity sync
    return !msg.message.includes('heartbeat') && !msg.message.includes('entity sync');
  });

  return (
    <div className="h-screen w-screen bg-gray-900 flex flex-col">
      {/* Debug Messages - fixed height */}
      <div className="p-2 h-24 flex-shrink-0 overflow-hidden">
        <div className="text-green-400 font-mono text-xs mb-1">
          Status: {connectionStatus}
        </div>
        <div className="h-16 overflow-hidden">
          {filteredMessages.slice(-5).map((msg, index) => (
            <div
              key={index}
              className={`font-mono text-xs leading-tight ${
                msg.type === 'console'
                  ? 'text-yellow-400'
                  : msg.type === 'error'
                    ? 'text-red-400'
                    : 'text-green-400 opacity-60'
              }`}
            >
              <span className="text-gray-500">{msg.timestamp}</span> {msg.message}
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>
      </div>

      {/* Game Map */}
      <div className="flex-1">
        {connectionStatus === 'fully-connected' && (
          <GameMap entities={entities} />
        )}
      </div>
    </div>
  );
};

export default GameArea;