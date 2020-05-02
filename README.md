
Property resolution order:
- command line arguments
- system properties
- environment variables
- properties file
    - if the propertiesFile property is present the mentioned file will be used (and profile is ignored for properties file resolution). Fails if file not found in classpath.
    - if propertiesFile is not present and profile is not present application.properties file will be used if present (does not fail if file not found in classpath)
    - if profile is present
        - and has a value that when lowercased equals "prod" or is empty string then application.properties file will be used if present (does not fail if file not found in classpath) otherwise
        - the application-${lowercase.profile}.properties file will be used if present (does not fail if file not found in classpath) otherwise


How the properties must be provided for each source:

1. command line arguments  => keyDelimiter="." , valueDelimiter="," 

-db.port=3306

-aliases=alias1,alias2,alias3

2. system properties => keyDelimiter="_" , valueDelimiter="," 

db_port=3306

aliases=alias1,alias2,alias3

3. environment properties => keyDelimiter="_" , valueDelimiter="," 

db_port=3306

aliases=alias1,alias2,alias3

4. properties file => keyDelimiter="." , valueDelimiter="," 

db.port=3306

aliases=alias1,alias2,alias3


Example usage:

(You can see the full example in ZioPropertiesExample.scala)

```scala
def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    //create the descriptor for your AppProperties
    val desc = descriptor[AppProperties]

    val p = for {
      //provide the arguments from main method and the descriptor to create the Layer
      layer <- ZioProperties.createPropertiesLayer(args, desc)
      props <- myProgram.provideCustomLayer(layer)
    } yield props

    ...
  }


/**
   * Effects that require AppProperties can retrieve it from the Layer
   */
  val myProgram: ZIO[Config[AppProperties] with Console, Nothing, AppProperties] =
    for {
      myProps <- config[AppProperties]
      _       <- putStrLn(myProps.toString())
    } yield myProps
      
```

