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


## License

Copyright LaunchOpen.com © 2014


