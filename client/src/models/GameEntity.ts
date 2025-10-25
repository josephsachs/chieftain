/**
 * Base interface for all game entities
 */
export interface GameEntity {
  _id: string;
  type: string;
  version?: number;
  state?: Record<string, any>;
  // Delta update properties
  operation?: 'update' | 'create' | 'delete';
  delta?: Record<string, any>;
  changedAt?: number;
  [key: string]: any;
}

/**
 * Type guard to check if an object is a GameEntity
 */
export function isGameEntity(obj: any): obj is GameEntity {
  return (
    obj &&
    typeof obj === 'object' &&
    typeof obj._id === 'string' &&
    typeof obj.type === 'string'
  );
}

/**
 * Helper to extract entities from WebSocket messages
 */
export function extractEntitiesFromMessage(message: any): GameEntity[] {
  if (!message) return [];

  // Extract from sync message
  if (message.type === 'sync' && message.data?.entities) {
    return message.data.entities.filter(isGameEntity);
  }

  // Extract from update batch
  if (message.type === 'update_batch' && message.updates) {
    // Convert object of updates to array and ensure they have the operation field
    return Object.values(message.updates)
      .filter(isGameEntity)
      .map((entity: any) => {
        // Make sure delta updates have the operation field
        if (entity.delta && !entity.operation) {
          return { ...entity, operation: 'update' };
        }
        return entity;
      });
  }

  return [];
}

/**
 * Filter entities by type
 */
export function filterEntitiesByType<T extends GameEntity>(
  entities: GameEntity[],
  entityType: string
): T[] {
  return entities.filter(entity => entity.type === entityType) as T[];
}