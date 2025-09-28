import { useRef, useEffect, useState, useCallback } from 'react';

interface MapZone {
  _id: string;
  type: string;
  state?: {
    location?: {
      first?: number;
      second?: number;
    };
    terrainType?: string;
    [key: string]: any;
  };
  [key: string]: any;
}

interface GameMapProps {
  mapData: MapZone[];
}

const TERRAIN_COLORS: { [key: string]: string } = {
  GRASSLAND: '#4CAF50',
  MEADOW: '#8BC34A',
  SCRUB: '#CDDC39',
  WOODLAND: '#2E7D32',
  ROCKLAND: '#795548',
  DRYLAND: '#FFC107',
  // Add more terrain types as needed
};

const DEFAULT_COLOR = '#9E9E9E';

const GameMap: React.FC<GameMapProps> = ({ mapData }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [lastMousePos, setLastMousePos] = useState({ x: 0, y: 0 });
  const [selectedHex, setSelectedHex] = useState<{ x: number; y: number; zone?: MapZone } | null>(null);
  const [showInfoPanel, setShowInfoPanel] = useState(false);
  const [infoPanelPos, setInfoPanelPos] = useState({ x: 0, y: 0 });

  // Hex grid constants
  const HEX_WIDTH = 96;
  const HEX_HEIGHT = 96;
  const HEX_RADIUS = 48;
  const BORDER_PADDING = 50;

  // Calculate hex vertices for flat-top hexagon
  const getHexVertices = (centerX: number, centerY: number): [number, number][] => {
    const vertices: [number, number][] = [];
    for (let i = 0; i < 6; i++) {
      const angle = (Math.PI / 3) * i;
      const x = centerX + HEX_RADIUS * Math.cos(angle);
      const y = centerY + HEX_RADIUS * Math.sin(angle);
      vertices.push([x, y]);
    }
    return vertices;
  };

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
    return candidates.length > 0 ? [candidates[0].x, candidates[0].y] : [gridX, gridY];
  };

  // Check if a point is inside a hexagon
  const isPointInHex = (pointX: number, pointY: number, hexCenterX: number, hexCenterY: number): boolean => {
    const distance = Math.sqrt(
      Math.pow(pointX - hexCenterX, 2) + 
      Math.pow(pointY - hexCenterY, 2)
    );
    return distance <= HEX_RADIUS;
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

  // Draw a single hexagon
  const drawHex = (ctx: CanvasRenderingContext2D, centerX: number, centerY: number, color: string) => {
    const vertices = getHexVertices(centerX, centerY);
    
    ctx.beginPath();
    ctx.moveTo(vertices[0][0], vertices[0][1]);
    for (let i = 1; i < vertices.length; i++) {
      ctx.lineTo(vertices[i][0], vertices[i][1]);
    }
    ctx.closePath();
    
    // Fill hex
    ctx.fillStyle = color;
    ctx.fill();
    
    // Draw border
    ctx.strokeStyle = '#333';
    ctx.lineWidth = 1;
    ctx.stroke();
  };

  // Render the map
  const renderMap = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    // Apply panning transform
    ctx.save();
    ctx.translate(pan.x, pan.y);
    
    // Calculate grid dimensions from map data
    let maxX = 0;
    let maxY = 0;
    
    if (mapData.length > 0) {
      mapData.forEach(zone => {
        if (zone.state?.location?.first !== undefined && zone.state?.location?.second !== undefined) {
          maxX = Math.max(maxX, zone.state.location.first);
          maxY = Math.max(maxY, zone.state.location.second);
        }
      });
    }
    
    // Create a map of grid positions to MapZones
    const zoneMap = new Map<string, MapZone>();
    mapData.forEach(zone => {
      if (zone.state?.location?.first !== undefined && zone.state?.location?.second !== undefined) {
        const key = `${zone.state.location.first},${zone.state.location.second}`;
        zoneMap.set(key, zone);
      }
    });
    
    // Render all grid positions based on actual data dimensions
    for (let y = 0; y <= maxY; y++) {
      for (let x = 0; x <= maxX; x++) {
        const [pixelX, pixelY] = gridToPixel(x, y);
        const key = `${x},${y}`;
        const zone = zoneMap.get(key);
        
        let color = DEFAULT_COLOR;
        if (zone?.state?.terrainType) {
          color = TERRAIN_COLORS[zone.state.terrainType] || DEFAULT_COLOR;
        }
        
        drawHex(ctx, pixelX, pixelY, color);
        
        // Highlight selected hex
        if (selectedHex && selectedHex.x === x && selectedHex.y === y) {
          ctx.strokeStyle = '#FFD700'; // Gold color for selection
          ctx.lineWidth = 3;
          ctx.stroke();
        }
      }
    }
    
    ctx.restore();
  }, [mapData, pan]);

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
    
    // Find the zone at this grid position
    const zone = mapData.find(z => 
      z.state?.location?.first === gridX && z.state?.location?.second === gridY
    );
    
    if (zone) {
      // Clicked on a valid hex with data
      setSelectedHex({ x: gridX, y: gridY, zone });
      setInfoPanelPos({ x: e.clientX, y: e.clientY });
      setShowInfoPanel(true);
    } else {
      // Clicked outside valid hex area
      setShowInfoPanel(false);
      setSelectedHex(null);
    }
  };

  const handleRightClick = (e: React.MouseEvent) => {
    e.preventDefault(); // Still prevent context menu just in case
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
      {showInfoPanel && selectedHex && (
        <div 
          className="absolute bg-gray-800 text-white p-3 rounded shadow-lg border border-gray-600 z-50"
          style={{ 
            left: infoPanelPos.x + 10, 
            top: infoPanelPos.y + 10,
            maxWidth: '200px'
          }}
        >
          <div className="text-sm font-semibold mb-2">Hex Information</div>
          <div className="text-xs space-y-1">
            <div>Coordinates: ({selectedHex.x}, {selectedHex.y})</div>
            {selectedHex.zone ? (
              <>
                <div>Terrain: {selectedHex.zone.state?.terrainType || 'Unknown'}</div>
                <div>ID: {selectedHex.zone._id}</div>
              </>
            ) : (
              <div className="text-gray-400">No zone data</div>
            )}
          </div>
          <div className="text-xs text-gray-400 mt-2">Left-click to dismiss</div>
        </div>
      )}
      
      {/* Debug info */}
      <div className="absolute top-2 right-2 bg-black bg-opacity-50 text-white text-xs p-2 rounded">
        <div>MapZones: {mapData.length}</div>
        <div>Pan: {pan.x.toFixed(0)}, {pan.y.toFixed(0)}</div>
        {selectedHex && (
          <div className="text-yellow-300">Selected: ({selectedHex.x}, {selectedHex.y})</div>
        )}
        <div className="text-gray-300 mt-1">Arrow keys or drag to pan</div>
        <div className="text-gray-300">Click hex for info</div>
      </div>
    </div>
  );
};

export default GameMap;