# ForumDB
# SQL Project 

Builds Docker image
```bash
docker build -t forum .
```

Create container and then starts it
```bash
docker run -p 5000:5000 --name forum forum
```

Start stopped containers
```bash
docker start forum
```
