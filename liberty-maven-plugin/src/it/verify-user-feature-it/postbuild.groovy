import java.io.File;

File buildLog = new File( basedir, 'build.log' )
assert buildLog.text.contains( 'All features were successfully verified.')
/*CWWKF1514E: The 0X05534365803788CE public key ID does not match the 0xWRONGKEYID provided key ID.*/
assert buildLog.text.contains( 'CWWKF1514E')
/*CWWKF1508E: The public key ID for the src/test/resources/SimpleActivatorValidKey.asc key URL was not provided.*/
assert buildLog.text.contains( 'CWWKF1508E')
/*CWWKF1512E: Unable to verify the following feature signatures:*/
assert buildLog.text.contains( 'CWWKF1512E')
