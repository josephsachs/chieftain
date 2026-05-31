import { City } from '../models/City';
import { GameEntity } from '../models/GameEntity';
import { Character, isCharacter } from '../models/Character';

interface CityPanelProps {
  city: City;
  entities: GameEntity[];
  onClose: () => void;
  onOpenCharacter: (character: Character) => void;
}

const CULTURE_COLORS: Record<string, string> = {
  SUMERIAN: '#c2a54f',
  AKKADIAN: '#a0522d',
  GUTIAN: '#708090',
  ELAMITE: '#8b4513',
  AMORITE: '#d2691e',
  HURRIAN: '#6b8e23',
  UNASSIGNED: '#888888',
};

const CityPanel: React.FC<CityPanelProps> = ({ city, entities, onClose, onOpenCharacter }) => {
  const s = city.state;
  const color = CULTURE_COLORS[s?.culture || 'UNASSIGNED'] || CULTURE_COLORS.UNASSIGNED;

  // Find the prince Character entity by princeId
  const princeId = s?.princeId as string | undefined;
  const princeEntity = princeId
    ? entities.find(e => e._id === princeId && isCharacter(e)) as Character | undefined
    : undefined;
  const princeName = princeEntity?.state?.name || princeId || 'None';

  // Navigate City -> alignedWith (Polity)
  const alignedWith = s?.alignedWith;
  const polityId = typeof alignedWith === 'string' ? alignedWith : alignedWith?._id;
  const polityEntity = polityId
    ? entities.find(e => e._id === polityId)
    : undefined;
  const polityName = polityEntity?.state?.name || alignedWith?.name || polityId;

  // Market depot
  const marketContents = s?.market?.contents as Record<string, Record<string, number>> | undefined;

  // Exchange rates
  const exchangeRates = s?.exchangeRates;
  const buyRates = exchangeRates?.buyRates as Record<string, number> | undefined;
  const sellRates = exchangeRates?.sellRates as Record<string, number> | undefined;

  return (
    <div className="fixed inset-0 z-50 flex">
      {/* Panel */}
      <div className="w-96 h-full bg-gray-900 border-r border-gray-700 shadow-2xl overflow-y-auto">
        {/* Header */}
        <div className="p-4 border-b border-gray-700">
          <div className="flex justify-between items-start">
            <div className="flex items-center gap-3">
              <div
                className="w-8 h-8 rounded border-2 border-gray-600 flex items-center justify-center text-xs font-bold"
                style={{ backgroundColor: color }}
              >
                &#x1F3DB;
              </div>
              <div>
                <h2 className="text-xl font-bold text-white">{s?.name || 'Unknown City'}</h2>
                <div className="text-sm text-gray-400">
                  {s?.culture || 'Unknown'} &middot; Pop. {s?.population || 0}
                </div>
              </div>
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-white text-lg px-2"
            >
              &times;
            </button>
          </div>
        </div>

        {/* Location */}
        <div className="p-4 border-b border-gray-700">
          <div className="flex justify-between items-center">
            <span className="text-sm text-gray-400">Location</span>
            <span className="text-sm text-white font-mono">
              ({s?.location?.x}, {s?.location?.y})
            </span>
          </div>
          {polityName && (
            <div className="flex justify-between items-center mt-1">
              <span className="text-sm text-gray-400">Polity</span>
              <span className="text-sm text-white">{polityName}</span>
            </div>
          )}
        </div>

        {/* Prince */}
        {princeId && (
          <div className="p-4 border-b border-gray-700">
            <h3 className="text-sm font-semibold text-gray-400 uppercase mb-2">Prince</h3>
            {princeEntity ? (
              <button
                onClick={() => onOpenCharacter(princeEntity)}
                className="text-blue-400 hover:text-blue-300 text-sm underline cursor-pointer"
              >
                {princeName}
              </button>
            ) : (
              <span className="text-sm text-gray-300">{princeName}</span>
            )}
          </div>
        )}

        {/* Market */}
        {marketContents && Object.keys(marketContents).length > 0 && (
          <div className="p-4 border-b border-gray-700">
            <h3 className="text-sm font-semibold text-gray-400 uppercase mb-2">Market</h3>
            <div className="text-sm">
              {Object.entries(marketContents).map(([group, items]) => {
                if (typeof items !== 'object' || items === null) return null;
                const nonZero = Object.entries(items as Record<string, number>)
                  .filter(([, v]) => v > 0);
                if (nonZero.length === 0) return null;

                return (
                  <div key={group} className="mb-2">
                    <div className="text-xs text-gray-500 uppercase mb-1">{group}</div>
                    {nonZero.map(([resource, qty]) => (
                      <div key={resource} className="flex justify-between py-0.5 pl-2">
                        <span className="text-gray-300 capitalize">
                          {resource.toLowerCase()}
                        </span>
                        <span className="text-white font-mono">{qty}</span>
                      </div>
                    ))}
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Exchange Rates */}
        {(buyRates || sellRates) && (
          <div className="p-4">
            <h3 className="text-sm font-semibold text-gray-400 uppercase mb-2">Exchange Rates</h3>
            <div className="text-sm">
              {buyRates && Object.keys(buyRates).length > 0 && (
                <div className="mb-2">
                  <div className="text-xs text-gray-500 uppercase mb-1">Buys</div>
                  {Object.entries(buyRates).map(([resource, rate]) => (
                    <div key={resource} className="flex justify-between py-0.5 pl-2">
                      <span className="text-gray-300 capitalize">{resource.toLowerCase()}</span>
                      <span className="text-white font-mono">{rate}x</span>
                    </div>
                  ))}
                </div>
              )}
              {sellRates && Object.keys(sellRates).length > 0 && (
                <div className="mb-2">
                  <div className="text-xs text-gray-500 uppercase mb-1">Sells</div>
                  {Object.entries(sellRates).map(([resource, rate]) => (
                    <div key={resource} className="flex justify-between py-0.5 pl-2">
                      <span className="text-gray-300 capitalize">{resource.toLowerCase()}</span>
                      <span className="text-white font-mono">{rate}x</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Backdrop */}
      <div className="flex-1 bg-black bg-opacity-50" onClick={onClose} />
    </div>
  );
};

export default CityPanel;
