# ForumDB
# SQL Project 

Builds Docker image
```bash
docker build -t tech-db-forum-orel-image .
```

Create container and then starts it
```bash
docker run -p 5000:5000 --name tech-db-forum-orel-container tech-db-forum-orel-image
```

Start stopped containers
```bash
docker start tech-db-forum-orel-container
```
