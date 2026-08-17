-- Randomizes the X-Session-Id header (0-49) on every request, so concurrent
-- wrk connections hit many different test sessions instead of just one.
-- Without this, the global-lock and per-session-lock variants would look
-- identical, since a single shared session serializes either way.
--
-- Usage:
--   wrk -t4 -c<N> -d10s --latency -s bench/session_lock.lua \
--       http://localhost:18080/bench/session/global
--   wrk -t4 -c<N> -d10s --latency -s bench/session_lock.lua \
--       http://localhost:18080/bench/session/per-session

request = function()
    wrk.headers["X-Session-Id"] = tostring(math.random(0, 49))
    return wrk.format(nil, nil, wrk.headers)
end
