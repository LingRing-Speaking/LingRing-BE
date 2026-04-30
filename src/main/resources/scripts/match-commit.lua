-- Atomic conditional commit for matching pair.
-- KEYS[1] = matching:queue (ZSET)
-- KEYS[2] = matching:result:{userId}
-- KEYS[3] = matching:result:{partnerId}
-- ARGV[1] = userId (string)
-- ARGV[2] = partnerId (string)
-- ARGV[3] = serialized result value for userId (e.g. "{partnerId}:{roomId}")
-- ARGV[4] = serialized result value for partnerId (e.g. "{userId}:{roomId}")
-- ARGV[5] = result TTL in seconds
-- Returns 1 if committed, 0 if either user is no longer in the queue.

if not redis.call('ZSCORE', KEYS[1], ARGV[1]) then
    return 0
end
if not redis.call('ZSCORE', KEYS[1], ARGV[2]) then
    return 0
end

redis.call('ZREM', KEYS[1], ARGV[1], ARGV[2])
redis.call('SET', KEYS[2], ARGV[3], 'EX', ARGV[5])
redis.call('SET', KEYS[3], ARGV[4], 'EX', ARGV[5])
return 1