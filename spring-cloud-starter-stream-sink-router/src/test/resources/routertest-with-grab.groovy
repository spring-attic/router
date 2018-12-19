@Grab(group='org.codehaus.groovy', module='groovy', version='2.5.2')
import org.codehaus.groovy.tools.Utilities
if (Utilities.repeatString(headers.route,2).equals('foofoo')) {
    return "$foo" // mapped to baz in 'variables'
}
else {
    return "$bar" // mapped to qux in properties file
}
