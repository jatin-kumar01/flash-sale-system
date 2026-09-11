-- reserve_stock.lua
-- Atomic stock reservation script
-- KEYS[1]: Redis key for available stock (e.g., "inventory:stock:{productId}")
-- KEYS[2]: Redis key for locked stock (e.g., "inventory:locked:{productId}")
-- ARGV[1]: Quantity to reserve
--
-- Returns:
--   1: Reservation successful
--   0: Insufficient available stock
--  -1: Key not initialized / missing

local stockKey = KEYS[1]
local lockedKey = KEYS[2]
local quantity = tonumber(ARGV[1])

local currentStock = redis.call('GET', stockKey)
if not currentStock then
    return -1
end

if tonumber(currentStock) < quantity then
    return 0
end

redis.call('DECRBY', stockKey, quantity)
redis.call('INCRBY', lockedKey, quantity)
return 1
