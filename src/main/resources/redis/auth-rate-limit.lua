-- A janela começa na primeira tentativa. Rejeições não renovam o TTL.
local key = KEYS[1]
local maximum = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local count = tonumber(redis.call('GET', key) or '0')
if count >= maximum then
    local ttl = redis.call('PTTL', key)
    if ttl < 0 then
        redis.call('PEXPIRE', key, window)
        ttl = window
    end
    return math.max(ttl, 1)
end
redis.call('INCR', key)
if count == 0 or redis.call('PTTL', key) < 0 then
    redis.call('PEXPIRE', key, window)
end
return 0
