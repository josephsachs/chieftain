import { GameEntity, filterEntitiesByType } from './GameEntity';

/**
 * Represents a hexagonal zone on the map
 */
export interface MapZone extends GameEntity {
  type: 'MapZone';
  state?: {
    location?: {
      x?: number;
      y?: number;
    };
    terrainType?: TerrainType;
    [key: string]: any;
  };
}

/**
 * Enum for terrain types (matches server-side enum)
 */
export enum TerrainType {
  UNASSIGNED = 'UNASSIGNED',
  GRASSLAND = 'GRASSLAND',
  MEADOW = 'MEADOW',
  SCRUB = 'SCRUB',
  WOODLAND = 'WOODLAND',
  ROCKLAND = 'ROCKLAND',
  DRYLAND = 'DRYLAND',
  MARSH = 'MARSH',
  OCEAN = 'OCEAN'
}

/**
 * Type guard to check if an entity is a MapZone
 */
export function isMapZone(entity: GameEntity): entity is MapZone {
  return entity.type === 'MapZone';
}

/**
 * Filter an array of game entities to get only MapZones
 */
export function getMapZones(entities: GameEntity[]): MapZone[] {
  return filterEntitiesByType<MapZone>(entities, 'MapZone');
}

/**
 * Get the coordinates of a map zone
 * Returns [x, y] or null if no valid location
 */
export function getMapZoneCoordinates(mapZone: MapZone): [number, number] | null {
  const x = mapZone.state?.location?.x;
  const y = mapZone.state?.location?.y;

  if (x !== undefined && y !== undefined) {
    return [x, y];
  }

  return null;
}

/**
 * Get the terrain color for a terrain type
 */
export function getTerrainColor(terrainType: TerrainType | string | undefined): string {
  const TERRAIN_COLORS: Record<string, string> = {
    [TerrainType.GRASSLAND]: '#4CAF50',
    [TerrainType.MEADOW]: '#8BC34A',
    [TerrainType.SCRUB]: '#CDDC39',
    [TerrainType.WOODLAND]: '#2E7D32',
    [TerrainType.ROCKLAND]: '#795548',
    [TerrainType.DRYLAND]: '#FFC107',
    [TerrainType.MARSH]: "#105249",
    [TerrainType.OCEAN]: '#2196F3',
    [TerrainType.UNASSIGNED]: '#9E9E9E'
  };

  return TERRAIN_COLORS[terrainType || ''] || '#9E9E9E';
}