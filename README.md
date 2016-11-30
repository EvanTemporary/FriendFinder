# FriendFinder

## About

I was asked to design and implement a service for use as an abstraction from a large-magnitude data stream.

This Play service is designed to maintain a graph of connections which may be quickly queried and manipulated
in order to provide fast and useful user information. It does so by ingesting a maintaining a high-volume
stream of "friendships": pieces of information denoting which users are friends with which. The ingestion
process makes use of Akka's non-blocking streaming framework to parse data, and Neo4j for storage.

## Design

I wanted to design the service to provide instant responses despite the magnitude of the data it must maintain. 
For this type of data, graph databases came to mind for their ability to provide quick responses to relational 
queries, and I haven't gotten to work with a graph db yet so now seemed like a good time.

The graph of choice is Neo4j, a forerunner in the graph db world, and which happens to be very easy to install and
configure.

I chose to use the Play framework because it is the most likely to be used in production for real-world services
like these, and because it is closest to what you and I actually use in the day-to-day. It has drawbacks
for small projects like this, like the amount of boilerplate and number of files, but I thought I
might as well use the real thing. Please excuse the heavy handedness.

In order to be constantly ready for instantaneous response the service needs to have an up-to-date graph
representation of friendships in the social network. To achieve this it needs two components:
#### Ingester
Maintains the ongoing process of contacting the data source, streaming the chucked response, parsing into scala,
and writing to db accordingly. 
#### Friendship API
Enables an endpoint to return information about a user by querying the database. The API is also used to trigger
the ingestion process. See more below.

## Running the service
* Setup Neo4J
    * Run `$ brew install neo4j`
    * Run `$ neo4j start`
    * If your username/password are not the default `neo4j`/`neo4j`, adjust `application.conf` accordingly.
* Clone locally and `cd` to `FriendFinder/`
* Run `$ sbt run` (`$ brew install sbt` if you don't have sbt)
* To start ingesting records: `$ curl http://localhost:3000/ingest`. If you are running this locally and don't want to
 trash your system you can terminate the program and restart it (see below re: SBT shortcomings).
* To query for a user with id=`[id]`'s friends: `$ curl http://localhost:3000/friends?id=[id]`

## This Design in Practice
My design has turned out to be less than ideal for operating on a single machine. This is not surprising given the 
magnitude of the data, but I wanted to write you software that, given the machines, could successfully scale
to perform very well. I'm not sure how/if you plan to test this, but beware thrashing!

Additionally, in the end, it seemed SBT was not the best choice for this design because of the need for controlled
and monitored background tasks. Triggering a background task via API is not my ideal, but seemed like a compromise
for this pet projects. The ingestion process should be separated out, but for this take-home including it in the Play
API seemed sufficient for demonstrating the design and, in large part, implementation.

## Future work

If I were developing this for use in a live system a number of features would need to be added. They include:
error handling, bad request responses, logging and metric statting, unit and api tests,
base traits with method declarations for multiple inheritance, AND most importantly framework for expansion
such as AWS and NGINX load ballancers. This just doesn't do well on a single machine.

Additionally, I could not find any Cypher tools that were compatible with Play 2.5.x but I wanted to play with the
new Akka-style streams so I wrote my own. This isn't ideal because of all the custom boilerplate, but I learned a lot
in the process.

Lastly, I left TODOs in the code to show a few places I would want to improve (or make fully functional).

## Misc

This is my first time working with graph dbs as well as HTTP data streams/chucks, so please let me know if you
have any questions or suggestions. I'm sure there is room for improvement in those corners of the service (and others!)
but it was a pleasure to work on and I hope to learn even more from the process from your feedback.

Thanks,

Evan

----------
##### Assignment


 
At https://immense-river-17982.herokuapp.com/, there is an service that has logged every time someone has clicked a button to add or remove a friendship to a fake social service.  For example,
 
```
curl -s https://immense-river-17982.herokuapp.com/?since=1352102565590 | head -5
{"from":{"id":"vFGj1gT4c6I=","name":"Christopher Newman"},"to":{"id":"V+HGOZuQQZY=","name":"Jeff Nieves"},"areFriends":true,"timestamp":"1352102565590"}
{"from":{"id":"XJ2CG/o6280=","name":"Alfred Blair"},"to":{"id":"Darv88kRhV4=","name":"ken Downs"},"areFriends":false,"timestamp":"1352102565591"}
{"from":{"id":"exffc8SJ36Y=","name":"John Williams"},"to":{"id":"nZW7lq6X1kc=","name":"Julio Miranda"},"areFriends":true,"timestamp":"1352102565592"}
{"from":{"id":"G2O+uLnMRtI=","name":"Floyd Nguyen"},"to":{"id":"U17kUNsYGhA=","name":"Shawn Atkinson"},"areFriends":true,"timestamp":"1352102565593"}
{"from":{"id":"PIAg6tkDEVA=","name":"Anthony Michael"},"to":{"id":"MARqIlKVT7c=","name":"Angel Santana"},"areFriends":true,"timestamp":"1352102565594"}
```
 
So from the first line, Christopher became friends with Jeff.  Then Alfred "unfriended" Ken, and so on.  The timestamp is the number of milliseconds since 1970.
 
This service is very much like a database transaction log.  It's a stream that never ends, although it will slow down as you catch up to the present time.  It is also resumable as of any timestamp (see the `since` querystring parameter).
 
Write a new http API in the language of your choice that consumes this existing API.  I should be able to make a call to your API with a user's id, and get back a list of who their current friends are. e.g.:
 
```
# your service
curl -s http://localhost:3000/friends?id=exffc8SJ36Y%3D
{ "friends": [{"id":"nZW7lq6X1kc=","name":"Julio Miranda"} ... ] }
```
