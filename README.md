## Micronaut Data Test Bug

### Description

While testing an app using Micronaut Data JDBC, I have the need of running queries to change data immediately before
calling the subject under test to simulate a rapid data burst that the SUT should be able to handle.

Using `JdbcOperations` to update data doesn't work as expected. It can change the data stored in a table, but the SUT is
not able to see changes, even when manually committing the transaction.

### How to test?

Just run the included tests (each one has a, hopefully, meaningful description).