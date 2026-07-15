-- Cancel an outgoing call invitation (idempotent).
-- KEYS[1] = matching:invitation:out:{inviterId}
-- ARGV[1] = in-key prefix ("matching:invitation:in:")
-- ARGV[2] = inviter id (string)
-- Deletes the invitee-side key only when it still belongs to this inviter,
-- so a third party's invitation that arrived after expiry is preserved.
-- Returns 1 if an outgoing invitation existed, 0 otherwise.

local inviteeId = redis.call('GET', KEYS[1])
if not inviteeId then
    return 0
end

local inKey = ARGV[1] .. inviteeId
local value = redis.call('GET', inKey)
if value and string.sub(value, 1, string.len(ARGV[2]) + 1) == ARGV[2] .. ':' then
    redis.call('DEL', inKey)
end
redis.call('DEL', KEYS[1])
return 1
