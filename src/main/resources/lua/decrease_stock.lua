local stockKey = KEYS[1]
local quantity = tonumber(ARGV[1])

if quantity == nil or quantity <= 0 then
    return -3
end

local stockValue = redis.call('GET', stockKey)

if stockValue == false then
    return -1
end

local stock = tonumber(stockValue)

if stock == nil then
    return -4
end

if stock < quantity then
    return -2
end

redis.call('DECRBY', stockKey, quantity)

return 1