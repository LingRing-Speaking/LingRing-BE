-- Atomic creation of a call invitation.
-- KEYS[1] = matching:invitation:out:{inviterId}
-- KEYS[2] = matching:invitation:in:{inviteeId}
-- KEYS[3] = matching:invitation:result:{inviterId}
-- ARGV[1] = invitee id (string, out-key value)
-- ARGV[2] = invitation value "{inviterId}:{roomId}:{deadlineMillis}"
-- ARGV[3] = TTL millis (string)
-- Returns 1 created, -1 inviter already has an outgoing invitation, -2 invitee slot occupied.

if redis.call('EXISTS', KEYS[1]) == 1 then
    return -1
end
if redis.call('EXISTS', KEYS[2]) == 1 then
    return -2
end

redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[3])
redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[3])
redis.call('DEL', KEYS[3])
return 1
