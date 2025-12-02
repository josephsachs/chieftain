import { GameEntity, filterEntitiesByType } from './GameEntity';

/**
 * Represents a clan entity in the game
 */
export interface Clan extends GameEntity {
  type: 'Clan';
  state?: {
    name?: string;
    population?: number;
    culture?: CultureGroup;
    location?: {
      x?: number;
      y?: number;
    };
    [key: string]: any;
  };
  properties?: {
    behavior?: ClanBehavior;
    [key: string]: any;
  };
}

/**
 * Enum for clan behaviors (matches server-side enum)
 */
export enum ClanBehavior {
  NONE = 'NONE',
  WANDERING = 'WANDERING'
}

/**
 * Enum for culture groups (matches server-side enum)
 */
export enum CultureGroup {
  UNASSIGNED = 'UNASSIGNED',
  EGYPTIAN = 'EGYPTIAN',
  CANAANITE = 'CANAANITE',
  SHASU = 'SHASU',
  HURRIAN = 'HURRIAN',
  AMORITE = 'AMORITE',
  HABIRU = 'HABIRU'
}

/**
 * Type guard to check if an entity is a Clan
 */
export function isClan(entity: GameEntity): entity is Clan {
  return entity.type === 'Clan';
}

/**
 * Filter an array of game entities to get only Clans
 */
export function getClans(entities: GameEntity[]): Clan[] {
  return filterEntitiesByType<Clan>(entities, 'Clan');
}

/**
 * Get the coordinates of a clan
 * Returns [x, y] or null if no valid location
 */
export function getClanCoordinates(clan: Clan): [number, number] | null {
  // First check if location is a Vector2 object
  if (clan.state?.location) {
    // Direct x,y properties on location (Vector2 format)
    const x = clan.state.location.x;
    const y = clan.state.location.y;

    if (x !== undefined && y !== undefined) {
      return [x, y];
    }
  }

  return null;
}

/**
 * Get a color for a clan based on its culture
 */
export function getClanColor(clan: Clan): string {
  const CULTURE_COLORS: Record<string, string> = {
    [CultureGroup.CANAANITE]: '#7986CB', // Indigo
    [CultureGroup.EGYPTIAN]: '#FFD700', // Gold
    [CultureGroup.SHASU]: '#FFB74D', // Orange
    [CultureGroup.HURRIAN]: '#66BB6A', // Green
    [CultureGroup.AMORITE]: '#4FC3F7', // Light Blue
    [CultureGroup.HABIRU]: '#FFD54F', // Yellow
    [CultureGroup.UNASSIGNED]: '#E0E0E0' // Grey
  };

  return CULTURE_COLORS[clan.state?.culture || ''] || '#E0E0E0';
}