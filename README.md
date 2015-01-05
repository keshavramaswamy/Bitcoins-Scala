Bitcoins Mining
================
* The project uses Scala and the actor model to generate bitcoins.

* The bitcoins are mined with the help of SHA-256 algorithm and the input string with the corresponding SHA-256 hash is printed.

* The program is implemented as a distributed system where the computer address of the server is taken as argument.The server mines coins independently but can also accomodate remote workers. 

* The ratio of CPU Time to Real Time is used as a metric to measure the performance based on which the work unit size was chosen.The code was also tested for the largest number of machines it can run on. 

File(s)
================
project1.scala - Bitcoin Mining