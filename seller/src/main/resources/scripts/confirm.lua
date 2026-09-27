-- confirm.lua
-- KEYS[1] = 'tickets:reserved'
-- ARGV[1] = reservation_id
-- ARGV[2] = current_timestamp

-- Find all reserved tickets
local reserved_items = redis.call('ZRANGE', KEYS[1], 0, -1)

for i, item in ipairs(reserved_items) do
    -- Parse the payload: reservation_id:ticket_number:user_id
    local parts = {}
    for part in string.gmatch(item, "([^:]+)") do
        table.insert(parts, part)
    end
    
    if parts[1] == ARGV[1] then
        -- Found the reservation. Check if it's expired.
        local score = redis.call('ZSCORE', KEYS[1], item)
        if tonumber(score) >= tonumber(ARGV[2]) then
            -- Valid! Remove from reserved.
            redis.call('ZREM', KEYS[1], item)
            return "CONFIRMED:" .. parts[2] .. ":" .. parts[3] -- ticket_number:user_id
        else
            return "EXPIRED"
        end
    end
end

return "NOT_FOUND"
