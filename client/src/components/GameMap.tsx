import { useRef, useEffect, useState, useCallback } from 'react';
import MapRenderer from './MapRenderer';
import { GameEntity } from '../models/GameEntity';
import { MapZone, getMapZoneCoordinates } from '../models/MapZone';
import { Clan, getClanCoordinates } from '../models/Clan';

interface GameMapProps {
  entities: GameEntity[];
}

const GameMap: React.FC<GameMapProps> = ({ entities }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const rendererRef = useRef<MapRenderer | null>(null);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [lastMousePos, setLastMousePos] = useState({ x: 0, y: 0 });
  const [selectedEntity, setSelectedEntity] = useState<{
    x: number;
    y: number;
    entities: GameEntity[];
  } | null>(null);
  const [showInfoPanel, setShowInfoPanel] = useState(false);
  const [infoPanelPos, setInfoPanelPos] = useState({ x: 0, y: 0 });

  // Hex grid constants
  const HEX_RADIUS = 48;
  const BORDER_PADDING = 50;

  // Initialize renderer when canvas is ready
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    try {
      rendererRef.current = new MapRenderer(canvas);
    } catch (error) {
      console.error('Failed to initialize MapRenderer:', error);
    }
  }, []);

  // Convert pixel coordinates back to hex grid coordinates
  const pixelToGrid = (pixelX: number, pixelY: number): [number, number] => {
    // Remove border padding and pan offset
    const adjustedX = pixelX - BORDER_PADDING - pan.x;
    const adjustedY = pixelY - BORDER_PADDING - pan.y;

    const hexWidth = HEX_RADIUS * 2;
    const hexHeight = HEX_RADIUS * Math.sqrt(3);

    // Rough grid coordinate estimate
    const gridX = Math.round(adjustedX / (hexWidth * 0.75));
    const gridY = Math.round(adjustedY / hexHeight);

    // Check nearby hexes to find the closest one
    const candidates = [];
    for (let dx = -1; dx <= 1; dx++) {
      for (let dy = -1; dy <= 1; dy++) {
        const testX = gridX + dx;
        const testY = gridY + dy;
        if (testX >= 0 && testY >= 0) {
          const [hexPixelX, hexPixelY] = gridToPixel(testX, testY);
          const distance = Math.sqrt(
            Math.pow(pixelX - hexPixelX - pan.x, 2) +
            Math.pow(pixelY - hexPixelY - pan.y, 2)
          );
          candidates.push({ x: testX, y: testY, distance });
        }
      }
    }

    // Return the closest valid hex
    candidates.sort((a, b) => a.distance - b.distance);
    return candidates.length > 0 ?
      [candidates[0].x, candidates[0].y] : [gridX, gridY];
  };

  const gridToPixel = (gridX: number, gridY: number): [number, number] => {
    const hexWidth = HEX_RADIUS * 2;
    const hexHeight = HEX_RADIUS * Math.sqrt(3);

    let pixelX = gridX * hexWidth * 0.75;
    let pixelY = gridY * hexHeight;

    // Offset odd columns (vertical offset)
    if (gridX % 2 === 1) {
      pixelY += hexHeight * 0.5;
    }

    // Add border padding
    pixelX += BORDER_PADDING;
    pixelY += BORDER_PADDING;

    return [pixelX, pixelY];
  };

  // Find entities at a specific grid position
  const getEntitiesAtPosition = (x: number, y: number): GameEntity[] => {
    const result: GameEntity[] = [];

    // Check all entities with a location
    entities.forEach(entity => {
      if (entity.type === 'MapZone') {
        const coords = getMapZoneCoordinates(entity as MapZone);
        if (coords && coords[0] === x && coords[1] === y) {
          result.push(entity);
        }
      } else if (entity.type === 'Clan') {
        const coords = getClanCoordinates(entity as Clan);
        if (coords && coords[0] === x && coords[1] === y) {
          result.push(entity);
        }
      }
    });

    return result;
  };

  // Render the map
  const renderMap = useCallback(() => {
    const renderer = rendererRef.current;
    if (!renderer) return;

    renderer.render(entities, pan, {
      showSelection: true,
      selectedHex: selectedEntity
    });
  }, [entities, pan, selectedEntity]);

  // Handle canvas resize
  const resizeCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * window.devicePixelRatio;
    canvas.height = rect.height * window.devicePixelRatio;

    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
    }

    renderMap();
  }, [renderMap]);

  // Mouse event handlers for panning and clicking
  const handleMouseDown = (e: React.MouseEvent) => {
    setIsDragging(true);
    setLastMousePos({ x: e.clientX, y: e.clientY });
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging) return;

    const deltaX = e.clientX - lastMousePos.x;
    const deltaY = e.clientY - lastMousePos.y;

    setPan(prev => ({
      x: prev.x + deltaX,
      y: prev.y + deltaY
    }));

    setLastMousePos({ x: e.clientX, y: e.clientY });
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const handleClick = (e: React.MouseEvent) => {
    if (isDragging) return; // Don't process clicks if we were dragging

    const canvas = canvasRef.current;
    if (!canvas) return;

    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const [gridX, gridY] = pixelToGrid(x, y);

    // Find entities at this grid position
    const entitiesAtPosition = getEntitiesAtPosition(gridX, gridY);

    if (entitiesAtPosition.length > 0) {
      // Clicked on a valid hex with entities
      setSelectedEntity({
        x: gridX,
        y: gridY,
        entities: entitiesAtPosition
      });
      setInfoPanelPos({ x: e.clientX, y: e.clientY });
      setShowInfoPanel(true);
    } else {
      // Clicked outside valid hex area
      setShowInfoPanel(false);
      setSelectedEntity(null);
    }
  };

  const handleRightClick = (e: React.MouseEvent) => {
    e.preventDefault(); // Prevent context menu
  };

  // Keyboard panning
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const panSpeed = 20;

      switch (e.key) {
        case 'ArrowUp':
          setPan(prev => ({ ...prev, y: prev.y + panSpeed }));
          break;
        case 'ArrowDown':
          setPan(prev => ({ ...prev, y: prev.y - panSpeed }));
          break;
        case 'ArrowLeft':
          setPan(prev => ({ ...prev, x: prev.x + panSpeed }));
          break;
        case 'ArrowRight':
          setPan(prev => ({ ...prev, x: prev.x - panSpeed }));
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Setup canvas and resize handling
  useEffect(() => {
    resizeCanvas();
    window.addEventListener('resize', resizeCanvas);
    return () => window.removeEventListener('resize', resizeCanvas);
  }, [resizeCanvas]);

  // Re-render when map data or pan changes
  useEffect(() => {
    renderMap();
  }, [renderMap]);

  // Get counts for debug display
  const mapZoneCount = entities.filter(e => e.type === 'MapZone').length;
  const clanCount = entities.filter(e => e.type === 'Clan').length;

  return (
    <div className="w-full h-full relative">
      <canvas
        ref={canvasRef}
        className="w-full h-full cursor-grab active:cursor-grabbing"
        style={{ cursor: isDragging ? 'grabbing' : 'grab' }}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        onClick={handleClick}
        onContextMenu={handleRightClick}
      />

      {/* Info Panel */}
      {showInfoPanel && selectedEntity && (
        <div
          className="absolute bg-gray-800 text-white p-3 rounded shadow-lg border border-gray-600 z-50"
          style={{
            left: infoPanelPos.x + 10,
            top: infoPanelPos.y + 10,
            maxWidth: '250px'
          }}
        >
          <div className="text-sm font-semibold mb-2">Hex Information</div>
          <div className="text-xs space-y-1">
            <div>Coordinates: ({selectedEntity.x}, {selectedEntity.y})</div>

            {/* Map Zone Info */}
            {selectedEntity.entities.filter(e => e.type === 'MapZone').map((zone, idx) => (
              <div key={`zone-${idx}`} className="mt-2">
                <div className="font-semibold">Terrain</div>
                <div>Type: {(zone as MapZone).state?.terrainType || 'Unknown'}</div>
                <div>ID: {zone._id}</div>
              </div>
            ))}

            {/* Clan Info */}
            {selectedEntity.entities.filter(e => e.type === 'Clan').map((clan, idx) => (
              <div key={`clan-${idx}`} className="mt-2 pt-2 border-t border-gray-600">
                <div className="font-semibold">Clan: {(clan as Clan).state?.name}</div>
                <div>Population: {(clan as Clan).state?.population || 0}</div>
                <div>Culture: {(clan as Clan).state?.culture || 'Unknown'}</div>
                <div>Behavior: {(clan as Clan).properties?.behavior || 'Unknown'}</div>
              </div>
            ))}
          </div>
          <div className="text-xs text-gray-400 mt-2">Left-click to dismiss</div>
        </div>
      )}

      {/* Debug info */}
      <div className="absolute top-2 right-2 bg-black bg-opacity-50 text-white text-xs p-2 rounded">
        <div>MapZones: {mapZoneCount}</div>
        <div>Clans: {clanCount}</div>
        <div>Pan: {pan.x.toFixed(0)}, {pan.y.toFixed(0)}</div>
        {selectedEntity && (
          <div className="text-yellow-300">Selected: ({selectedEntity.x}, {selectedEntity.y})</div>
        )}
        <div className="text-gray-300 mt-1">Arrow keys or drag to pan</div>
        <div className="text-gray-300">Click hex for info</div>
      </div>
    </div>
  );
};

export default GameMap;