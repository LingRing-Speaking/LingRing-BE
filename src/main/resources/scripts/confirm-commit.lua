-- Atomic transition: queue -> confirm.
-- KEYS[1] = matching:queue (ZSET)
-- KEYS[2] = matching:confirm:{pairKey}
-- KEYS[3] = matching:confirm:user:{userAId}
-- KEYS[4] = matching:confirm:user:{userBId}
-- ARGV[1] = userAId (string)
-- ARGV[2] = userBId (string)
-- ARGV[3] = roomId
-- ARGV[4] = deadline epoch millis (string)
-- ARGV[5] = pairKey
-- ARGV[6] = TTL seconds (string)
-- ARGV[7] = userA(lo) enqueuedAt epoch millis (string)
-- ARGV[8] = userB(hi) enqueuedAt epoch millis (string)
-- Returns 1 if committed, 0 if either user is no longer in the queue.

if not redis.call('ZSCORE', KEYS[1], ARGV[1]) then
    return 0
end
if not redis.call('ZSCORE', KEYS[1], ARGV[2]) then
    return 0
end

redis.call('ZREM', KEYS[1], ARGV[1], ARGV[2])
redis.call('HSET', KEYS[2],
    'userAId', ARGV[1],
    'userBId', ARGV[2],
    'userAAccepted', '0',
    'userBAccepted', '0',
    'roomId', ARGV[3],
    'deadline', ARGV[4],
    'userAEnqueuedAt', ARGV[7],
    'userBEnqueuedAt', ARGV[8])
redis.call('EXPIRE', KEYS[2], ARGV[6])
redis.call('SET', KEYS[3], ARGV[5], 'EX', ARGV[6])
redis.call('SET', KEYS[4], ARGV[5], 'EX', ARGV[6])
return 1
