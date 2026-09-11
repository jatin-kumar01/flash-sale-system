-- release_stock.lua
-- Atomic stock release / rollback script
-- KEYS[1]: Redis key for available stock (e.g., "inventory:stock:{productId}")
-- KEYS[2]: Redis key for locked stock (e.g., "inventory:locked:{productId}")
-- ARGV[1]: Quantity to release back to available
--
-- Returns:
--   1: Release successful
--   0: Locked stock is less than release quantity
--  -1: Keys do not exist

local stockKey = KEYS[1]
local lockedKey = KEYS[2]
local quantity = tonumber(ARGV[1])

local currentLocked = redis.call('GET', lockedKey)
if not currentLocked then
    return -1
end

if tonumber(currentLocked) < quantity then
    return 0
end

redis.call('DECRBY', lockedKey, quantity)
redis.call('INCRBY', stockKey, quantity)
return 1
