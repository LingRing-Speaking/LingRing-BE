-- Atomic accept: set user's flag, and if both accepted, promote to result keys.
-- KEYS[1] = matching:confirm:user:{userId}
-- ARGV[1] = userId (string)
-- ARGV[2] = now epoch millis (string)
-- ARGV[3] = result key prefix (e.g. "matching:result:")
-- ARGV[4] = result TTL seconds (string)
-- ARGV[5] = result value delimiter (":")
--
-- Returns a Lua table:
--   {-1}            = no confirm record for this user
--   {-2}            = expired (caller should expire & re-enqueue via Java)
--   {0}             = accepted, waiting for partner
--   {1, partnerId}  = both accepted, result keys promoted (caller may persist Call)

local pairKey = redis.call('GET', KEYS[1])
if not pairKey then
    return {-1}
end

local confirmKey = 'matching:confirm:' .. pairKey
local userAId = redis.call('HGET', confirmKey, 'userAId')
local userBId = redis.call('HGET', confirmKey, 'userBId')
if not userAId or not userBId then
    return {-1}
end

local deadlineStr = redis.call('HGET', confirmKey, 'deadline')
if not deadlineStr or tonumber(deadlineStr) <= tonumber(ARGV[2]) then
    return {-2}
end

local flagField
local partnerId
if userAId == ARGV[1] then
    flagField = 'userAAccepted'
    partnerId = userBId
elseif userBId == ARGV[1] then
    flagField = 'userBAccepted'
    partnerId = userAId
else
    return {-1}
end

redis.call('HSET', confirmKey, flagField, '1')

local aOk = redis.call('HGET', confirmKey, 'userAAccepted')
local bOk = redis.call('HGET', confirmKey, 'userBAccepted')

if aOk == '1' and bOk == '1' then
    local roomId = redis.call('HGET', confirmKey, 'roomId')
    redis.call('SET', ARGV[3] .. userAId, userBId .. ARGV[5] .. roomId, 'EX', ARGV[4])
    redis.call('SET', ARGV[3] .. userBId, userAId .. ARGV[5] .. roomId, 'EX', ARGV[4])
    redis.call('DEL', confirmKey)
    redis.call('DEL', 'matching:confirm:user:' .. userAId)
    redis.call('DEL', 'matching:confirm:user:' .. userBId)
    return {1, partnerId}
end

return {0}
