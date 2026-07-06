local stockKey = KEYS[1]
local quantity = tonumber(ARGV[1])

local stock = tonumber(redis.call('GET', stockKey))

if stock = nil then
    return -1
end

if stock < quantity then
    return -2
end

redis.call('DECRBY', stockKey, quantity)

return 1