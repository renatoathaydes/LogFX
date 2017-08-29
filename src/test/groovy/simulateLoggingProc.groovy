/**
 * This script simulates a process that generates predictable logging output.
 */
if ( args.size() != 1 ) {
    println 'Wrong number of arguments (expected 1, got ' + args.size() +
            ').\nUsage: simulateLoggingProc <fileName>'
    System.exit( 1 )
}

file = new File( args[ 0 ] )

if ( file.exists() ) {
    file.delete()
}

def write = { String msg ->
    file << 'INFO ' + new Date() + ' - ' + msg + '\n'
}

write 'Sleeping 5 seconds...'
sleep 5_000

write 'Logging 10x padLeft(n, n.toString)'

10.times { it -> write( it.toString().padLeft( it, it.toString() ) ) }

200.times {
    write UUID.randomUUID().toString()
    sleep 100
}

write 'Sleeping 5 seconds'
sleep 5_000

write 'Logging numbers 50 to 1, one per second'

( 50..1 ).each {
    write it.toString()
    sleep 1_000
}

write 'Good bye!'
