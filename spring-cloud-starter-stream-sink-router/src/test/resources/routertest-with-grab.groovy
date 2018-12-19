@Grab('com.johnnywey:flipside:0.1.26-SNAPSHOT')
import static com.johnnywey.flipside.Matcher.match

def channel = "no-channel"

// Should match the first Closure even though both would apply.
match headers.route on {
    matches "foo", { channel = "$foo" }
    matches "bar", { channel = "$bar" }
}

return channel