#  Tiny Store Manager

**Work in progress**

* Converting a offer file from Bol.com shop-in-shop to an csv file suitable for importing into Woocommerce.
* Enrich list of isbn's with metadata using LibraryThing

Module set-up based on sample-project [Play Scala Isolated Slick Example](https://developer.lightbend.com/start/?group=slick&project=play-samples-play-scala-isolated-slick-example)

Original instructions below

## Database Migration

```bash
sbt flyway/flywayMigrate
```

## Slick Code Generation

You will need to run the flywayMigrate task first, and then you will be able to generate tables using sbt-codegen.

```bash
sbt slickCodegen
```

## Testing

You can run functional tests against an in memory database and Slick easily with Play from a clean slate:

```bash
sbt clean flyway/flywayMigrate slickCodegen compile test
```

## Running

To run the project, start up Play:

```bash
sbt run
```

And that's it!

Now go to <http://localhost:9000>, and you will see the list of users in the database.
