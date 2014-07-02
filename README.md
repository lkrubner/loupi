# loupi

loupi is the Launch Open User Persistence Integration -- it provides endpoints to our front Javascript to enable all CRUD operations via Ajax.

## Our RESTful Philosophy

For our purposes, CRUD maps to HTTP verbs like this: 

Create = 

    PUT (if there is a document _id in the document -- the old document is completely overwritten)

    POST (if there is NOT a document _id in the document)

Retrieve = GET

Update = POST (there is MUST be a document _id in the document, and the new content will be merged with old)

Delete = DELETE (there is MUST be a document _id in the document, and the document will be deleted)

If you need to know more about CRUD to REST mapping, read here:

http://jcalcote.wordpress.com/2008/10/16/put-or-post-the-rest-of-the-story/


## Usage

We expect a collection name, at a minimum. 

This returns the first 10 items in the CourseActivityGrade collection:

GET

http://das.launchopen.com/v0.1/CourseActivityGrade

The return would look like this (only 2 items shown for the sake of brevity):

    [
    {
	_id : ObjectId("53a484c03d698858278c2198"),
	accountID : "opensis",
	id : "55-67-4846",
	finalgrade : "56.00",
	userid : "55"
    },
    {
	_id : ObjectId("53a484c03d698858278c2199"),
	accountID : "opensis",
	id : "55-67-4845",
	finalgrade : "78.00",
	userid : "55"
    }
    ]

If you append a document id:

GET

http://das.launchopen.com/v0.1/CourseActivityGrade/53a484c03d698858278c219c

Then you get that document:

    {
	_id : ObjectId("53a484c03d698858278c219c"),
	accountID : "opensis",
	id : "13-67-4846",
	finalgrade : "85.00",
	userid : "13"
    }

If you add the "sort" parameter, then you can add 3 more parameters: a sort field, a starting document (offset), and how many you want (limit). So this:

http://das.launchopen.com/v0.1/CourseActivityGrade/sort/finalgrade/400/1000

will sort by "finalgrade", then skip over the first 400 documents, and then give you 1,000 documents.

If you then POST an attribute and value { "country_of_origin" : "Poland" } using this document id: 

POST

http://das.launchopen.com/v0.1/CourseActivityGrade/53a484c03d698858278c219c

the data is then merged with the existing document:

    {
	_id : ObjectId("53a484c03d698858278c219c"),
	accountID : "opensis",
	id : "13-67-4846",
	finalgrade : "85.00",
	userid : "13",
	country_of_origin : "Poland"
    }

However, if you use PUT instead of POST, then the document is over-written with the new data:

PUT

http://das.launchopen.com/v0.1/CourseActivityGrade/53a484c03d698858278c219c

gives you:

    {
	_id : ObjectId("53a484c03d698858278c219c"),
	country_of_origin : "Poland"
    }

You can also use PUT to create a new document. Here we assume you are sending this data with no document id: 

    {
	"accountID" : "opensis",
	"id" : "55-67-4847",
	"finalgrade" : "85.00",
	"userid" : "55"
    }

So this with the above payload:

PUT

http://das.launchopen.com/v0.1/CourseActivityGrade/53a484c03d698858278c219c

means this document is now in the database:

    {
	"_id" : ObjectId("53a484c03d698858278c2197"),
	"accountID" : "opensis",
	"id" : "55-67-4847",
	"finalgrade" : "85.00",
	"userid" : "55"
    }

Finally, if you DELETE with a document id, then the document is deleted: 

DELETE

http://das.launchopen.com/v0.1/CourseActivityGrade/53a484c03d698858278c219c



## Database Collections

We currently have these collections in our MongoDB database: 

Apps

Assignments

Attendance

Awards

Course

CourseActivity

CourseActivityGrade

GradeHistory

Grades

Reminders

Role

SchoolYear

Searches

User

UserApps

UserAwards

UserDisciplinaryIncidents

UserProvider

UserRole



## Setup

Database configuration is kept in /resources/config/credentials.edn, which looks like this:

{
 :host "localhost
 :db "database"
 :username "user"
 :password "1234"
}

We set .gitignore to keep this file out of the repo, so you'll need to set this file yourself. 

To compile this app, you will need to have the JVM installed on your computer.

Also, be sure you have Leinengen installed on your computer:

http://leiningen.org/

Once you have that installed, you can cd to the directory where this project is, and then, at the command prompt, type:

lein uberjar

That will give you a single binary that combines everything: HTML, CSS, Javascript, the Jetty webserver, the logic. You can the start the app by cd'ing to the directory where the binary is and running: 

java -jar loupi-0.1-standalone.jar 40000

That starts the app on port 40000. You can specify any port you want. If you don't already have an app listening on port 80, you can run this on port 80. If you forget to specify a port, the app defaults to port 34000 (I picked a high number to avoid conflicts with any other software you might be running). 


## Our Design Philosophy

I am a big believer in "design by contract" so the important functions have both pre and post assertions. For instance, the database function that paginates results (allows a limit and offset) defines 8 pre assertions, and 1 post assertion. These assertions partly take the place of unit tests, and they clearly tell all future developers what this function is expecting. (The assertions slow the code and so they are only used in development. The compiler accepts a flag that strips out all of the assertions when we are ready to move to production.)

    (defn paginate-results [ctx]
    {:pre [
         (map? ctx)
         (map? (:database-where-clause-map ctx))
         (string? (get-in ctx [:request :name-of-collection])) 
         (string? (get-in ctx [:request :field-to-sort-by])) 
         (string? (get-in ctx [:request :offset-by-how-many])) 
         (string? (get-in ctx [:request :return-how-many])) 
         (number? (Integer/parseInt (get-in ctx [:request :offset-by-how-many])))
         (number? (Integer/parseInt (get-in ctx [:request :return-how-many]))) 
         ]
    :post [(= (type %) clojure.lang.LazySeq)]}
    (with-collection (get-in ctx [:request :name-of-collection])
    (find (:database-where-clause-map ctx))
    (sort (array-map  (get-in ctx [:request :field-to-sort-by]) 1))
    (limit (Integer/parseInt (get-in ctx [:request :return-how-many]))
    (skip (Integer/parseInt (get-in ctx [:request :offset-by-how-many]))))))




## License

Copyright LaunchOpen.com © 2014


