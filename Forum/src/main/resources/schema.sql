CREATE EXTENSION IF NOT EXISTS citext ;


CREATE TABLE IF NOT EXISTS FUser(
nickname citext NOT NULL PRIMARY KEY,
email citext NOT NULL UNIQUE,
fullname citext,
about citext,
CHECK (((nickname)::text ~ similar_escape('[A-Za-z0-9!_!.]+'::text, '!'::text)))
);

CREATE TABLE IF NOT EXISTS Forum(
slug citext NOT NULL PRIMARY KEY,
title varchar(100)  NOT NULL,
admin citext NOT NULL,
FOREIGN KEY (admin) REFERENCES FUser (nickname) ON DELETE RESTRICT ON UPDATE CASCADE,
CHECK (((slug)::text ~ '^(\d|\w|-|_)*(\w|-|_)(\d|\w|-|_)*$'::text))
);

CREATE TABLE IF NOT EXISTS Thread(
author citext NOT NULL,
slug citext UNIQUE,
create_date timestamptz DEFAULT now() NOT NULL,
message citext NOT NULL,
title citext NOT NULL,
forum citext NOT NULL,
thread_id SERIAL4 PRIMARY KEY,
CHECK (((slug)::text ~ '^(\d|\w|-|_)*(\w|-|_)(\d|\w|-|_)*$'::text)),
FOREIGN KEY (author) REFERENCES FUser (nickname) ON DELETE RESTRICT ON UPDATE CASCADE,
FOREIGN KEY (forum) REFERENCES Forum (slug) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS Message(
message_id SERIAL8 PRIMARY KEY,
thread_id int4 NOT NULL,
message citext NOT NULL,
author citext NOT NULL,
create_date timestamptz(6) DEFAULT now() NOT NULL,
is_edit bool DEFAULT false NOT NULL,
parent_id int8,
FOREIGN KEY (parent_id) REFERENCES Message (message_id) ON DELETE RESTRICT ON UPDATE CASCADE,
FOREIGN KEY (thread_id) REFERENCES Thread (thread_id) ON DELETE RESTRICT ON UPDATE CASCADE,
FOREIGN KEY (author) REFERENCES FUser (nickname) ON DELETE RESTRICT ON UPDATE CASCADE
);


CREATE TABLE IF NOT EXISTS TLike(
author citext NOT NULL,
thread_id int4 NOT NULL,
mark int2 NOT NULL,
FOREIGN KEY (thread_id) REFERENCES Thread (thread_id) ON DELETE RESTRICT ON UPDATE CASCADE,
FOREIGN KEY (author) REFERENCES FUser (nickname) ON DELETE RESTRICT ON UPDATE CASCADE,
UNIQUE (author, thread_id)
);




