CREATE EXTENSION IF NOT EXISTS citext ;


CREATE TABLE IF NOT EXISTS FUser(
  nickname citext NOT NULL PRIMARY KEY,
  email citext NOT NULL UNIQUE,
  fullname citext,
  about citext
);

CREATE TABLE IF NOT EXISTS Forum(
  slug citext NOT NULL PRIMARY KEY,
  title varchar(100)  NOT NULL,
  admin citext NOT NULL,
  messages INTEGER DEFAULT 0,
  threads INTEGER DEFAULT 0,
  FOREIGN KEY (admin) REFERENCES FUser (nickname) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_forums_admin ON Forum (lower(admin));

CREATE TABLE IF NOT EXISTS Thread(
  author citext NOT NULL,
  slug citext UNIQUE,
  create_date timestamptz(6) DEFAULT now() NOT NULL,
  message citext NOT NULL,
  title citext NOT NULL,
  forum citext NOT NULL,
  thread_id SERIAL4 PRIMARY KEY,
  votes INT NOT NULL DEFAULT 0,
  FOREIGN KEY (author) REFERENCES FUser (nickname) ON DELETE RESTRICT ON UPDATE CASCADE,
  FOREIGN KEY (forum) REFERENCES Forum (slug) ON DELETE RESTRICT ON UPDATE CASCADE
);


CREATE INDEX IF NOT EXISTS idx_thread_author ON Thread (lower(author));
CREATE INDEX IF NOT EXISTS idx_thread_forum ON Thread (lower(forum));
CREATE INDEX IF NOT EXISTS idx_thread_slug ON Thread (LOWER(slug));

CREATE TABLE IF NOT EXISTS Message(
  message_id SERIAL8 PRIMARY KEY,
  thread_id int4 NOT NULL,
  message citext NOT NULL,
  author citext NOT NULL,
  create_date timestamptz(6) DEFAULT now() NOT NULL,
  is_edit bool DEFAULT false NOT NULL,
  parent_id int8  NOT NULL DEFAULT 0,
  path INT ARRAY,
  forum citext,
  FOREIGN KEY (thread_id) REFERENCES Thread (thread_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  FOREIGN KEY (author) REFERENCES FUser (nickname) ON DELETE RESTRICT ON UPDATE CASCADE,
  FOREIGN KEY (forum) REFERENCES Forum (slug) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_message_author ON Message (lower(author));
CREATE INDEX IF NOT EXISTS idx_message_thread_id ON Message (thread_id);
CREATE INDEX IF NOT EXISTS idx_message_path ON Message ((path[1]));
CREATE INDEX IF NOT EXISTS idx_posts_get ON Message (message_id, thread_id);
CREATE INDEX IF NOT EXISTS idx_message_parents ON Message(message_id, parent_id, thread_id);
CREATE INDEX IF NOT EXISTS idx_message_flat ON Message (thread_id, create_date, message_id);
CREATE INDEX IF NOT EXISTS idx_message_tree ON Message (thread_id, path);

CREATE TABLE IF NOT EXISTS TLike(
  author citext NOT NULL,
  thread_id int4 NOT NULL,
  mark int2 NOT NULL,
  FOREIGN KEY (thread_id) REFERENCES Thread (thread_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  FOREIGN KEY (author) REFERENCES FUser (nickname) ON DELETE RESTRICT ON UPDATE CASCADE,
  UNIQUE (author, thread_id)
);
CREATE INDEX IF NOT EXISTS idx_like_thread_id ON TLike (thread_id);

CREATE TABLE IF NOT EXISTS Forum_users (
  nickname  citext NOT NULL,
  forum citext NOT NULL
--   FOREIGN KEY (nickname) REFERENCES FUser (nickname) ON DELETE RESTRICT ON UPDATE CASCADE,
--   FOREIGN KEY (forum) REFERENCES Forum (slug) ON DELETE RESTRICT ON UPDATE CASCADE
);
-- CREATE INDEX IF NOT EXISTS idx_forum_users_nickname ON Forum_users (nickname);
-- CREATE INDEX IF NOT EXISTS idx_forum_users_forum ON Forum_users (forum);


CREATE OR REPLACE FUNCTION add_forum_users() RETURNS TRIGGER AS '
BEGIN
  INSERT INTO Forum_users (nickname, forum) VALUES (NEW.author, NEW.forum);
  RETURN NEW;
END;
' LANGUAGE plpgsql;


DROP TRIGGER IF EXISTS thread_insert_trigger ON Thread;
CREATE TRIGGER thread_insert_trigger AFTER INSERT ON Thread
FOR EACH ROW EXECUTE PROCEDURE add_forum_users();

-- DROP TRIGGER IF EXISTS message_insert_trigger ON Message;
-- CREATE TRIGGER message_insert_trigger AFTER INSERT ON Message
-- FOR EACH ROW EXECUTE PROCEDURE add_forum_users();